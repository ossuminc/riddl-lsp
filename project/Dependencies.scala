import sbt.*
import sbt.librarymanagement.ModuleID

/** V - Dependency Versions object */

object V {
  val lang3 = "3.14.0"
  val pureconfig = "0.17.6"
  val scalacheck = "1.17.0"
  val scalatest = "3.2.18"
  val scopt = "4.1.0"
  val slf4j = "2.0.4"
  val lsp4j = "0.22.0"
  val ossumRiddl = "0.42.0"
}

object Dep {
  val lang3 = "org.apache.commons" % "commons-lang3" % V.lang3
  val pureconfig = "com.github.pureconfig" %% "pureconfig-core" % V.pureconfig
  val scalactic = "org.scalactic" %% "scalactic" % V.scalatest
  val scalatest = "org.scalatest" %% "scalatest" % V.scalatest
  val scalacheck = "org.scalacheck" %% "scalacheck" % V.scalacheck
  val scopt = "com.github.scopt" %% "scopt" % V.scopt
  val slf4j = "org.slf4j" % "slf4j-nop" % V.slf4j
  val lsp4j = "org.eclipse.lsp4j" % "org.eclipse.lsp4j" % V.lsp4j
  val riddlc = "com.ossuminc" %% "riddlc" % V.ossumRiddl
  val riddlTestkit = "com.ossuminc" %% "riddl-testkit" % V.ossumRiddl
  val riddlHugo = "com.ossuminc" %% "riddl-hugo" % V.ossumRiddl % "test"

  val basic: Seq[ModuleID] = Seq(scalactic, scalatest, scalacheck, riddlTestkit, riddlc)

  val testing: Seq[ModuleID] = Seq(scalactic % "test", scalatest % "test", scalacheck % "test")
}
