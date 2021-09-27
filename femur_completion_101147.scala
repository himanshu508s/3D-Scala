package project

import java.io.File

import scalismo.geometry._
import scalismo.common._
import scalismo.ui.api._
import scalismo.mesh._
import scalismo.io.{LandmarkIO, MeshIO, StatisticalModelIO}
import scalismo.statisticalmodel._
import scalismo.numerics.UniformMeshSampler3D
import scalismo.kernels._
import breeze.linalg.{DenseMatrix, DenseVector}

object femur_completion_101147 {

  def main(args: Array[String]) {
    scalismo.initialize()
    implicit val rng = scalismo.utils.Random(42)
    val ui = ScalismoUI()

    val femur_partial = MeshIO.readMesh(new java.io.File("data/partials/101147/101147.stl")).get

    val targetGroup = ui.createGroup("target")
    ui.show(targetGroup, femur_partial,"femur_broken")

    val smallModel = StatisticalModelIO.readStatisticalMeshModel(new java.io.File("data/results/0_aligned.h5")).get

    val scalarValuedKernel = GaussianKernel[_3D](70) * 10.0

    case class XmirroredKernel(kernel : PDKernel[_3D]) extends PDKernel[_3D] {
      override def domain = RealSpace[_3D]
      override def k(x: Point[_3D], y: Point[_3D]) = kernel(Point(x(0) * -1f ,x(1), x(2)), y)
    }

    def symmetrizeKernel(kernel : PDKernel[_3D]) : MatrixValuedPDKernel[_3D] = {
      val xmirrored = XmirroredKernel(kernel)
      val k1 = DiagonalKernel(kernel, 3)
      val k2 = DiagonalKernel(xmirrored * -1f, xmirrored, xmirrored)
      k1 + k2
    }

    val gp = GaussianProcess[_3D, EuclideanVector[_3D]](symmetrizeKernel(scalarValuedKernel))
    val lowrankGP = LowRankGaussianProcess.approximateGPCholesky(
      smallModel.referenceMesh.pointSet,
      gp,
      relativeTolerance = 0.01,
      interpolator = NearestNeighborInterpolator())

    val model = StatisticalMeshModel.augmentModel(smallModel, lowrankGP)

    val modelGroup = ui.createGroup("femur refrence")
    val ssmView = ui.show(modelGroup, model, "femur_refrence")

    val referenceLandmarks = LandmarkIO.readLandmarksJson[_3D](new java.io.File("data/ref_0_aligned.json")).get
    val referencePoints : Seq[Point[_3D]] = referenceLandmarks.map(lm => lm.point)
    val referenceLandmarkViews = referenceLandmarks.map(lm => ui.show(modelGroup, lm, s"lm-${lm.id}"))


    val femur_partialLandmarks = LandmarkIO.readLandmarksJson[_3D](new java.io.File("data/partials/101147/101147.json")).get
    val femur_partialPoints : Seq[Point[_3D]] = femur_partialLandmarks.map(lm => lm.point)
    val femur_partialLandmarkViews = femur_partialLandmarks.map(lm => ui.show(targetGroup, lm, s"lm-${lm.id}"))

    val domain = UnstructuredPointsDomain(referencePoints.toIndexedSeq)
    val deformations = (0 until referencePoints.size).map(i => femur_partialPoints(i) - referencePoints(i) )
    val defField = DiscreteField[_3D, UnstructuredPointsDomain[_3D], EuclideanVector[_3D]](domain, deformations)
   ui.show(modelGroup, defField, "partial_Field")

    val littleNoise = MultivariateNormalDistribution(DenseVector.zeros[Double](3), DenseMatrix.eye[Double](3) * 0.1)

    val regressionData = for ((refPoint, femur_partialPoint) <- referencePoints zip femur_partialPoints) yield {
      val refPointId = model.referenceMesh.pointSet.findClosestPoint(refPoint).id
      (refPointId, femur_partialPoint, littleNoise)
    }

    val posterior = model.posterior(regressionData.toIndexedSeq)

    val posteriorGroup = ui.createGroup("posterior-model")
    ui.show(posteriorGroup, posterior, "posterior")

    val femur_partialPtIDs = model.referenceMesh.pointSet.pointIds.filter { id =>
      (model.referenceMesh.pointSet.point(id) - model.referenceMesh.pointSet.point(PointId(140))).norm <= 75
    }

    val posterior_femur_partialModel = posterior.marginal(femur_partialPtIDs.toIndexedSeq)

    val posterior_femur_partialGroup = ui.createGroup("posterior-femur_partial-model")
    ui.show(posterior_femur_partialGroup, posterior_femur_partialModel, "posterior_femur_partialModel")
    val meanFemur : TriangleMesh[_3D] = posterior_femur_partialModel.mean
   ui.show(posterior_femur_partialGroup, meanFemur, "meanFemur")
    val filename=new File("Data/results/101147_aligned.stl")
    MeshIO.writeMesh(meanFemur, filename)





   // val targetMesh = femur_partial
   // val model1 = smallModel
    //val resultGroup = ui.createGroup("result-model")
/**
    val sampler = UniformMeshSampler3D(model1.referenceMesh, numberOfPoints = 5000)
    val points : Seq[Point[_3D]] = sampler.sample.map(pointWithProbability => pointWithProbability._1)
    val ptIds = points.map(point => model1.referenceMesh.pointSet.findClosestPoint(point).id)
    def attributeCorrespondences(movingMesh: TriangleMesh[_3D], ptIds: Seq[PointId]): Seq[(PointId, Point[_3D])] = {
      ptIds.map { id: PointId =>
        val pt = movingMesh.pointSet.point(id)
        val closestPointOnMesh2 = targetMesh.pointSet.findClosestPoint(pt).point
        (id, closestPointOnMesh2)
      }
    }

   /** def nonrigidICP(movingMesh: TriangleMesh[_3D], ptIds : Seq[PointId], numberOfIterations : Int) : TriangleMesh[_3D] = {
      if (numberOfIterations == 0) movingMesh
      else {
        val correspondences = attributeCorrespondences(movingMesh, ptIds)
        val transformed = fitModel(correspondences)

        nonrigidICP(transformed, ptIds, numberOfIterations - 1)
      }
    }**/

    def nonrigidICP(movingMesh: TriangleMesh[_3D], posteriorModel: StatisticalMeshModel, numberOfIterations: Int): TriangleMesh[_3D] = {
      if (numberOfIterations % 10 == 0) println("iter:" + numberOfIterations)

      if (numberOfIterations == 0) movingMesh
      else {
        // we get the points again
        val correspondences = attributeCorrespondences(posteriorModel.mean, ptIds)

        val regressionData = correspondences.map(correspondence =>
          (correspondence._1, correspondence._2, littleNoise)
        )

        val posterior = posteriorModel.posterior(regressionData.toIndexedSeq)

        nonrigidICP(posterior.mean, posterior, numberOfIterations - 1)
      }
    }

    val finalFit = nonrigidICP( model1.mean, model1, 10)

    ui.show(resultGroup, finalFit, "final fit")

**/
  }

}
