package project


import java.io.File

import breeze.linalg.{DenseMatrix, DenseVector}
import scalismo.common._
import scalismo.geometry._
import scalismo.io.{LandmarkIO, MeshIO, StatisticalModelIO}
import scalismo.kernels._
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel._
import scalismo.ui.api._

object femur_completion_101149 {


  def main(args: Array[String]) {
    scalismo.initialize()
    implicit val rng = scalismo.utils.Random(42)
    val ui = ScalismoUI()

    val femur_partial = MeshIO.readMesh(new java.io.File("data/partials/101149/101149.stl")).get

    val targetGroup = ui.createGroup("target")
    ui.show(targetGroup, femur_partial, "femur_broken")

    val smallModel = StatisticalModelIO.readStatisticalMeshModel(new java.io.File("data/results/6_aligned.h5")).get

    val scalarValuedKernel = GaussianKernel[_3D](70) * 10.0

    case class XmirroredKernel(kernel: PDKernel[_3D]) extends PDKernel[_3D] {
      override def domain = RealSpace[_3D]

      override def k(x: Point[_3D], y: Point[_3D]) = kernel(Point(x(0) * -1f, x(1), x(2)), y)
    }

    def symmetrizeKernel(kernel: PDKernel[_3D]): MatrixValuedPDKernel[_3D] = {
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

    val referenceLandmarks = LandmarkIO.readLandmarksJson[_3D](new java.io.File("data/ref_6_aligned.json")).get
    val referencePoints : Seq[Point[_3D]] = referenceLandmarks.map(lm => lm.point)
    val referenceLandmarkViews = referenceLandmarks.map(lm => ui.show(modelGroup, lm, s"lm-${lm.id}"))


    val femur_partialLandmarks = LandmarkIO.readLandmarksJson[_3D](new java.io.File("data/partials/101149/101149.json")).get
    val femur_partialPoints : Seq[Point[_3D]] = femur_partialLandmarks.map(lm => lm.point)
    val femur_partialLandmarkViews = femur_partialLandmarks.map(lm => ui.show(targetGroup, lm, s"lm-${lm.id}"))

    val domain = UnstructuredPointsDomain(referencePoints.toIndexedSeq)
    val deformations = (0 until referencePoints.size).map(i => femur_partialPoints(i) - referencePoints(i) )
    val defField = DiscreteField[_3D, UnstructuredPointsDomain[_3D], EuclideanVector[_3D]](domain, deformations)
    // ui.show(modelGroup, defField, "partial_Field")

    val littleNoise = MultivariateNormalDistribution(DenseVector.zeros[Double](3), DenseMatrix.eye[Double](3) * 0.5)

    val regressionData = for ((refPoint, femur_partialPoint) <- referencePoints zip femur_partialPoints) yield {
      val refPointId = model.referenceMesh.pointSet.findClosestPoint(refPoint).id
      (refPointId, femur_partialPoint, littleNoise)
    }

    val posterior = model.posterior(regressionData.toIndexedSeq)

    val posteriorGroup = ui.createGroup("posterior-model")
    //ui.show(posteriorGroup, posterior, "posterior")

    val femur_partialPtIDs = model.referenceMesh.pointSet.pointIds.filter { id =>
      (model.referenceMesh.pointSet.point(id) - model.referenceMesh.pointSet.point(PointId(10500))).norm <= 500
    }

    val posterior_femur_partialModel = posterior.marginal(femur_partialPtIDs.toIndexedSeq)

    val posterior_femur_partialGroup = ui.createGroup("posterior-femur_partial-model")
    ui.show(posterior_femur_partialGroup, posterior_femur_partialModel, "posterior_femur_partialModel")
    StatisticalModelIO.writeStatisticalMeshModel(posterior_femur_partialModel, new java.io.File("data/101149_aligned_final.h5"))
    val meanFemur : TriangleMesh[_3D] = posterior_femur_partialModel.mean
    ui.show(posterior_femur_partialGroup, meanFemur, "meanFemur")
    val filename=new File("Data/results/101149_aligned_bottom.stl")
    MeshIO.writeMesh(meanFemur, filename)

  }
}