/*
 * Copyright 2022 Magnus Madsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.uwaterloo.flix.language.phase.unification

import ca.uwaterloo.flix.language.ast.{SourceLocation, Symbol, Type}
import ca.uwaterloo.flix.util.InternalCompilerException
import ca.uwaterloo.flix.util.collection.Bimap

import scala.collection.immutable.SortedSet
import org.sosy_lab.pjbdd.api.{Builders, DD}
import org.sosy_lab.pjbdd.util.parser.DotExporter
import java.util.concurrent.locks.ReentrantLock

object BddFormula {

  val creator = Builders.bddBuilder().build()
  val lock = new ReentrantLock()

  class BddFormula(val dd: DD) {
    def getDD(): DD = dd
  }

  implicit val AsBoolAlgTrait: BoolAlg[BddFormula] = new BoolAlg[BddFormula] {
    /**
      * Returns `true` if `f` represents TRUE.
      */
    override def isTrue(f: BddFormula): Boolean = f.getDD().isTrue()

    /**
      * Returns `true` if `f` represents FALSE.
      */
    override def isFalse(f: BddFormula): Boolean = f.getDD().isFalse()

    /**
      * Returns a representation of TRUE.
      */
    override def mkTrue: BddFormula = {
      new BddFormula(creator.makeTrue())
    }

    /**
      * Returns a representation of FALSE.
      */
    override def mkFalse: BddFormula = {
      new BddFormula(creator.makeFalse())
    }

    /**
      * Returns a representation of the variable with the given `id`.
      */
    override def mkVar(id: Int): BddFormula = {
      new BddFormula(creator.makeIthVar(id))
    }

    /**
      * Returns a representation of the complement of `f`.
      */
    override def mkNot(f: BddFormula): BddFormula = {
      new BddFormula(creator.makeNot(f.getDD()))
    }

    /**
      * Returns a representation of the disjunction of `f1` and `f2`.
      */
    override def mkOr(f1: BddFormula, f2: BddFormula): BddFormula = {
      new BddFormula(creator.makeOr(f1.getDD(), f2.getDD()))
    }

    /**
      * Returns a representation of the conjunction of `f1` and `f2`.
      */
    override def mkAnd(f1: BddFormula, f2: BddFormula): BddFormula = {
      new BddFormula(creator.makeAnd(f1.getDD(), f2.getDD()))
    }

    /**
      * Returns a representation of the formula `f1 == f2`.
      */
    override def mkEq(f1: BddFormula, f2: BddFormula): BddFormula = {
      new BddFormula(creator.makeXnor(f1.getDD(), f2.getDD()))
    }

    /**
      * Returns the set of free variables in `f`.
      */
    //TODO: Optimize!
    override def freeVars(f: BddFormula): SortedSet[Int] = {
      freeVarsAux(f.getDD())
    }

    private def freeVarsAux(dd: DD): SortedSet[Int] = {
      if (dd.isLeaf()) {
        SortedSet.empty
      } else {
        SortedSet(dd.getVariable()) ++
          freeVarsAux(dd.getLow) ++
          freeVarsAux(dd.getHigh)
      }
    }

    /**
      * Applies the function `fn` to every variable in `f`.
      */
      //TODO: Check correctness
    override def map(f: BddFormula)(fn: Int => BddFormula): BddFormula = {
      val exporter = new DotExporter()
        lock.lock()

        val creator = Builders.bddBuilder().build()

        val x1 = creator.makeIthVar(1)
        val notx1 = creator.makeNot(x1)
        val x2 = creator.makeIthVar(2)

        val nand = creator.makeNand(x1,x2)
        val comp = creator.makeCompose(nand, 1, notx1)

        println("Original f: x1 NAND x2")
        println(exporter.bddToString(nand))


        println("f|1<-x1 using makeCompose")
        println(exporter.bddToString(comp))

        val and1 : DD = creator.makeAnd(creator.makeNot(notx1), creator.restrict(nand, 1, false))
        val and2 : DD = creator.makeAnd(notx1, creator.restrict(nand, 1, true))
        val or = creator.makeOr(and1, and2)

        println("f|1<-x1 using formula")
        println(exporter.bddToString(or))

        System.exit(-1)
        lock.unlock()

      if(f.getDD().isLeaf()) {
        f
      } else {
        val varSet = freeVars(f)
        println("freeVars in map: " + varSet.toString())

        //make x -> x' map
        val maxVar = varSet.max
        val noVars = varSet.size
        val newVarNames = (maxVar+1 to maxVar+noVars).toList
        println("New names for vars: " + newVarNames.toString())
        val varMap = varSet.zip(newVarNames).foldLeft(Bimap.empty[Int, Int]) {
          case (macc, (old_x, new_x)) => macc + (old_x -> new_x)
        }
        println("Bimap: " + varMap.toString())

        var res = f.getDD()

        println("Original DD")
        println(exporter.bddToString(res))

        //for each i in varSet create BddFormula' with primed variables
        //and compose f with BddFormula'
        for (var_i <- varSet) {
          val subst = fn(var_i)
          val substVarSet = freeVars(subst)

          println("substVarSet before: " + substVarSet.toString())
          var substDD = subst.getDD()
          println("SubstDD before")
          println(exporter.bddToString(substDD))

          //create the substitute BDD with the new names
          for (var_j <- substVarSet) {
            val j_prime = varMap.getForward(var_j) match {
              case Some(j) => j
              case None => ??? //should never happen
            }
            substDD = creator.makeReplace(substDD, creator.makeIthVar(var_j), creator.makeIthVar(j_prime))
          }
          println("substVarSet after: " + freeVarsAux(substDD).toString())
          println("SubstDD after")
          println(exporter.bddToString(substDD))

          res = creator.makeCompose(res, var_i, substDD)
          println("DD after substitution on " + var_i)
          println(exporter.bddToString(res))
        }

        val varSetPrime = freeVarsAux(res)
        println("varSetPrime: " + varSetPrime.toString())

        //for each x' map back to x in f
        for (var_i_prime <- varSetPrime) {
          val old_i = varMap.getBackward(var_i_prime) match {
            case Some(i) => i
            case None => ??? //should never happen
          }
          res = creator.makeReplace(res, creator.makeIthVar(var_i_prime), creator.makeIthVar(old_i))
          println("DD after substitution on " + var_i_prime)
          println(exporter.bddToString(res))
        }

        println("varSetRes before return: " + freeVarsAux(res).toString())
        println("res before return")
        val resForm = new BddFormula(res)
        println(exporter.bddToString(res))

        println("")
        println("")
        resForm
      }
    }

    /**
      * Returns a representation equivalent to `f` (but potentially smaller).
      */
    override def minimize(f: BddFormula): BddFormula = f

    /**
      * Returns an environment built from the given types mapping between type variables and formula variables.
      *
      * This environment should be used in the functions [[toType]] and [[fromType]].
      */
    override def getEnv(fs: List[Type]): Bimap[Symbol.KindedTypeVarSym, Int] =
    {
      // Compute the variables in `tpe`.
      val tvars = fs.flatMap(_.typeVars).map(_.sym).to(SortedSet)

      // Construct a bi-directional map from type variables to indices.
      // The idea is that the first variable becomes x0, the next x1, and so forth.
      tvars.zipWithIndex.foldLeft(Bimap.empty[Symbol.KindedTypeVarSym, Int]) {
        case (macc, (sym, x)) => macc + (sym -> x)
      }
    }

    /**
      * Converts the given formula f into a type.
      */
    override def toType(f: BddFormula, env: Bimap[Symbol.KindedTypeVarSym, Int]): Type = {
      createTypeFromBDDAux(f.getDD(), Type.True, env)
    }

    //TODO: Optimize
    private def createTypeFromBDDAux(dd: DD, tpe: Type, env: Bimap[Symbol.KindedTypeVarSym, Int]): Type = {
      if (dd.isLeaf()) {
        return if (dd.isTrue()) tpe else Type.False
      }

      val currentVar = dd.getVariable()
      val typeVar = env.getBackward(currentVar) match {
        case Some(sym) => Type.Var(sym, SourceLocation.Unknown)
        case None => throw InternalCompilerException(s"unexpected unknown ID: $currentVar")
      }

      val lowType = Type.mkApply(Type.And, List(tpe, Type.Apply(Type.Not, typeVar, SourceLocation.Unknown)), SourceLocation.Unknown)
      val lowRes = createTypeFromBDDAux(dd.getLow(), lowType, env)
      val highType = Type.mkApply(Type.And, List(tpe, typeVar), SourceLocation.Unknown)
      val highRes = createTypeFromBDDAux(dd.getHigh(), highType, env)

      (lowRes, highRes) match {
        case (Type.False, Type.False) => Type.False
        case (Type.False, _) => highRes
        case (_, Type.False) => lowRes
        case (_, _) => Type.mkApply(Type.Or, List(lowRes, highRes), SourceLocation.Unknown)
      }
    }

    /**
      * Optional operation. Returns `None` if not implemented.
      *
      * Returns `Some(true)` if `f` is satisfiable (i.e. has a satisfying assignment).
      * Returns `Some(false)` otherwise.
      */
    override def satisfiable(f: BddFormula): Option[Boolean] = Some(!f.getDD().isFalse())
  }
}
