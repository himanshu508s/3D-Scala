package project


import java.awt.Color

import scalismo.io.{LandmarkIO, MeshIO, StatisticalModelIO}
import java.io.File

import scalismo.geometry._3D
import scalismo.mesh.MeshMetrics
import scalismo.ui.api.ScalismoUI

object femur_distance {

  def main(args: Array[String]) {

    // setting a seed for the random generator to allow for reproducible results
    implicit val rng = scalismo.utils.Random(42)

    // required to initialize native libraries (VTK, HDF5 ..)
    scalismo.initialize()

    // Your application code goes below here. Below is a dummy application that reads a mesh and displays it

    // create a visualization window
    val ui = ScalismoUI()

    // read a mesh from file
    // val mesh = MeshIO.readMesh(new File("data/facemesh.ply")).get
    // val mesh = MeshIO.readMesh(new File("data/meshes/0.stl")).get
    //val landmarkIOFemur = LandmarkIO.readLandmarksJson[_3D](new File("data/landmarks/0.json")).get
    // display it
    //val meshView = ui.show(mesh, "femur")
    //val landmarkView = ui.show(landmarkIOFemur, "Lfemur")
    // change its color
    //meshView.color = Color.GREEN
    var min = 10000000000.0
    var mesh_id = 0
    var distance = 0.0
    var min2=0.0
    var mesh_id2=0
    var temp=0



    val femur_partial = MeshIO.readMesh(new java.io.File("data/partials/101156/101156.stl")).get

    val targetGroup = ui.createGroup("target")
    ui.show(targetGroup, femur_partial,"femur_broken")
    for(i <- 0 to 49)
    {

      // load rigid mesh
      //val meshName = "FemurRigid_" + i
      // val meshRigid= StatisticalModelIO.readStatisticalMeshModel(new File("data/results/" + i + "_aligned.h5")).get
      val meshRigid= StatisticalModelIO.readStatisticalMeshModel(new File("data/femur_bottom/femur_bottom_" + i + ".h5")).get
      //ui.show(meshRigid, "FemurRigid")
      distance=MeshMetrics.hausdorffDistance(femur_partial,meshRigid.mean);
      println("distance between mesh "+i+" and candidate mesh is "+distance);
      if (distance<min)
      {
        min2=min
        mesh_id2=mesh_id
        min=distance
        mesh_id=i
        println("Distance between mesh "+i+" and candidate mesh is less that previous min distance. Current nearest mesh is mesh "+ mesh_id)
      }
    }

    println("Nearest mesh is mesh "+mesh_id)
    val targetMesh= MeshIO.readMesh(new File("data/results/" + mesh_id + "_aligned.vtk")).get
    ui.show(targetMesh, "FemurRef_near")

    println("Second Nearest mesh is mesh "+mesh_id2)
    val targetMesh2= MeshIO.readMesh(new File("data/results/" + mesh_id2 + "_aligned.vtk")).get
    ui.show(targetMesh2, "FemurRef_second")

    //for top 18 and 13,
    //for middle 25 and 7
    //bottom is 15 and 11
    //for 51, best all is 4, middle is 25 and 7
    //for 52, use what is there previously or 37
    //for 56, maybe 3 or 2
  }
}

