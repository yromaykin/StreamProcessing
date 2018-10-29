name := "StreamProcessing"

version := "1.0"

scalaVersion := "2.11.12"

sparkVersion := "2.3.2"


//libraryDependencies += "org.apache.spark" % "spark-core_2.11" % "2.3.2"
libraryDependencies += "org.apache.spark" % "spark-sql-kafka-0-10_2.11" % "2.3.2"
libraryDependencies += "org.apache.spark" % "spark-sql_2.11" % "2.3.2" % "compile"


//libraryDependencies += Seq(
//  "org.apache.spark" % "spark-core_2.11"  % "2.3.2",
//  "org.apache.spark" % "spark-sql-kafka-0-10_2.11" % "2.3.2"
////  "org.apache.maven.plugins" % "maven-shade-plugin" % "2.4.3"
//)


//libraryDependencies += "org.apache.spark" % "spark-streaming-kafka-0-10_2.11" % "2.2.0"
//libraryDependencies += "org.apache.spark" %% "spark-streaming-kafka-0-8" % "2.1.0"

//libraryDependencies += "org.apache.spark" % "spark-streaming_2.11" % "2.2.0"


sparkComponents ++= Seq("sql", "streaming")

//Cassandra
//spDependencies += "datastax/spark-cassandra-connector:2.0.1-s_2.11"


assemblyJarName in assembly := s"${name.value.replace(' ','-')}-${version.value}.jar"