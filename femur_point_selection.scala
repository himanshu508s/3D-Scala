package project

import scalismo.geometry._
import scalismo.common._
import scalismo.ui.api._
import scalismo.mesh._
import scalismo.registration.LandmarkRegistration
import scalismo.io.{MeshIO}
import scalismo.numerics.UniformMeshSampler3D
import breeze.linalg.{DenseMatrix, DenseVector}

object femur_point_selection {
  def main(args: Array[String]) {
    scalismo.initialize()
    implicit val rng = scalismo.utils.Random(42)
    val ui = ScalismoUI()
    val femur_partial = MeshIO.readMesh(new java.io.File("data/partials/101156/101156.stl")).get

    val targetGroup = ui.createGroup("target")
   // ui.show(targetGroup, femur_partial,"femur_broken")

    val femur_ref = MeshIO.readMesh(new java.io.File("data/results/25_aligned.vtk")).get
    //ui.show(targetGroup, femur_ref,"femur_ref")
    //val x=femur_ref.referenceMesh.pointSet.findClosestPoint().id
    val x=femur_ref.pointSet.findClosestPoint(Point(-17.245712280273438,50.134891510009766,236.3813934326172)).id;
    //val ptIds = (0 until femur_partial.pointSet.numberOfPoints by 50).map(i => PointId(i))
    //ui.show(targetGroup, ptIds.map(id => femur_partial.pointSet.point(id)), "selected")
    //val x2=MeshMetrics.hausdorffDistance()
    val x1=MeshMetrics.hausdorffDistance(femur_partial,femur_ref);
    println("Point id is "+x)
    println("Hausdorff Distance is "+ x1);




  }

}
