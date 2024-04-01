Global / onChangedBuildSource := ReloadOnSourceChanges
(Global / excludeLintKeys) ++= Set(mainClass)
Global / scalaVersion := "3.4.0"

enablePlugins(OssumIncPlugin)

lazy val riddl: Project = Root("riddl-lsp", "ossuminc", "com.ossuminc.riddl.lsp", startYr = 2024)
  .configure(With.noPublishing, With.git, With.dynver)
  .aggregate(server, plugin)

lazy val Server = config("server")
lazy val server: Project = Module("server", "riddl-lsp-server")
  .configure(
    With.noPublishing,
    With.typical,
    With.build_info,
    With.coverage(90)
  )
  .settings(
    buildInfoPackage := "com.orissuminc.riddl.lsp.server",
    buildInfoObject := "RiddlLSPServerBuildInfo",
    description := "The server for the LSP for RIDDL",
    libraryDependencies ++= Dep.testing ++ Dep.basic ++ Seq(
      Dep.lang3,
      Dep.pureconfig,
      Dep.scopt,
      Dep.slf4j,
      Dep.lsp4j
    )
  )

lazy val Plugin = config("plugin")
lazy val plugin: Project = Module("plugin", "riddl-lsp-plugin")
  .configure(
    With.noPublishing,
    With.typical,
    With.build_info,
    With.coverage(90)
  )
  .settings(
    buildInfoPackage := "com.ossuminc.riddl.lsp.plugin",
    buildInfoObject := "RiddlLSPPluginBuildInfo",
    description := "The plugin for supporting RIDDL in IntelliJ",
    libraryDependencies ++= Dep.testing ++ Dep.basic
  )
