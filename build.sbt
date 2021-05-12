name := "distributed-systems-playground"
organization in ThisBuild := "com.github.rasheeddavid"
version in ThisBuild := "0.1"

/** [project-dependencies] */

lazy val dependencies =
	new {
		val rc_akkaVersion = "2.5.21"
		val protobufVersion = "3.6.1"
		val sparkVersion = "3.0.0"

		// remote-cluster projects
		val rc_akka_actor = "com.typesafe.akka" %% "akka-actor" % rc_akkaVersion
		val rc_akka_remote = "com.typesafe.akka" %% "akka-remote" % rc_akkaVersion
		val rc_akka_cluster = "com.typesafe.akka" %% "akka-cluster" % rc_akkaVersion
		val rc_akka_cluster_sharding = "com.typesafe.akka" %% "akka-cluster-sharding" % rc_akkaVersion
		val rc_akka_cluster_tools = "com.typesafe.akka" %% "akka-cluster-tools" % rc_akkaVersion

		// spark
		val spark_core = "org.apache.spark" %% "spark-core" % sparkVersion
		val spark_sql = "org.apache.spark" %% "spark-sql" % sparkVersion
		val spark_streaming = "org.apache.spark" %% "spark-streaming" % sparkVersion
	}

lazy val rc_scalaVersion = "2.12.8"
lazy val spark_scalaVersion = "2.12.10"

lazy val rc = Seq(
	dependencies.rc_akka_actor,
	dependencies.rc_akka_cluster_tools,
	dependencies.rc_akka_cluster_sharding,
	dependencies.rc_akka_cluster,
	dependencies.rc_akka_remote
)

/** [independent-project] */
lazy val `akka-chat-room-cluster` = project.settings(
	scalaVersion := rc_scalaVersion,
	libraryDependencies ++= rc
)

lazy val `akka-word-counting-cluster` = project.settings(
	scalaVersion := rc_scalaVersion,
	libraryDependencies ++= rc
)

lazy val `akka-word-counting-remote` = project.settings(
	scalaVersion := rc_scalaVersion,
	libraryDependencies ++= rc
)

lazy val `spark-stateful-computations` = project.settings(
	scalaVersion := spark_scalaVersion,
	resolvers ++= Seq(
		"bintray-spark-packages" at "https://dl.bintray.com/spark-packages/maven",
		"Typesafe Simple Repository" at "https://repo.typesafe.com/typesafe/simple/maven-releases",
		"MavenRepository" at "https://mvnrepository.com"
	),
	libraryDependencies ++= Seq(
		dependencies.spark_core,
		dependencies.spark_sql,
		dependencies.spark_streaming
	)
)


/** [project-settings] */

