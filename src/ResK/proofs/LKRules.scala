package ResK.proofs

import ResK.judgments._
import ResK.expressions.{E,Var}
import ResK.formulas._
import ResK.positions._


class Axiom(override val mainFormulas: Sequent) extends SequentProof("Ax",Nil,Map())
with NoImplicitContraction {
  override def activeAncestry(f: E, premise: SequentProof) = throw new Exception("Active formulas in axioms have no ancestors.")
}

class AndL(val premise:SequentProof, val auxL:E, val auxR:E)
extends SequentProof("AndL", premise::Nil, Map(premise -> Sequent(auxL::auxR::Nil,Nil)))
with SingleMainFormula with Left with NoImplicitContraction{
  override val mainFormula = And(auxL,auxR)
}

class AndR(val leftPremise:SequentProof, val rightPremise:SequentProof, val auxL:E, val auxR:E)
extends SequentProof("AndR", leftPremise::rightPremise::Nil,
                      Map(leftPremise -> Sequent(Nil,auxL), rightPremise -> Sequent(Nil,auxR)))
with NoImplicitContraction with SingleMainFormula with Right  {
  override val mainFormula = And(auxL,auxR)
}

class AllL(val premise:SequentProof, val aux:E, val v:Var, val pl:List[Position])
extends SequentProof("AllL", premise::Nil,Map(premise -> Sequent(aux,Nil)))
with SingleMainFormula with Left with NoImplicitContraction {
  override val mainFormula = All(aux,v,pl)
}

class ExR(val premise:SequentProof, val aux:E, val v:Var, val pl:List[Position])
extends SequentProof("ExR", premise::Nil,Map(premise -> Sequent(Nil,aux)))
with SingleMainFormula with Right with NoImplicitContraction {
  override val mainFormula = Ex(aux,v,pl)
}

trait EigenvariableCondition extends SequentProof {
  val eigenvar: Var
  require(!conclusionContext.ant.exists(e => (eigenvar occursIn e)) &&
          !conclusionContext.suc.exists(e => (eigenvar occursIn e)))
}

class AllR(val premise:SequentProof, val aux:E, val v:Var, val eigenvar:Var)
extends SequentProof("AllR", premise::Nil,Map(premise -> Sequent(Nil,aux)))
with SingleMainFormula with Right with NoImplicitContraction
with EigenvariableCondition {
  override val mainFormula = All(aux,v,eigenvar)
}

class ExL(val premise:SequentProof, val aux:E, val v:Var, val eigenvar:Var)
extends SequentProof("ExL", premise::Nil,Map(premise -> Sequent(aux,Nil)))
with SingleMainFormula with Left with NoImplicitContraction 
with EigenvariableCondition {
  override val mainFormula = Ex(aux,v,eigenvar)
}


abstract class AbstractCut(val leftPremise:SequentProof, val rightPremise:SequentProof, 
                            val auxL:E, val auxR:E)
extends SequentProof("Cut",leftPremise::rightPremise::Nil,
                      Map(leftPremise -> Sequent(Nil,auxL),
                          rightPremise -> Sequent(auxR,Nil)))
with NoMainFormula {
  require(auxL == auxR)
}

class Cut(leftPremise:SequentProof, rightPremise:SequentProof, auxL:E, auxR:E)
extends AbstractCut(leftPremise, rightPremise, auxL, auxR)
with NoImplicitContraction 

class CutIC(leftPremise:SequentProof, rightPremise:SequentProof, auxL:E, auxR:E)
extends AbstractCut(leftPremise, rightPremise, auxL, auxR)
with ImplicitContraction 





object Axiom {
  def apply(conclusion: Sequent) = new Axiom(conclusion)
  def unapply(p: SequentProof) = p match {
    case p: Axiom => Some(p.conclusion)
    case _ => None
  }
}
object AllR {
  def apply(premise:SequentProof, aux:E, v:Var, eigenvar:Var) = new AllR(premise,aux,v,eigenvar)
  def unapply(p: SequentProof) = p match {
    case p: AllR => Some((p.premise,p.aux,p.v,p.eigenvar))
    case _ => None
  }
}
object AllL {
  def apply(premise:SequentProof, aux:E, v:Var, pl:List[Position]) = new AllL(premise,aux,v,pl)
  def unapply(p: SequentProof) = p match {
    case p: AllL => Some((p.premise,p.aux,p.v,p.pl))
    case _ => None
  }
}
object AndL {
  def apply(premise: SequentProof, auxL:E, auxR:E) = new AndL(premise,auxL,auxR)
  def unapply(p: SequentProof) = p match {
    case p: AndL => Some((p.premise,p.auxL,p.auxR))
    case _ => None
  }
}
object AndR {
  def apply(leftPremise: SequentProof, rightPremise: SequentProof, auxL:E, auxR:E) = new AndR(leftPremise,rightPremise,auxL,auxR)
  def unapply(p: SequentProof) = p match {
    case p: AndR => Some((p.leftPremise,p.rightPremise,p.auxL,p.auxR))
    case _ => None
  }
}
//ToDo: Companion objects for ExL and ExR are missing. They are not needed yet.
object Cut {
  def apply(leftPremise: SequentProof, rightPremise: SequentProof, auxL:E, auxR:E) = new Cut(leftPremise,rightPremise,auxL,auxR)
  def unapply(p: SequentProof) = p match {
    case p: Cut => Some((p.leftPremise,p.rightPremise,p.auxL,p.auxR))
    case _ => None
  }
}
object CutIC {
  def apply(leftPremise: SequentProof, rightPremise: SequentProof, auxL:E, auxR:E) = new CutIC(leftPremise,rightPremise,auxL,auxR)
  def apply(premise1:SequentProof, premise2:SequentProof) = {
    def findPivots(p1:SequentProof, p2:SequentProof): Option[(E,E)] = {
      for (auxL <- p1.conclusion.suc; auxR <- p2.conclusion.ant) if (auxL == auxR) return Some(auxL,auxR)
      return None
    }
    findPivots(premise1,premise2) match {
      case Some((auxL,auxR)) => new CutIC(premise1,premise2,auxL,auxR)
      case None => findPivots(premise2,premise1) match {
        case Some((auxL,auxR)) => new CutIC(premise2,premise1,auxL,auxR)
        case None => throw new Exception("Resolution: the conclusions of the given premises are not resolvable.")
      }
    }
  }
  def unapply(p: SequentProof) = p match {
    case p: CutIC => Some((p.leftPremise,p.rightPremise,p.auxL,p.auxR))
    case _ => None
  }
}

