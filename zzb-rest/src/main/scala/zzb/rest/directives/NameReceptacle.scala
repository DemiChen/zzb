package zzb.rest.directives

import zzb.rest.unmarshalling.{ FromStringOptionDeserializer ⇒ FSOD, Deserializer }

trait ToNameReceptaclePimps {
  implicit def symbol2NR(symbol: Symbol) = new NameReceptacle[String](symbol.name)
  implicit def string2NR(string: String) = new NameReceptacle[String](string)
}

case class NameReceptacle[A](name: String) {
  def as[B] = NameReceptacle[B](name)
  def as[B](deserializer: FSOD[B]) = NameDeserializerReceptacle(name, deserializer)
  def ? = as[Option[A]]
  def ?[B](default: B) = NameDefaultReceptacle(name, default)
  def ![B](requiredValue: B) = RequiredValueReceptacle(name, requiredValue)
}

case class NameDeserializerReceptacle[A](name: String, deserializer: FSOD[A]) {
  def ? = NameDeserializerReceptacle(name, Deserializer.liftToTargetOption(deserializer))
  def ?(default: A) = NameDeserializerDefaultReceptacle(name, deserializer, default)
  def !(requiredValue: A) = RequiredValueDeserializerReceptacle(name, deserializer, requiredValue)
}

case class NameDefaultReceptacle[A](name: String, default: A)

case class RequiredValueReceptacle[A](name: String, requiredValue: A)

case class NameDeserializerDefaultReceptacle[A](name: String, deserializer: FSOD[A], default: A)

case class RequiredValueDeserializerReceptacle[A](name: String, deserializer: FSOD[A], requiredValue: A)