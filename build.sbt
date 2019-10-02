version := "1.0-rc2" 

scalaVersion := "2.11.7"

organization := "org.armadeus"

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)

libraryDependencies += "edu.berkeley.cs" %% "chisel3" % "3.1.8"
libraryDependencies += "edu.berkeley.cs" %% "chisel-iotesters" % "1.2.10"

scalacOptions ++= Seq("-deprecation", "-feature")

scalacOptions ++= Seq("-language:reflectiveCalls")

//publish local https://github.com/Martoni/WbPlumbing.git
libraryDependencies ++= Seq("org.armadeus" %% "wbplumbing" % "0.1")
