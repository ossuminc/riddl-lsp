import com.ossuminc.sbt.helpers.Publishing
import com.ossuminc.sbt.helpers.RootProjectInfo.Keys.{
  gitHubOrganization,
  gitHubRepository
}
import org.scoverage.coveralls.Imports.CoverallsKeys._

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
  .aggregate(server, plugin)

lazy val Server = config("server")
lazy val server: Project = Module("server", "riddl-lsp-server")
  .enablePlugins(OssumIncPlugin)
  .configure(
    With.typical,
    With.build_info,
    With.coverage(90) /*, With.native()*/
  )
  .settings(
    buildInfoPackage := "com.ossuminc.riddl.lsp.server",
    buildInfoObject := "RiddlLSPServerBuildInfo",
    description := "The server for the LSP for RIDDL",
    libraryDependencies ++= Dep.testing ++ Dep.basic
  )

lazy val Plugin = config("plugin")
lazy val plugin: Project = Module("plugin", "riddl-lsp-plugin")
  .enablePlugins(OssumIncPlugin)
  .configure(
    With.typical,
    With.build_info,
    With.coverage(90) /*, With.native()*/
  )
  .settings(
    buildInfoPackage := "com.ossuminc.riddl.lsp.plugin",
    buildInfoObject := "RiddlLSPPluginBuildInfo",
    description := "The plugin for supporting RIDDL in IntelliJ",
    libraryDependencies ++= Dep.testing ++ Dep.basic
  )
