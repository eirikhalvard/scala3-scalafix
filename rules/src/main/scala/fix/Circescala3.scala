package fix

import scalafix.v1.{Patch, _}

import scala.meta._

class Circescala3 extends SemanticRule("Circescala3") {

  override def fix(implicit doc: SemanticDocument): Patch = {

    //    println("Tree.syntax: " + doc.tree.syntax)
    //    println("Tree.structure: " + doc.tree.structure)
    //    println("Tree.structureLabeled: " + doc.tree.structureLabeled)

    val config = Set(
      Circescala3.Config("io.circe.Encoder.AsObject", "Encoder.AsObject"),
      Circescala3.Config("io.circe.Encoder", "Encoder.AsObject"),
      Circescala3.Config("io.circe.Decoder", "Decoder"),
      Circescala3.Config("io.circe.Codec.AsObject", "Codec.AsObject"),
      Circescala3.Config("io.circe.Codec", "Codec.AsObject")
    ).map(c => c.typ -> c).toMap

    doc.tree.collect { case CaseClassWithCompanion(c, SemiAutoDerived(items)) =>
      items.flatMap(item => config.get(item.deriveType).map(item -> _)) match {
        case Nil => Patch.empty
        case toRewrite =>
          val base = if (c.templ. derives.isEmpty) " derives " else ", "
          val derivePatch = Patch.addRight(c, base ++ toRewrite.map(_._2.derived).mkString(", "))
          val removeImplicitPatches = Patch.removeTokens(toRewrite.flatMap(_._1.defnVal.tokens))
          derivePatch + removeImplicitPatches
      }
    }.asPatch
  }

}

object Circescala3 {

  case class Config(
      typ: String,
      derived: String
  )
}

object CaseClassWithCompanion {
  def unapply(t: Tree): Option[(Defn.Class, Defn.Object)] =
    t match {
      case c @ Defn.Class(mods, cName, _, _, _) if isCaseClass(mods) =>
        println(c.structure)
        c.parent.flatMap { st =>
          st.children.collectFirst {
            case o @ Defn.Object(_, oName, _) if cName.value == oName.value => c -> o
          }
        }
      case _ => None
    }

  private def isCaseClass(mods: List[Mod]) =
    mods.exists {
      case _: Mod.Case => true
      case _           => false
    }
}

case class SemiAutoDerived(
    deriveType: String,
    defnVal: Defn.Val
)
object SemiAutoDerived {

  def unapply(o: Defn.Object)(implicit doc: SemanticDocument): Option[List[SemiAutoDerived]] =
    nonEmptyList(o.templ.stats.collect {
      case v @ Defn.Val(mods, _, Some(typeApply @ Type.Apply(_, (typeName: Type.Name) :: Nil)), t)
          if matchingType(o, typeName) && hasImplicitMod(mods) && isSemiAuto(t) =>
        SemiAutoDerived(typeApply.symbol.normalized.value.dropRight(1), v)
    })

  private def matchingType(o: Defn.Object, typeName: Type.Name) =
    typeName.value == o.name.value

  private def isSemiAuto(t: Term)(implicit doc: SemanticDocument) =
    t.symbol.normalized.value.contains(".semiauto.")

  private def nonEmptyList[A](l: List[A]): Option[List[A]] =
    if (l.isEmpty) None else Some(l)

  private def hasImplicitMod(mods: List[Mod]) =
    mods.exists {
      case _: Mod.Implicit => true
      case _               => false
    }
}
