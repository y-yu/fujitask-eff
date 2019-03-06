name := "fujitask-eff"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.8"

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  // masterブランチをSNAPSHO版としてリリースした。
  "com.github.y-yu" %% "kits-eff" % "0.10.0-SNAPSHOT",
  "org.scalikejdbc" %% "scalikejdbc"       % "3.3.2",
  "org.scalikejdbc" %% "scalikejdbc-config" % "3.3.2",
  "com.h2database"  %  "h2"                % "1.4.197",
  "ch.qos.logback"  %  "logback-classic"   % "1.2.3",
  "com.google.inject" % "guice" % "4.2.2"
)