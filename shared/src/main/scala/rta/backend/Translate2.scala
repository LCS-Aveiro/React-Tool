package rta.backend

import rta.syntax.Program2.{RxGraph, QName, Edge}
import rta.backend.RxSemantics
import scala.collection.mutable

object ExplicitTranslatorV4 {

  def translate(root: RxGraph, modelName: String): String = {
    val visited = mutable.Map[RxGraph, String]()
    val queue = mutable.Queue[RxGraph]()
    val allTransitions = mutable.ListBuffer[(String, String, String, String, String)]() 
    
    def getSafeName(g: RxGraph): String = {
      val loc = g.inits.headOption.map(_.show).getOrElse("s")
      val vars = g.val_env.toList.sortBy(_._1.show).map(_._2).mkString("")
      s"${loc}_$vars"
    }

    queue.enqueue(root)
    visited(root) = getSafeName(root)
    val initialStateName = visited(root)

    var edgeCount = 0
    while (queue.nonEmpty) {
      val current = queue.dequeue()
      val currentName = visited(current)
      val nextSteps = RxSemantics.nextEdge(current)
      
      for ((edge, nextGraph) <- nextSteps) {
        if (!visited.contains(nextGraph)) {
          visited(nextGraph) = getSafeName(nextGraph)
          queue.enqueue(nextGraph)
        }
        edgeCount += 1
        val targetName = visited(nextGraph)
        val originalLabel = edge._4.show
        val uniqueLabel = s"${originalLabel}_$currentName"
        
        allTransitions += ((currentName, targetName, s"t$edgeCount", uniqueLabel, originalLabel))
      }
    }

    val sb = new StringBuilder()
    sb.append(s"name ${modelName}_Explicit\n")
    sb.append(s"init $initialStateName\n\n")


    for ((from, to, id, uniqueLbl, originalLbl) <- allTransitions) {
      val status = if (from == initialStateName) "" else "disabled"
      sb.append(s"$from - $id -> $to : $uniqueLbl $status\n")
    }

    sb.append("\n// --- Reconfiguração baseada em Labels Únicas ---\n")


    val transitionsBySource = allTransitions.groupBy(_._1)

    for ((from, to, id, uniqueLbl, _) <- allTransitions) {
      sb.append(s"// Ao realizar $uniqueLbl:\n")

      transitionsBySource.getOrElse(from, Nil).foreach { case (_, _, _, lblToDisable, _) =>
        val ruleLabel = s"deact_${uniqueLbl}_${lblToDisable}"
        sb.append(s"$uniqueLbl --! $lblToDisable : $ruleLabel\n")
      }

      transitionsBySource.getOrElse(to, Nil).foreach { case (_, _, _, lblToEnable, _) =>
        val ruleLabel = s"act_${uniqueLbl}_${lblToEnable}"
        sb.append(s"$uniqueLbl ->> $lblToEnable : $ruleLabel\n")
      }
      sb.append("\n")
    }

    sb.toString()
  }
}