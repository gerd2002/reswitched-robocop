import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "tech.gerd",
      scalaVersion := "2.12.4",
      version      := "0.1.0"
    )),
    name := "robocop",
    resolvers += Resolver.jcenterRepo,
    libraryDependencies ++= Seq(
      "net.dv8tion" % "JDA" % "3.6.0_354",
      "org.xerial" % "sqlite-jdbc" % "3.8.7",
      "io.getquill" %% "quill-jdbc" % "2.4.1",
      "org.slf4j" % "slf4j-nop" % "1.6.4",
      "org.scala-lang" % "scala-compiler" % "2.12.4"
    )
  )
