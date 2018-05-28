mainClass          in assembly := Some("org.fs.checker.TorrentUpdatesCheckerEntry")
assemblyJarName    in assembly := name.value + "-" + version.value + "_" + currentDateTimeString.value + ".jar"
assemblyOutputPath in assembly := file("./_build") / (assemblyJarName in assembly).value

// Discard META-INF files to avoid assembly deduplication errors
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x                             => MergeStrategy.first
}

val currentDateTimeString = taskKey[String](s"Retrieves current date/time as yyyy-MM-dd_HH-mm-ss")

currentDateTimeString := {
  val sdf = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
  sdf.format(new java.util.Date())
}
