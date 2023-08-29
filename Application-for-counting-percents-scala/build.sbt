name := "objectScala"

version := "0.1"

scalaVersion := "3.1.0"

idePackagePrefix := Some("org.apenko.app")

libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "8.0.27" % "provided",
  "com.typesafe" % "config" % "1.4.1" % "provided"
)

mainClass in assembly := Some("org.apenko.app.Application")
