enablePlugins(JavaAppPackaging)

organization  := "org.phenoscape"

name          := "annotations-demo"

version       := "0.1"

publishArtifact in Test := false

licenses := Seq("BSD-3-Clause" -> url("https://opensource.org/licenses/BSD-3-Clause"))

scalaVersion  := "2.12.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

scalacOptions in Test ++= Seq("-Yrangepos")

mainClass in Compile := Some("org.phenoscape.annotations.demo.Main")

javaOptions += "-Xmx10G"

fork in Test := true

libraryDependencies ++= {
  Seq(
    "org.scalaz"             %% "scalaz-core"         % "7.2.24",
    "org.apache.jena"        %  "apache-jena-libs"    % "3.8.0" pomOnly(),
    "net.sourceforge.owlapi" %  "owlapi-distribution" % "4.5.2",
    "org.phenoscape"         %% "scowl"               % "1.3.1",
    "org.semanticweb.elk"    %  "elk-owlapi"          % "0.4.3"
  )
}
