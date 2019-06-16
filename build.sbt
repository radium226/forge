import sbt.Keys.libraryDependencies

ThisBuild / organization := "com.github.radium226"
ThisBuild / scalaVersion := "2.12.7"
ThisBuild / version      := "0.1-SNAPSHOT"

ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:postfixOps",
  "-language:higherKinds",
  "-Ypartial-unification")

lazy val root = (project in file("."))
  .settings(
    name := "forge",
    libraryDependencies ++= Dependencies.cats,
    libraryDependencies ++= Dependencies.http4s,
    libraryDependencies ++= Dependencies.scopt,
    libraryDependencies ++= Dependencies.xtract,
    libraryDependencies ++= Dependencies.libpam4j,
    assembly / assemblyJarName := "forge.jar",
    assembly / assemblyMergeStrategy := {
      case "module-info.class" => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    publishTo := Some("Forge" at "http://localhost:1234/maven2"),
    credentials += Credentials(new File(".credentials")),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8")
  )

resolvers += Resolver.sonatypeRepo("releases")
