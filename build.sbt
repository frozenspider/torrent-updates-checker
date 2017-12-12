name         := "torrent-updates-checker"
version      := "1.0.1"
scalaVersion := "2.12.3"

sourceManaged            := baseDirectory.value / "src_managed"
sourceManaged in Compile := baseDirectory.value / "src_managed" / "main" / "scala"
sourceManaged in Test    := baseDirectory.value / "src_managed" / "test" / "scala"

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, buildInfoBuildNumber),
    buildInfoPackage := "org.fs.checker"
  )

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies ++= Seq(
  // Logging
  "org.slf4s"               %% "slf4s-api"       % "1.7.25",
  "org.slf4j"               %  "jul-to-slf4j"    % "1.7.25",
  "ch.qos.logback"          %  "logback-classic" % "1.1.2",
  // UI
  "org.scala-lang.modules"  %% "scala-swing"     % "2.0.0",
  // Web
  "com.github.finagle"      %% "finch-core"      % "0.16.0-M5",
  "com.github.finagle"      %% "finch-circe"     % "0.16.0-M5",
  "io.circe"                %% "circe-generic"   % "0.9.0-M2",
  // Other
  "com.github.frozenspider" %% "fs-web-utils"    % "0.5.3",
  "org.apache.commons"      %  "commons-lang3"   % "3.4",
  "com.github.nscala-time"  %% "nscala-time"     % "2.16.0",
  "com.typesafe"            %  "config"          % "1.3.0",
  // Test
  "junit"                   %  "junit"           % "4.12"  % "test",
  "org.scalactic"           %% "scalactic"       % "3.0.4" % "test",
  "org.scalatest"           %% "scalatest"       % "3.0.4" % "test"
)
