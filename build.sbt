import sbt.Keys.libraryDependencies

logLevel := Level.Debug

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
    libraryDependencies ++= Dependencies.guava, 
    assembly / assemblyJarName := "forge.jar",
    assembly / assemblyMergeStrategy := {
      case "module-info.class" => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    mainClass in assembly := Some("com.github.radium226.forge.Main"),
    publishMavenStyle := true,
    publishArtifact in (Compile, packageDoc) := false,
    publishTo := Some("forge" at "http://forge.rouages.xyz:1234/maven2"),
    credentials += Credentials(file(".credentials")), 
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8"),
    updateOptions := updateOptions.value.withGigahorse(false), // https://github.com/sbt/sbt/issues/3570
    logBuffered in Test := false,
    libraryDependencies ++= Dependencies.scalatic,
    libraryDependencies ++= Dependencies.scalaTest map(_ % Test),
    libraryDependencies ++= Dependencies.circe,
    libraryDependencies ++= Dependencies.config
  )
    .dependsOn(`system`)

lazy val `system` = RootProject(uri("../system-scala"))

resolvers += Resolver.sonatypeRepo("releases")
