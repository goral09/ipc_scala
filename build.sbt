import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.7",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "ipc_poc",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "com.github.jnr" % "jnr-unixsocket" % "0.21",
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    )
  )
