package project


import scalismo.geometry._
import scalismo.common._
import scalismo.ui.api._
import scalismo.registration.Transformation
import scalismo.io.MeshIO
import scalismo.mesh.{TriangleCell, TriangleList, TriangleMesh, TriangleMesh3D}

object femur_combine_mesh {
  def main(args: Array[String]) {
    scalismo.initialize()
    implicit val rng = scalismo.utils.Random(42)
  val ui=ScalismoUI()
    val bone1Mesh : TriangleMesh[_3D]= MeshIO.readMesh(new java.io.File("data/partials/101156/101156.stl")).get
    //val bone1Mesh  = femur_partial
    val bone2Mesh : TriangleMesh[_3D]= MeshIO.readMesh(new java.io.File("Data/results/101156_aligned.vtk")).get

    val numPointsBone1 = bone1Mesh.pointSet.numberOfPoints
    val newBone2Cells = bone1Mesh.cells.map(cell =>
      TriangleCell(PointId(cell.ptId1.id + numPointsBone1),
        PointId(cell.ptId2.id + numPointsBone1), PointId(cell.ptId3.id +
          numPointsBone1))
    )
    val combinedMesh = TriangleMesh3D(
      UnstructuredPointsDomain(bone1Mesh.pointSet.points.toIndexedSeq ++
        bone2Mesh.pointSet.points.toIndexedSeq),
      TriangleList(bone1Mesh.cells ++ newBone2Cells)
    )
    ui.show(combinedMesh, "CombinedFemur")

  }
}

