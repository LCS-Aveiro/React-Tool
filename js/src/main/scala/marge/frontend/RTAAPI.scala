package rta.frontend

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import rta.syntax.Parser2
import rta.syntax.Program2.{RxGraph, Edge, QName}
import rta.backend.{RxSemantics, CytoscapeConverter, PdlEvaluator, MCRL2, AnalyseLTS}
import rta.syntax.PdlParser
import rta.syntax.RTATranslator
import rta.syntax.Condition

@JSExportTopLevel("RTA")
object RTAAPI {

  private var currentGraph: Option[RxGraph] = None
  private var currentSource: String = ""
  private var history: List[RxGraph] = Nil
  private var showRules: Boolean = true

  private def escapeJson(str: String): String = {
    if (str == null) "" else str
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "")
      .replace("\t", "\\t")
  }

  @JSExport
  def setShowRules(v: Boolean): String = {
    showRules = v
    currentGraph.map(g => generateSimulationJson(g, None)).getOrElse("{}")
  }


  @JSExport
  def findBestPath(targetStr: String): String = {
    currentGraph match {
      case Some(rx) =>
        val adaptedTarget = targetStr.replace('/', '.')
        Parser2.pp[QName](Parser2.qname, adaptedTarget) match {
          case Right(targetQName) =>
            AnalyseLTS.findShortestPath(rx, targetQName) match {
              case Some(steps) => 
                js.JSON.stringify(js.Array(steps: _*))
              case None => 
                """{"error": "Caminho não encontrado ou muito longo."}"""
            }
          case Left(err) => 
            s"""{"error": "Estado inválido: $err"}"""
        }
      case None => 
        """{"error": "Carregue o modelo primeiro."}"""
    }
  }

  @JSExport
  def findPathToValue(condStr: String): js.Any = {
    currentGraph match {
      case Some(rx) =>
        try {

          val formatted = condStr
            .replace("&&", "] && [")
            .replace("AND", "] && [")
          
          val formulaStr = s"[$formatted]"
          
          val formula = PdlParser.parsePdlFormula(formulaStr)
          
          def formulaToCondition(f: rta.syntax.Formula): rta.syntax.Condition = f match {
            case rta.syntax.Formula.CondProp(c) => c
            case rta.syntax.Formula.And(f1, f2) => rta.syntax.Condition.And(formulaToCondition(f1), formulaToCondition(f2))
            case _ => throw new Exception("Use apenas comparações simples unidas por &&")
          }

          val finalCond = formulaToCondition(formula)

          rta.backend.AnalyseLTS.findShortestPathToCondition(rx, finalCond) match {
            case Some(steps) => js.Array(steps: _*)
            case None => js.Dynamic.literal(error = "Caminho não encontrado.")
          }
        } catch {
          case e: Throwable => js.Dynamic.literal(error = s"Erro: ${e.getMessage}")
        }
      case None => js.Dynamic.literal(error = "Modelo não carregado.")
    }
  }

  @JSExport
  def getAllStepsMermaid(): String = {
    currentGraph.map { root =>
      var visited = Set[RxGraph](root)
      var queue = List(root)
      var transitionsStr = List[String]()
      
      var stateToId = Map[RxGraph, Int](root -> 0)
      var idCounter = 0
      
      def getId(g: RxGraph): Int = {
        stateToId.getOrElse(g, {
          idCounter += 1
          stateToId += (g -> idCounter)
          idCounter
        })
      }

      val maxStates = 500 

      while(queue.nonEmpty && visited.size < maxStates) {
        val current = queue.head
        queue = queue.tail
        val sourceId = getId(current)
        
        val edgeNexts = RxSemantics.nextEdge(current)
        for ((edge, nextState) <- edgeNexts) {
          val (from, to, tId, label) = edge
          val targetId = getId(nextState)
          
          val displayLabel = if (tId == label) label.show else s"${label.show}(${tId.show})"
          
          transitionsStr = s"""$sourceId --->|"$displayLabel"| $targetId""" :: transitionsStr
          if (!visited.contains(nextState)) {
            visited += nextState
            queue = queue :+ nextState
          }
        }
      }
       
      val nodeDefinitions = stateToId.map { case (state, id) =>
        val label = s"${state.inits.mkString(", ")}"
        val style = if (state == root) s"\nstyle $id fill:#9ece6a,stroke:#333,stroke-width:2px" else ""
        s"""$id("$label")$style"""
      }.mkString("\n")

      s"""graph LR
         |${transitionsStr.distinct.reverse.mkString("\n")}
         |$nodeDefinitions
         |""".stripMargin
      
    }.getOrElse("graph LR\n0(Nenhum modelo carregado)")
  }

  @JSExport
  def loadModel(sourceCode: String): String = {
    try {
      currentSource = sourceCode
      val graph = Parser2.parseProgram(sourceCode)
      currentGraph = Some(graph)
      history = List(graph)
      generateSimulationJson(graph, None)
    } catch {
      case e: Throwable =>
        s"""{"error": "${escapeJson("Error parsing: " + e.getMessage)}"}"""
    }
  }

  @JSExport
  def takeStep(edgeJson: String): String = {
    currentGraph match {
      case Some(graph) =>
        try {
          val edgeData = js.JSON.parse(edgeJson)
          val from = stringToQName(edgeData.selectDynamic("from").toString)
          val to = stringToQName(edgeData.selectDynamic("to").toString)
          val tId = stringToQName(edgeData.selectDynamic("tId").toString)
          val label = stringToQName(edgeData.selectDynamic("label").toString)
          
          val clickedEdge: Edge = (from, to, tId, label)

          RxSemantics.nextEdge(graph).find(_._1 == clickedEdge) match {
            case Some((_, nextGraph)) =>
              history = nextGraph :: history
              currentGraph = Some(nextGraph)
              generateSimulationJson(nextGraph, Some(clickedEdge))
            case None => s"""{"error": "Transição inválida."}"""
          }
        } catch {
          case e: Throwable => s"""{"error": "${escapeJson(e.getMessage)}"}"""
        }
      case None => """{"error": "Nenhum modelo carregado."}"""
    }
  }

  @JSExport
  def undo(): String = {
    if (history.size > 1) {
      history = history.tail
      currentGraph = history.headOption
      generateSimulationJson(currentGraph.get, None)
    } else {
      currentGraph.map(g => generateSimulationJson(g, None)).getOrElse("{}")
    }
  }


  @JSExport
  def getMcrl2(): String = currentGraph.map(g => MCRL2(g)).getOrElse("Modelo vazio")

  @JSExport
  def translateToGLTS(): String = {
    currentGraph match {
      case Some(g) => RTATranslator.translate_syntax(g, currentSource)
      case None => "Error: Please load a model first."
    }
  }

  @JSExport
  def checkProblems(): String = {
    currentGraph.map { g =>
      AnalyseLTS.randomWalk(g)._4 match {
        case Nil => "Nenhum problema encontrado."
        case m => m.mkString("\n")
      }
    }.getOrElse("Modelo vazio")
  }

  @JSExport
  def getStats(): String = {
    currentGraph.map { root =>
      var visited = Set[RxGraph]()
      var toVisit = List(root)
      var edgesCount = 0
      val limit = 2000
      
      while(toVisit.nonEmpty && visited.size < limit) {
        val current = toVisit.head
        toVisit = toVisit.tail
        if (!visited.contains(current)) {
           visited += current
           val nexts = RxSemantics.nextEdge(current).map(_._2)
           edgesCount += nexts.size
           toVisit = toVisit ++ nexts.toList
        }
      }
      val msg = if (visited.size >= limit) s" (parou após $limit estados)" else ""
      s"""== Estatísticas ==\nEstados: ${visited.size}$msg\nTransições: $edgesCount"""
    }.getOrElse("Modelo vazio")
  }

  @JSExport
  def runPdl(stateStr: String, formulaStr: String): String = {
    currentGraph match {
      case Some(rx) =>
        try {
          val adaptedState = stateStr.replace('/', '.')
          Parser2.pp[QName](Parser2.qname, adaptedState) match {
            case Left(err) => s"Error parsing state '$stateStr': $err"
            case Right(startState) =>
              if (!rx.states.contains(startState)) {
                 s"State '${startState.show}' not found in the current model."
              } else {
                 val formula = PdlParser.parsePdlFormula(formulaStr)
                 val result = PdlEvaluator.evaluateFormula(startState, formula, rx)
                 s"Result: $result"
              }
          }
        } catch {
          case e: Throwable => 
            val msg = if (e.getMessage != null) e.getMessage else e.toString
            s"Evaluation Error: $msg"
        }
      case None => "Model not loaded."
    }
  }

  @JSExport
  def getExamples(): String = {
    val examples = List(
      "Simple" ->
    """name Simple
      |init s0
      |s0 ---> s1: a
      |s1 ---> s0: b
      |a  --! a: offA""".stripMargin,

  "Conditions" ->
    """name Conditions
      |int counter = 0
      |init start
      |start ---> middle: step1  if (counter < 2) then {
      |  counter' := counter + 1
      |}
      |middle ---> endN: activateStep2 if (counter == 1)""".stripMargin,
  "pontes" ->
      """name pontes
      |init West
      |int bridges_crossed = 0
      |int TRUE = 1
      |West - p1_wn -> North : b1 bridges_crossed' := bridges_crossed + 1
      |North - p1_nw -> West : b1 bridges_crossed' := bridges_crossed + 1
      |b1 --! b1 : r1
      |West - p2_wn -> North : b2 bridges_crossed' := bridges_crossed + 1
      |North - p2_nw -> West : b2 bridges_crossed' := bridges_crossed + 1
      |b2 --! b2 : r2
      |West - p3_ws -> South : b3 bridges_crossed' := bridges_crossed + 1
      |South - p3_sw -> West : b3  bridges_crossed' := bridges_crossed + 1
      |b3 --! b3 : r3
      |West - p4_ws -> South : b4  bridges_crossed' := bridges_crossed + 1
      |South - p4_sw -> West : b4 bridges_crossed' := bridges_crossed + 1
      |b4 --! b4 : r4
      |West - p5_we -> East : b5 bridges_crossed' := bridges_crossed + 1
      |East - p5_ew -> West : b5 bridges_crossed' := bridges_crossed + 1
      |b5 --! b5 : r5
      |North - p6_ne -> East : b6 bridges_crossed' := bridges_crossed + 1
      |East - p6_en -> North : b6 bridges_crossed' := bridges_crossed + 1
      |b6 --! b6 : r6
      |South - p7_se -> East : b7 bridges_crossed' := bridges_crossed + 1
      |East - p7_es -> South : b7 bridges_crossed' := bridges_crossed + 1
      |b7 --! b7 : r7""".stripMargin,
  "TravessiaRio" ->
  """name TravessiaRio
      |init West
      |int farmer = 0; int wolf = 0 ; int goat = 0; int cabbage = 0
      |East ---> West : solo if (farmer == 1 AND wolf != goat AND goat != cabbage) then { 
      |    farmer' := 0 
      |}
      |East ---> West : take_goat if (farmer == 1 AND goat == 1) then { 
      |    farmer' := 0; goat' := 0 
      |}
      |East ---> West : take_wolf if (farmer == 1 AND wolf == 1 AND goat != cabbage) then { 
      |    farmer' := 0; wolf' := 0 
      |}
      |East ---> West : take_cabbage if (farmer == 1 AND cabbage == 1 AND wolf != goat) then { 
      |    farmer' := 0; cabbage' := 0 
      |}
      |West ---> East : solo if (farmer == 0 AND wolf != goat AND goat != cabbage) then { 
      |    farmer' := 1 
      |}
      |West ---> East : take_goat if (farmer == 0 AND goat == 0) then { 
      |    farmer' := 1; goat' := 1 
      |}
      |West ---> East : take_wolf if (farmer == 0 AND wolf == 0 AND goat != cabbage) then { 
      |    farmer' := 1; wolf' := 1 
      |}
      |West ---> East : take_cabbage if (farmer == 0 AND cabbage == 0 AND goat != wolf) then { 
      |    farmer' := 1; cabbage' := 1 
      |}""".stripMargin,
  "GRG" ->
   """name GRG
      |int a_active   = 1
      |int b_active   = 0
      |int c_active = 0
      |
      |init s0
      |
      |s0 ---> s1: aa  if (a_active == 1) then {
      |  b_active' := 1;
      |  if (c_active == 1) then {
      |  	a_active' := 0
      |  }
      |}
      |
      |s1 ---> s0: bb  if (b_active == 1) then {
      |  c_active' := 1;
      |  if (a_active == 0) then {
      |  	b_active' := 0
      |  }
      |}
      |
      |s1 ---> s2: cc  if (c_active == 1)
      |
      |
      |aa --! aa: offA2 disabled
      |aa ->> bb: onB if (b_active == 0)
      |bb ->> offA2: onOffA if (c_active == 0)
      |""".stripMargin,

  "Vending (max eur1)" ->
    """name Vending1eur
      |init Insert
      |Insert ---> Coffee: ct50
      |Insert ---> Chocolate: eur1
      |Coffee ---> Insert: GetCoffee
      |Chocolate ---> Insert: GetChoc
      |
      |eur1 --! ct50
      |eur1 --! eur1
      |ct50 --! ct50: lastct50 disabled
      |ct50 --! eur1
      |ct50 ->> lastct50""".stripMargin,

  "Vending (max 3prod)" ->
    """name Vending3prod
      |init pay
      |pay ---> select: insertCoin
      |select ---> soda: askSoda
      |select ---> beer: askBeer
      |soda ---> pay: getSoda
      |beer ---> pay: getBeer
      |
      |askSoda --! askSoda: noSoda disabled
      |askBeer --! askBeer: noBeer
      |askSoda ->> noSoda""".stripMargin,
    "RubiksSolver" ->
     """name RubiksSolver
      |int true = 1
      |int u1 = 3
      |int u2 = 3
      |int u3 = 2
      |int u4 = 2
      |int d1 = 0
      |int d2 = 0
      |int d3 = 1
      |int d4 = 5
      |int f1 = 3
      |int f2 = 2
      |int f3 = 4
      |int f4 = 4
      |int b1 = 5
      |int b2 = 1
      |int b3 = 4
      |int b4 = 3
      |int l1 = 5
      |int l2 = 1
      |int l3 = 5
      |int l4 = 4
      |int r1 = 0
      |int r2 = 0
      |int r3 = 2
      |int r4 = 1
      |
      |init s0
      |
      |s0 ---> s0 : R if (true==1) then { r1' := r3; r2' := r1; r4' := r2; r3' := r4; u2' := f2; u4' := f4; f2' := d2; f4' := d4; d2' := b3; d4' := b1; b3' := u2; b1' := u4 }
      |s0 ---> s0 : Ri if (true==1) then { r3' := r1; r1' := r2; r2' := r4; r4' := r3; f2' := u2; f4' := u4; d2' := f2; d4' := f4; b3' := d2; b1' := d4; u2' := b3; u4' := b1 }
      |s0 ---> s0 : U if (true==1) then { u1' := u3; u2' := u1; u4' := u2; u3' := u4; f1' := r1; f2' := r2; r1' := b1; r2' := b2; b1' := l1; b2' := l2; l1' := f1; l2' := f2 }
      |s0 ---> s0 : Ui if (true==1) then { u3' := u1; u1' := u2; u2' := u4; u4' := u3; r1' := f1; r2' := f2; b1' := r1; b2' := r2; l1' := b1; l2' := b2; f1' := l1; f2' := l2 }
      |s0 ---> s0 : F if (true==1) then { f1' := f3; f2' := f1; f4' := f2; f3' := f4; u3' := r2; u4' := r4; r2' := d2; r4' := d1; d2' := l3; d1' := l1; l3' := u3; l1' := u4 }
      |s0 ---> s0 : Fi if (true==1) then { f3' := f1; f1' := f2; f2' := f4; f4' := f3; r2' := u3; r4' := u4; d2' := r2; d1' := r4; l3' := d2; l1' := d1; u3' := l3; u4' := l1 }""".stripMargin
        
    )
    "{" + examples.map{ case (k,v) => s""""$k": ${js.JSON.stringify(v)}""" }.mkString(",") + "}"
  }


  @JSExport
  def getCurrentStateText(): String = currentGraph.map(_.toString).getOrElse("")

  @JSExport
  def getCurrentStateMermaid(): String = currentGraph.map(g => RxGraph.toMermaid(g)).getOrElse("")

  @JSExport
  def getCurrentStateMermaidSimple(): String = currentGraph.map(g => RxGraph.toMermaidPlain(g)).getOrElse("")


  @JSExport
  def translateToExplicit(): String = {
    currentGraph match {
      case Some(g) => 
        rta.backend.ExplicitTranslatorV4.translate(g, "Rio")
      case None => "Load model first"
    }
  }

  private def stringToQName(str: String): QName = if (str.isEmpty) QName(Nil) else QName(str.split('/').toList)
  
  private def generateSimulationJson(graph: RxGraph, traversedEdge: Option[Edge]): String = {
     val graphElementsJson = CytoscapeConverter(graph,showRules)
     
     val eventTransitions = RxSemantics.nextEdge(graph).map(_._1)
     val eventTransitionsJson = eventTransitions.map { case (from, to, tId, label) =>
       val displayName = if (tId == label) label.show else s"${label.show}(${tId.show})"
       s"""{"from":"$from", "to":"$to", "tId":"$tId", "label":"$label", "displayName":"$displayName", "isDelay": false}"""
     }.mkString(",")

     // Transição de delay removida
     val allEnabledTransitions = eventTransitionsJson

     val valEnvJson = graph.val_env.map { case (n, v) => s""""${n.show}": $v""" }.mkString(",")

     val traversedJson = traversedEdge match {
       case Some((from, to, tId, label)) => s"""{"from":"$from", "to":"$to", "tId":"$tId", "label":"$label"}"""
       case None => "null"
     }

     s"""
       |{
       |  "graphElements": $graphElementsJson,
       |  "panelData": { 
       |     "enabled": [$allEnabledTransitions], 
       |     "variables": {$valEnvJson}, 
       |     "canUndo": ${history.size > 1} 
       |  },
       |  "lastTransition": $traversedJson
       |}
       |""".stripMargin
  }
}