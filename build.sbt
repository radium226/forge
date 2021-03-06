import sbt.Keys.libraryDependencies

logLevel := Level.Debug

ThisBuild / organization := "com.github.radium226"
ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version      := "0.1-SNAPSHOT"

ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:postfixOps",
  "-language:higherKinds")/*,
  "-Xlog-implicits")*/

lazy val root = (project in file("modules/forge"))
  .settings(
    name := "forge",
    libraryDependencies ++= Dependencies.cats,
    libraryDependencies ++= Dependencies.http4s,
    libraryDependencies ++= Dependencies.xtract,
    libraryDependencies ++= Dependencies.libpam4j,
    libraryDependencies ++= Dependencies.guava,
    libraryDependencies +=  "commons-io" % "commons-io" % "2.6",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",
    libraryDependencies += "uk.co.caprica" % "juds" % "0.94.1",
    assembly / assemblyJarName := "forge.jar",
    assembly / assemblyMergeStrategy := {
      case x if x.endsWith("module-info.class") =>
        MergeStrategy.discard

      case x if x.contains("junixsocket") =>
        MergeStrategy.singleOrError

      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    mainClass in assembly := Some("com.github.radium226.forge.Main"),
    publishMavenStyle := true,
    publishArtifact in (Compile, packageDoc) := false,
    publishTo := Some("forge" at "http://forge.rouages.xyz:1234/maven2"),
    credentials += Credentials(file(".credentials")),
    addCompilerPlugin("org.typelevel" % "kind-projector_2.13.1" % "0.11.0"),
    updateOptions := updateOptions.value.withGigahorse(false), // https://github.com/sbt/sbt/issues/3570
    logBuffered in Test := false,
    libraryDependencies ++= Dependencies.scalatic,
    libraryDependencies ++= Dependencies.scalaTest map(_ % Test),
    libraryDependencies ++= Dependencies.circe,
    libraryDependencies += "com.beachape" %% "enumeratum" % "1.5.13",
    libraryDependencies +=  "commons-io" % "commons-io" % "2.6",
    scalaVersion := "2.13.1"
  )
    .dependsOn(`system`, `http4s-fastcgi`, `config`)

lazy val `system` = RootProject(uri("../system-scala"))

lazy val `http4s-fastcgi` = RootProject(uri("../http4s-fastcgi"))

lazy val `config` = RootProject(uri("../config-scala"))

resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.mavenLocal
