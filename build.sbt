ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "http4s-js-lambda-zio-handler",
    libraryDependencies ++= Seq(
      "net.exoego" %%% "aws-lambda-scalajs-facade" % "0.12.1",
      "dev.zio" %%% "zio" % "2.0.0",
      "dev.zio" %%% "zio-managed" % "2.0.0",
      "dev.zio" %%% "zio-interop-cats" % "3.3.0",
      "io.github.cquiroz" %%% "scala-java-time" % "2.2.0",
      "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.2.0",
      "org.http4s" %%% "http4s-dsl" % "0.23.16"
    )
  ).enablePlugins(ScalaJSPlugin)
