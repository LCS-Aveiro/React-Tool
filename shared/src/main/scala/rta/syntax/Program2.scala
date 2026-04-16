package rta.syntax

import rta.backend.RxSemantics
import rta.syntax.Program2.EdgeMap
import rta.syntax.{Condition, CounterUpdate, UpdateExpr, Statement, UpdateStmt, IfThenStmt}
import scala.annotation.tailrec
import scala.language.implicitConversions

object Program2:

  type Rel[A,B] = Map[A,Set[B]]
  def empty[A,B] = Map[A,Set[B]]().withDefaultValue(Set())
  def add[A,B](ab:(A,B), r:Rel[A,B]) = r + (ab._1 -> (r(ab._1)+(ab._2)))
  def join[A,B](r1:Rel[A,B], r2:Rel[A,B]) = r1 ++ (r2.map(ab => ab._1 -> (r1(ab._1)++(ab._2))))

  private def isGlobalControlVar(q: QName): Boolean = q.n.mkString.contains("_")

  case class QName(n:List[String]):
    override def toString = n.mkString("/")
    def show = if n.isEmpty then "-" else toString
    def /(other:QName) = if (other.n.isEmpty) other else if (n.isEmpty) other else QName(n ::: other.n)
    def /(other:String) = QName(n:::List(other))

    def /(e:EdgeMap):EdgeMap =
      e.map((src, targets) => (this / src) -> targets.map((to, id, lbl) => (this / to, this / id, this / lbl)))

    def /(es:Edges): Edges =
      es.map((src, to, id, lbl) => (this / src, this / to, this / id, this / lbl))

    def /-(lblsMap:Map[QName,Edges]): Map[QName,Edges] =
      lblsMap.map((lbl, edges) => (this / lbl) -> (this / edges))

    def /-(ns:Set[QName]): Set[QName] =
      ns.map(n => this / n)
    
    def scope: QName = if n.isEmpty then this else QName(n.init)

    def /(rx: RxGraph): RxGraph =
      rx.copy( 
        edg = this / rx.edg,
        on = this / rx.on,
        off = this / rx.off,
        lbls = this /- rx.lbls,
        inits = this /- rx.inits,
        act = this / rx.act,
        val_env = rx.val_env.map { case (k, v) => (this / k) -> v },
        edgeConditions = rx.edgeConditions.map { case (edge, condOpt) =>
          (this / edge._1, this / edge._2, this / edge._3, this / edge._4) -> condOpt.map(c => applyPrefixToCondition(this, c))
        },
        edgeUpdates = rx.edgeUpdates.map { case (edge, stmtList) =>
          (this / edge._1, this / edge._2, this / edge._3, this / edge._4) -> stmtList.map(stmt => applyPrefixToStatement(this, stmt))
        }
      )
  
  def applyPrefixToCondition(prefix: QName, cond: Condition): Condition = {
    cond match {
      case Condition.AtomicCond(left, op, right) =>
        val newLeft = if (isGlobalControlVar(left)) left else prefix / left
        val newRight = right match {
          case Left(i) => Left(i)
          case Right(q) => if (isGlobalControlVar(q)) Right(q) else Right(prefix / q)
        }
        Condition.AtomicCond(newLeft, op, newRight)
      case Condition.And(l, r) => Condition.And(applyPrefixToCondition(prefix, l), applyPrefixToCondition(prefix, r))
      case Condition.Or(l, r) => Condition.Or(applyPrefixToCondition(prefix, l), applyPrefixToCondition(prefix, r))
    }
  }

  def applyPrefixToStatement(prefix: QName, stmt: Statement): Statement = {
    stmt match {
      case UpdateStmt(upd) =>
        val newVar = if (isGlobalControlVar(upd.variable)) upd.variable else prefix / upd.variable
        val newExpr = upd.expr match {
            case UpdateExpr.Add(v, e) => UpdateExpr.Add(if(isGlobalControlVar(v)) v else prefix/v, e match {
                case Right(q) if !isGlobalControlVar(q) => Right(prefix/q)
                case other => other
            })
            case UpdateExpr.Sub(v, e) => UpdateExpr.Sub(if(isGlobalControlVar(v)) v else prefix/v, e match {
                case Right(q) if !isGlobalControlVar(q) => Right(prefix/q)
                case other => other
            })
            case UpdateExpr.Var(q) if !isGlobalControlVar(q) => UpdateExpr.Var(prefix/q)
            case other => other
        }
        UpdateStmt(upd.copy(variable = newVar, expr = newExpr))
      case IfThenStmt(cond, thenStmts) =>
        IfThenStmt(
          applyPrefixToCondition(prefix, cond),
          thenStmts.map(s => applyPrefixToStatement(prefix, s))
        )
    }
  }

  type Edge = (QName, QName, QName, QName)
  type Edges = Set[Edge]
  type EdgeMap = Rel[QName, (QName, QName, QName)]

  def showEdge(e: Edge): String = {
    val (from, to, transId, label) = e
    if (transId == label) {
      s"${from.show} ---> ${to.show} : ${label.show}"
    } else {
      s"${from.show} -${transId.show}-> ${to.show} : ${label.show}"
    }
  }
  def showEdges(abc:Edges): String =
    abc.map(showEdge).mkString(", ")

  private def showEdgeMap(abc:EdgeMap): String =
    val es = for (a, bcs) <- abc.toSet; (b, id, lbl) <- bcs yield (a, b, id, lbl)
    showEdges(es)


  case class RxGraph(edg:EdgeMap,
                     on:EdgeMap, off: EdgeMap,
                     lbls: Map[QName,Edges],
                     inits: Set[QName],
                     act: Edges,
                     val_env: Map[QName, Int], 
                     edgeConditions: Map[Edge, Option[Condition]], 
                     edgeUpdates: Map[Edge, List[Statement]] 
                    ):

    def showSimple: String =
      s"[at] ${inits.mkString(",")}" +
      s"${if val_env.nonEmpty then s" [vars] ${val_env.map(kv => s"${kv._1}=${kv._2}").mkString(", ")}" else ""}" +
      s" [active] ${showEdges(act)}"

    override def toString: String =
      s"""[init]  ${inits.mkString(",")}
         |[vars]  ${val_env.map(kv => s"${kv._1}=${kv._2}").mkString(", ")}
         |[act]   ${showEdges(act)}
         |[edges] ${showEdgeMap(edg)}
         |[on]    ${showEdgeMap(on)}
         |[off]   ${showEdgeMap(off)}
         |[conds] ${edgeConditions.filter(_._2.isDefined).map(kv => s"${showEdge(kv._1)} -> ${kv._2.get.toMermaidString}").mkString(", ")}
         |[upd]   ${edgeUpdates.filter(_._2.nonEmpty).map(kv => s"${showEdge(kv._1)} -> ${kv._2.map(_.toString).mkString("; ")}").mkString(", ")}"""
    
    def states =
      for (src, dests) <- edg.toSet; (d, _, _) <- dests; st <- Set(src, d) yield st

    def addEdge(s1:QName, s2:QName, transId:QName, label:QName, cond: Option[Condition] = None, upd: List[Statement] = Nil) = {
      val edge: Edge = (s1, s2, transId, label)
      this.copy(
        edg = add(s1 -> (s2, transId, label), edg), 
        lbls = add(label -> edge, lbls),           
        act = act + edge,
        edgeConditions = edgeConditions + (edge -> cond),
        edgeUpdates = edgeUpdates + (edge -> upd)
      )
    }

    def addOn(s1: QName, s2: QName, tId: QName, l: QName, cond: Option[Condition] = None, upd: List[Statement] = Nil) = {
      val edge: Edge = (s1, s2, tId, l)
      this.copy(
        on = add(s1 -> (s2, tId, l), on),
        lbls = add(l -> edge, lbls),
        act = act + edge,
        edgeConditions = edgeConditions + (edge -> cond),
        edgeUpdates = edgeUpdates + (edge -> upd))
    }

    def addOff(s1: QName, s2: QName, tId: QName, l: QName, cond: Option[Condition] = None, upd: List[Statement] = Nil) = {
      val edge: Edge = (s1, s2, tId, l)
      this.copy(
        off = add(s1 -> (s2, tId, l), off),
        lbls = add(l -> edge, lbls),
        act = act + edge,
        edgeConditions = edgeConditions + (edge -> cond),
        edgeUpdates = edgeUpdates + (edge -> upd))
    }

    def deactivate(s1:QName, s2:QName, tId:QName, l:QName) =
      this.copy(act = act - ((s1, s2, tId, l)))

    def addInit(s:QName) =
      this.copy(inits = inits + s)

    def addVariable(name: QName, value: Int) =
      this.copy(val_env = val_env + (name -> value))

    def ++(r:RxGraph) =
      RxGraph(
        join(edg,r.edg),join(on,r.on),join(off,r.off),
        join(lbls,r.lbls),inits++r.inits,act++r.act,
        val_env ++ r.val_env, 
        edgeConditions ++ r.edgeConditions, 
        edgeUpdates ++ r.edgeUpdates 
      )


  object RxGraph: 
    def apply(): RxGraph = RxGraph(
      Map().withDefaultValue(Set()),Map().withDefaultValue(Set()),
      Map().withDefaultValue(Set()),Map().withDefaultValue(Set()),Set(),Set(),
      Map(), Map().withDefaultValue(None), Map().withDefaultValue(Nil))

    def toMermaid(rx: RxGraph): String =
      var i = -1
      def fresh(): Int = {i += 1; i}
      s"flowchart LR\n${
        drawEdges(rx.edg, rx, fresh, ">", "stroke:black, stroke-width:2px",(x,y) => Set(x.toString), withConditions = true)}${
        drawEdges(rx.on, rx, fresh, ">", "stroke:blue, stroke-width:3px",getLabel, withConditions = true)}${
        drawEdges(rx.off,rx, fresh, "x", "stroke:red, stroke-width:3px",getLabel, withConditions = true)}${
        (for s<-rx.inits yield s"  style $s fill:#8f7,stroke:#363,stroke-width:4px\n").mkString
      }"

    def toMermaidPlain(rx: RxGraph): String =
      var i = -1
      def fresh(): Int = {i += 1; i}
      s"flowchart LR\n${
        drawEdges(rx.edg, rx, fresh, ">", "stroke:black, stroke-width:2px",(x,y) => Set(x.toString),simple=true, withConditions = false)}${
        (for s<-rx.inits yield s"  style $s fill:#8f7,stroke:#363,stroke-width:4px\n").mkString 
      }"

    private def getLabel(n: QName, rx: RxGraph): Set[String] =
      for (edge <- rx.lbls.getOrElse(n, Set())) 
        yield cleanId(edge._1, edge._2, edge._3, edge._4)
    
    private def cleanId(a: Any, b: Any, id: Any, lbl: Any): String =
      s"$a$b$id$lbl".replaceAll("[^a-zA-Z0-9]", "")

    private def drawEdges(
      es: EdgeMap,
      rx: RxGraph,
      fresh: () => Int,
      tip: String,
      style: String,
      getEnds: (QName, RxGraph) => Set[String],
      simple: Boolean = false,
      withConditions: Boolean = false
    ): String =
      (for
        (a, bs) <- es.toList
        (b, transId, lbl) <- bs.toList
        a2 <- getEnds(a, rx).toList
        b2 <- getEnds(b, rx).toList
      yield
        val edge: Edge = (a, b, transId, lbl)

        val isGloballyActive = rx.act(edge)
        val isConditionSatisfied = rx.edgeConditions.getOrElse(edge, None) match {
          case None => true 
          case Some(condition) => Condition.evaluate(condition, rx.val_env)
        }

        val line = if (isGloballyActive && isConditionSatisfied) then "---" else "-.-"

        val qNameLabel = if transId == lbl then lbl.show else s"${lbl.show}(${transId.show})"
        val updList = rx.edgeUpdates.getOrElse(edge, Nil)
        val updText = if (withConditions && updList.nonEmpty)  s"{ ${updList.map(_.toMermaidString).mkString("; ")} }"  else ""

        val condOpt = rx.edgeConditions.get(edge).flatten
        val condText = if (withConditions && condOpt.isDefined)  s"[${condOpt.get.toMermaidString}]"  else ""
        val combined = List(condText, qNameLabel, updText).filter(_.nonEmpty).mkString(" ")

        
        val edgeLabel = if combined.nonEmpty then s"|\"${combined}\"|" else ""

        if lbl.n.isEmpty && transId.n.isEmpty then
          s"  $a2 $line$tip $edgeLabel $b2\n" +
          s"  linkStyle ${fresh()} $style\n"
        else if simple then
          s"  $a2 $line$tip $edgeLabel $b2\n" +
          s"  linkStyle ${fresh()} $style\n"
        else
          val anchorId = cleanId(a, b, transId, lbl)
          s"  $a2 $line $anchorId( ) $line$tip $edgeLabel $b2\n" +
          s"  style $anchorId width: 0\n" +
          s"  linkStyle ${fresh()} $style\n" +
          s"  linkStyle ${fresh()} $style\n"
      ).mkString

  object Examples:
    implicit def s2n(str:String): QName = QName(List(str))
    val a = s2n("a")
    val t1 = s2n("t1")
    val s1 = s2n("s1")
    val s2 = s2n("s2")

    val g1 = RxGraph()
      .addInit(s1)
      .addEdge(s1, s2, t1, a)
      .addOff(a, a, s2n("rule1"), s2n("off-a"))

    val counter = RxGraph()
      .addInit("0")
      .addEdge("0", "0", "t_act", "act")
      .addOff("act", "act", "r1", "offAct")
      .deactivate("act", "act", "r1", "offAct")
      .addOn("act", "offAct", "r2", "on1")
      .deactivate("act", "offAct", "r2", "on1")
      .addOn("act", "on1", "r3", "on2")