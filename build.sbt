name         := "torrent-updates-checker"
version      := "0.3.1"
scalaVersion := "2.11.8"

sourceManaged            <<= baseDirectory { _ / "src_managed" }
sourceManaged in Compile <<= baseDirectory { _ / "src_managed" / "main" / "scala" }
sourceManaged in Test    <<= baseDirectory { _ / "src_managed" / "test" / "scala" }

filterScalaLibrary := false

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, buildInfoBuildNumber),
    buildInfoPackage := "org.fs.checker"
  )

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies ++= Seq(
  // Logging
  "org.slf4s"                 %% "slf4s-api"        % "1.7.12",
  "ch.qos.logback"            %  "logback-classic"  % "1.1.2",
  // Other
  "com.github.frozenspider"   %% "fs-web-utils"     % "0.5.1",
  "org.apache.commons"        %  "commons-lang3"    % "3.4",
  "com.github.nscala-time"    %% "nscala-time"      % "2.16.0",
  "com.typesafe"              %  "config"           % "1.3.0",
  "org.scala-lang.modules"    %% "scala-swing"      % "2.0.0-M2",
  // Test
  "junit"                     %  "junit"            % "4.12"  % "test",
  "org.scalatest"             %% "scalatest"        % "2.2.4" % "test"
)