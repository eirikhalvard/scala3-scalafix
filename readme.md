# Scala 3 Scalafix rules

## SemiAuto

Rewrite implicit derived values to scala 3 syntax

**Configuration:**

```hocon
// default config
SemiAuto.bundle = [all]

// option to specify a bundle of rewrites
SemiAuto.bundle = [circe, doobie]

//Bring your own rewrites.
SemiAuto.rewrites = [
  { typeClass = "my.Decoder", derived =  "Decoder" }
  { typeClass = "my.Encoder", derived =  "Encoder" }
]
```

**Example:**

```scala
// Before
case class Person(name: String)
object Person{
  implicit val codec: Codec.AsObject[Person] = deriveEncoder
}
// After
case class Person(name: String) derives Codec.AsObject
```

## GivenAndUsing

Rewrite implicit keywords to given and using.

**Example:**
```scala
// Before
case class Message(m: String)
object Message {
  implicit show: Show[Message] = ???
}
class Printer(implicit console: Console) {
  def print[A](implicit showA: Show[A]): Unit = ???
}

//After
case class Message(m: String)
object Message {
  given show: Show[Message] = ???
}
class Printer(using console: Console) {
  def print[A](using showA: Show[A]): Unit = ???
}
```

# PackageObjectExport

Rewrite [dropped feature](https://docs.scala-lang.org/scala3/reference/dropped-features/package-objects.html) package objects

**Example:**
```scala
// Before
package foo
package object db extends doobie.syntax.AllSyntax
    with doobie.Aliases
    with doobie.postgres.free.Types
    with doobie.postgres.free.Modules
    with doobie.postgres.free.Instances
    with doobie.postgres.hi.Modules
    with doobie.postgres.Instances
    with doobie.postgres.syntax.ToFragmentOps
    with doobie.postgres.syntax.ToPostgresMonadErrorOps {
  
  def otherUsefulMethod[A](input: String): ConnectioIO[A] = ???
  def otherUsefulMethod2[A](input: Int): ConnectioIO[A] = ???
}

//After
package foo
package db {

  def otherUsefulMethod[A](input: String): ConnectioIO[A] = ???
  def otherUsefulMethod2[A](input: Int): ConnectioIO[A] = ???

  private object dbImpl extends doobie.syntax.AllSyntax
    with doobie.Aliases
    with doobie.postgres.free.Types
    with doobie.postgres.free.Modules
    with doobie.postgres.free.Instances
    with doobie.postgres.hi.Modules
    with doobie.postgres.Instances
    with doobie.postgres.syntax.ToFragmentOps
    with doobie.postgres.syntax.ToPostgresMonadErrorOps
  export dbImpl.*
}
```