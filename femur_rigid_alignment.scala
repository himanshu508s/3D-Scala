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

object femur_rigid_alignment {

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
    val mesh : TriangleMesh3D = MeshIO.readMesh(new File("data/femur.stl")).get
    ui.show(mesh, "Femur")

    //load reference landmarks
    val femurFS = LandmarkIO.readLandmarksJson[_3D](new File("data/femur.json")).get


    for(i <- 0 to 49) {

      // load rigid mesh
      val meshRigid: TriangleMesh3D = MeshIO.readMesh(new File("data/meshes/" + i + ".stl")).get
      ui.show(meshRigid, "FemurRigid")

      // load rigid landmarks
      val rigidfemurFS = LandmarkIO.readLandmarksJson[_3D](new File("data/landmarks/" + i + ".json")).get

      val bestTransform: RigidTransformation[_3D] = LandmarkRegistration.rigid3DLandmarkRegistration(rigidfemurFS, femurFS, center = Point(0, 0, 0))

      val femurGroup = ui.createGroup("femur")

      val transformedLms = rigidfemurFS.map(lm => lm.transform(bestTransform))
      val landmarkViews = ui.show(femurGroup, transformedLms, "transformedLMs")
      val alignedFemur = meshRigid.transform(bestTransform)
      if(display){
        val alignedFemurView = ui.show(femurGroup, alignedFemur, "alignedFemur")
        alignedFemurView.color = java.awt.Color.RED
      }


      val alignedLandmarks = rigidfemurFS.map(lm => lm.copy(point = bestTransform(lm.point)))

      MeshIO.writeVTK(alignedFemur, new File("data/results/"+i+"_aligned.vtk"))
      LandmarkIO.writeLandmarksJson(alignedLandmarks, new File("data/results/"+i+"_aligned.json"))

      println("finished running it for:"+i)

    }
  }
}