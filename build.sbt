import sbt.Keys._
import sbt._
import ReleaseTransformations._
import UpdateReadme.updateReadme

val scala213 = "2.13.1"

lazy val root = (project in file("."))
  .settings(
    name := "fujitask-eff",
    publishArtifact := false,
    publish := {},
    publishLocal := {}
  )
  .settings(publishSettings)
  .aggregate(fujitaskEff, example)

lazy val example = (project in file("example"))
  .settings(
    scalaVersion := scala213,
    publishArtifact := false,
    publish := {},
    publishLocal := {},
    libraryDependencies ++= Seq(
      "org.scalikejdbc" %% "scalikejdbc"       % "3.4.0",
      "org.scalikejdbc" %% "scalikejdbc-config" % "3.4.0",
      "com.h2database"  %  "h2"                % "2.2.220",
      "ch.qos.logback"  %  "logback-classic"   % "1.2.3",
      "org.scalatest" %% "scalatest" % "3.0.8" % "test",
      "com.google.inject" % "guice" % "4.2.2"
    )
  )
  .settings(publishSettings)
  .dependsOn(fujitaskEff)

lazy val fujitaskEff = (project in file("fujitask-eff"))
  .settings(
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-Xlint",
      "-language:implicitConversions", "-language:higherKinds", "-language:existentials",
      "-unchecked"
    ),
    scalaVersion := scala213,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    organization := "com.github.y-yu",
    name := "fujitask-eff",
    description := "Fujitask(one of the transaction monads) implementation by Extensible Effects",
    homepage := Some(url("https://github.com/y-yu")),
    licenses := Seq("MIT" -> url(s"https://github.com/y-yu/fujitask-eff/blob/master/LICENSE")),
    libraryDependencies ++= Seq(
      "org.halcat" %% "kits-eff" % "0.10.0"
    )
  )
  .settings(publishSettings)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishTo := Some(
    if (isSnapshot.value)
      Opts.resolver.sonatypeSnapshots
    else
      Opts.resolver.sonatypeStaging
  ),
  publishArtifact in Test := false,
  pomExtra :=
    <developers>
      <developer>
        <id>y-yu</id>
        <name>Hikaru Yoshimura</name>
        <url>https://github.com/y-yu</url>
      </developer>
    </developers>
      <scm>
        <url>git@github.com:y-yu/fujitask-eff.git</url>
        <connection>scm:git:git@github.com:y-yu/fujitask-eff.git</connection>
        <tag>{tagOrHash.value}</tag>
      </scm>,
  releaseTagName := tagName.value,
  releaseCrossBuild := true,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    updateReadme,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("^ publishSigned"),
    setNextVersion,
    commitNextVersion,
    updateReadme,
    releaseStepCommand("sonatypeReleaseAll"),
    pushChanges
  )
)

val tagName = Def.setting {
  s"v${if (releaseUseGlobalVersion.value) (version in ThisBuild).value else version.value}"
}

val tagOrHash = Def.setting {
  if (isSnapshot.value) sys.process.Process("git rev-parse HEAD").lineStream_!.head
  else tagName.value
}
