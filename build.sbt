enablePlugins(JavaAppPackaging)

organization  := "org.phenoscape"

name          := "annotations-demo"

version       := "0.0.1"

publishArtifact in Test := false

licenses := Seq("BSD-3-Clause" -> url("https://opensource.org/licenses/BSD-3-Clause"))

scalaVersion  := "2.11.11"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

scalacOptions in Test ++= Seq("-Yrangepos")

mainClass in Compile := Some("org.phenoscape.annotations.demo.Main")

javaOptions += "-Xmx10G"

fork in Test := true

libraryDependencies ++= {
  Seq(
    "org.scalaz"             %% "scalaz-core"         % "7.2.1",
    "org.apache.jena"        %  "apache-jena-libs"    % "3.2.0" pomOnly(),
    "net.sourceforge.owlapi" %  "owlapi-distribution" % "4.2.8",
    "org.phenoscape"         %% "scowl"               % "1.3",
    "org.semanticweb.elk"    %  "elk-owlapi"          % "0.4.3"
  )
}
