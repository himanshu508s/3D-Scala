package project



import scalismo.geometry._
import scalismo.io.MeshIO
import scalismo.io.LandmarkIO
import scalismo.geometry.{Point, _3D}
import scalismo.mesh.TriangleMesh3D
import scalismo.registration.RigidTransformation
import scalismo.registration.LandmarkRegistration
import java.io.File

import scalismo.ui.api.{ScalismoUI, ScalismoUIHeadless}

object femur_realignment
{

  def main(args: Array[String])
  {

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
    val mesh : TriangleMesh3D = MeshIO.readMesh(new File("data/partials/101156/101156.stl")).get
    ui.show(FemurBroken,mesh, "Femur")

    val FemurRigid = ui.createGroup("FemurRigid")
    val meshRigid : TriangleMesh3D = MeshIO.readMesh(new File("Data/results/101156_aligned.stl")).get
    ui.show(FemurRigid,meshRigid, "FemurRigid")

    //load reference landmarks
    val femurFS = LandmarkIO.readLandmarksJson[_3D](new File("data/broken_10.json")).get

    val rigidfemurFS = LandmarkIO.readLandmarksJson[_3D](new File("data/target_10.json")).get

    val bestTransform: RigidTransformation[_3D] = LandmarkRegistration.rigid3DLandmarkRegistration(rigidfemurFS, femurFS, center = Point(0, 0, 0))
    val femurGroup = ui.createGroup("femur")
    val transformedLms = rigidfemurFS.map(lm => lm.transform(bestTransform))
    val landmarkViews = ui.show(femurGroup, transformedLms, "transformedLMs")
    val alignedFemur = meshRigid.transform(bestTransform)

    // this is for the view

      val alignedFemurView = ui.show(femurGroup, alignedFemur, "alignedFemur")
      alignedFemurView.color = java.awt.Color.RED

    val filename=new File("Data/101156_aligned_final.ply")
    MeshIO.writeMesh(alignedFemur, filename)

    println("Finished")


  }
}
