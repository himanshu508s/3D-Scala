package project

import scalismo.common.PointId
import scalismo.io.StatisticalModelIO
import scalismo.ui.api.ScalismoUI

object femur_example_head {
  def main(args: Array[String]): Unit = {

    scalismo.initialize()
    implicit val rng = scalismo.utils.Random(42)

    val ui = ScalismoUI()
    for(i <- 0 to 49) {
      val model = StatisticalModelIO.readStatisticalMeshModel(new java.io.File("data/results/"+i+"_aligned.h5")).get
      val gp = model.gp

      //val modelGroup = ui.createGroup("modelGroup")
     // val ssmView = ui.show(modelGroup, model, "model")
      val referencePointSet = model.referenceMesh.pointSet
      val topFemur = referencePointSet.point(PointId(150))
      val femurPtIDs: Iterator[PointId] = referencePointSet.pointsWithId
        .filter(ptAndId => { // yields tuples with point and ids
          val (pt, id) = ptAndId
          (pt - topFemur).norm < 150
        })
        .map(ptAndId => ptAndId._2) // extract the id's

      val femurModel = model.marginal(femurPtIDs.toIndexedSeq)
      //val femurGroup = ui.createGroup("femurModel")
      //ui.show(femurGroup, femurModel, "femurModel")
      StatisticalModelIO.writeStatisticalMeshModel(femurModel, new java.io.File("data/femur_top/femur_top_" + i + ".h5"))
      println("Finished running for "+i)
    }
  }
}
