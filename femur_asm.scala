package project
import java.io.File

import scalismo.common._
import scalismo.geometry._
import scalismo.io.{MeshIO, StatisticalModelIO}
import scalismo.kernels._
import scalismo.mesh._
import scalismo.numerics.RandomMeshSampler3D
import scalismo.statisticalmodel._
import scalismo.ui.api.ScalismoUI

object femur_asm {

  // required to initialize native libraries (VTK, HDF5 ..)
  scalismo.initialize()

  // we need to random value
  implicit val rng = scalismo.utils.Random(42)

  // create a visualization window
  val ui = ScalismoUI()

  def main(args: Array[String]) {

    val smoothness : List[Double] = List(110)
    val lengths : List[Double]    = List(2)
    for(i <- 0 to 49) {
      val model = CombineKernels(lengths, smoothness, i)
    }
   // val group1 = ui.createGroup("Femur")
    //val referenceMesh = MeshIO.readMesh(new File("data/femur.stl")).get
    //ui.show(group1, referenceMesh, "reference")
    //ui.show(group1, model,"model")
  }


  def IdentityKernel(ker : PDKernel[_3D]) : MatrixValuedPDKernel[_3D] = {
    val k1 = DiagonalKernel(ker,ker,ker)
    return k1
  }
  def CombineKernels(sValues : List[Double], lValues : List[Double], i: Int): StatisticalMeshModel ={

    implicit val rng = scalismo.utils.Random(42)

    val referenceMesh = MeshIO.readMesh(new java.io.File("data/results/"+i+"_aligned.vtk")).get


    val zeroMean = Field(RealSpace[_3D], (pt:Point[_3D]) => EuclideanVector(0,0,0))

    val sampler = RandomMeshSampler3D(
      referenceMesh,
      numberOfPoints = 300,
      seed = 42)


    var combineKernel = IdentityKernel(GaussianKernel[_3D](lValues(0)) * sValues(0))

    for(i <- 0 until sValues.length) {
      combineKernel = combineKernel + IdentityKernel(GaussianKernel[_3D](lValues(i)) * sValues(i))
    }

    val scalarValuedGaussianKernel : PDKernel[_3D]= GaussianKernel(sigma = 110)
    val matrixValuedGaussianKernel = DiagonalKernel(scalarValuedGaussianKernel, scalarValuedGaussianKernel, scalarValuedGaussianKernel*2)
    combineKernel = combineKernel + matrixValuedGaussianKernel

    val gp = GaussianProcess(zeroMean, combineKernel)

    val lowRankGP = LowRankGaussianProcess.approximateGPNystrom(
      gp,
      sampler,
      numBasisFunctions = 100)

    val meshModel = StatisticalMeshModel(referenceMesh,lowRankGP)
    StatisticalModelIO.writeStatisticalMeshModel(meshModel, new java.io.File("data/"+i+"_aligned.h5"))
    println("finished running for "+i)
    return meshModel
  }
}