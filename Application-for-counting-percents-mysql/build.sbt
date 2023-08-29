ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "Application-for-counting-percents-mysql",
    idePackagePrefix := Some("org.apenko.app"),
    libraryDependencies ++= Seq(
      "mysql" % "mysql-connector-java" % "8.0.27" % "provided",
      "com.typesafe" % "config" % "1.4.1" % "provided"
    ),
    mainClass in assembly := Some("org.apenko.app.LoadDataToStaging"),
    mainClass in (Compile, run) := Some("org.apenko.app.LoadDataToStaging"),
    mainClass in (Compile, packageBin) := Some("org.apenko.app.LoadDataToStaging")
  )
