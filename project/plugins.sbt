addSbtPlugin("com.ossuminc" % "sbt-ossuminc" % "0.9.5")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.11")

// This enables sbt-bloop to create bloop config files for Metals editors
// Uncomment locally if you use metals, otherwise don't slow down other
// people's builds by leaving it commented in the repo.
// addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.4.6")

ThisBuild / libraryDependencySchemes +=
  "org.scala-lang.modules" %% "scala-xml" % "always"
