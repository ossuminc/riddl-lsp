import com.ossuminc.sbt.helpers.Publishing
import com.ossuminc.sbt.helpers.RootProjectInfo.Keys.{
  gitHubOrganization,
  gitHubRepository
}
import org.scoverage.coveralls.Imports.CoverallsKeys.*

Global / onChangedBuildSource := ReloadOnSourceChanges
(Global / excludeLintKeys) ++= Set(mainClass)
Global / scalaVersion := "3.4.0"

enablePlugins(OssumIncPlugin)

lazy val riddl: Project = Root("", "riddl", startYr = 2024)
  .configure(Publishing.configure, With.git, With.dynver)
  .settings(
    gitHubRepository := "riddl-lsp",
    gitHubOrganization := "ossuminc",
    publish / skip := true
  )
  .aggregate(server)

lazy val Server = config("server")
lazy val server: Project = Module("server", "riddl-lsp-server")
  .enablePlugins(OssumIncPlugin)
  .configure(
    With.typical,
    With.build_info,
    With.coverage(70) /*, With.native()*/
  )
  .settings(
    buildInfoPackage := "com.ossuminc.riddl.lsp.server",
    buildInfoObject := "RiddlBuildInfo",
    description := "The server for the LSP for RIDDL",
    libraryDependencies ++= Dep.testing
  )
