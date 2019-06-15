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

lazy val `commons` = (project in file("commons"))
  .settings(
    name := "commons"
  )

lazy val `maven` = (project in file("maven"))
  .settings(
    name := "maven",
    libraryDependencies ++= Dependencies.cats,
    libraryDependencies ++= Dependencies.http4s,
    libraryDependencies ++= Dependencies.scopt,
    libraryDependencies ++= Dependencies.xtract,
    libraryDependencies ++= Dependencies.libpam4j,
    assembly / assemblyJarName := "maven.jar",
    assembly / assemblyMergeStrategy := {
      case "module-info.class" => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8")
  )
  .dependsOn(`commons`)

lazy val `pacman` = (project in file("pacman"))
  .settings(
    name := "pacman"
  )
  .dependsOn(`commons`)

lazy val root = (project in file("."))
  .aggregate(
    `commons`,
    `maven`,
    `pacman`
  )

resolvers += Resolver.sonatypeRepo("releases")
