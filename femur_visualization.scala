package project

import java.io.File

import scalismo.io.{MeshIO, StatisticalModelIO}
import scalismo.mesh.TriangleMesh3D
import scalismo.ui.api.ScalismoUI

object femur_visualization {


  def main(args: Array[String]) {

    // required to initialize native libraries (VTK, HDF5 ..)
    scalismo.initialize()

    val display = false

    // we need to random
    implicit val rng = scalismo.utils.Random(42)

    // Your application code goes below here. Below is a dummy application that reads a mesh and displays it

    // create a visualization window
    val ui = ScalismoUI()
    //val ui = ScalismoUIHeadless()

    // load reference mesh
    val FemurBroken = ui.createGroup("FemurBroken")
    val mesh: TriangleMesh3D = MeshIO.readMesh(new File("data/new/partials/101154/101154.stl")).get
    ui.show(FemurBroken, mesh, "Femur")
    //val mesh1: TriangleMesh3D = MeshIO.readMesh(new File("data/new/partials/101156/101156_aligned_final.ply")).get
    //ui.show(FemurBroken, mesh1, "Femur")

    val model = StatisticalModelIO.readStatisticalMeshModel(new java.io.File("data/new/partials/101154/101154_aligned_final.h5")).get
   ui.show(FemurBroken, model, "Femur1")



  }
}
