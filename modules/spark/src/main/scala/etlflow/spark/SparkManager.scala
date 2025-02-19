package etlflow.spark

import etlflow.log.ApplicationLogger
import etlflow.spark.Environment._
import org.apache.spark.sql.SparkSession

object SparkManager extends ApplicationLogger {
  private def showSparkProperties(spark: SparkSession): Unit = {
    logger.info(s"spark.scheduler.mode = ${spark.sparkContext.getSchedulingMode}")
    logger.info(s"spark.default.parallelism = ${spark.conf.getOption("spark.default.parallelism")}")
    logger.info(s"spark.sql.shuffle.partitions = ${spark.conf.getOption("spark.sql.shuffle.partitions")}")
    logger.info(s"spark.sql.sources.partitionOverwriteMode = ${spark.conf.getOption("spark.sql.sources.partitionOverwriteMode")}")
    logger.info(s"spark.sparkContext.uiWebUrl = ${spark.sparkContext.uiWebUrl}")
    logger.info(s"spark.sparkContext.applicationId = ${spark.sparkContext.applicationId}")
    logger.info(s"spark.sparkContext.sparkUser = ${spark.sparkContext.sparkUser}")
    logger.info(s"spark.eventLog.dir = ${spark.conf.getOption("spark.eventLog.dir")}")
    logger.info(s"spark.eventLog.enabled = ${spark.conf.getOption("spark.eventLog.enabled")}")
    spark.conf.getAll.filter(m1 => m1._1.contains("yarn")).foreach(kv => logger.info(kv._1 + " = " + kv._2))
  }

  def createSparkSession(
      env: Set[Environment] = Set(LOCAL),
      props: Map[String, String] = Map(
        "spark.scheduler.mode"                     -> "FAIR",
        "spark.sql.sources.partitionOverwriteMode" -> "dynamic",
        "spark.default.parallelism"                -> "10",
        "spark.sql.shuffle.partitions"             -> "10"
      ),
      hiveSupport: Boolean = true
  ): SparkSession =
    SparkSession.getActiveSession.fold {
      logger.info(s"###### Creating Spark Session with $env support ##########")
      @SuppressWarnings(Array("org.wartremover.warts.Var"))
      var sparkBuilder = SparkSession.builder()

      props.foreach { prop =>
        sparkBuilder = sparkBuilder.config(prop._1, prop._2)
      }

      env.foreach {
        case GCP(service_account_key_path, project_id) =>
          sparkBuilder = sparkBuilder
            .config("fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
            .config("fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
            .config("fs.gs.project.id", project_id)
            .config("fs.gs.auth.service.account.enable", "true")
            .config("google.cloud.auth.service.account.json.keyfile", service_account_key_path)
            .config("credentialsFile", service_account_key_path)
        case AWS(access_key, secret_key) =>
          sparkBuilder = sparkBuilder
            .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
            .config("spark.hadoop.fs.s3a.access.key", access_key)
            .config("spark.hadoop.fs.s3a.secret.key", secret_key)
        case LOCAL =>
          sparkBuilder = sparkBuilder
            .config("spark.ui.enabled", "false")
            .master("local[*]")
      }

      if (hiveSupport) sparkBuilder = sparkBuilder.enableHiveSupport()

      val spark = sparkBuilder.getOrCreate()
      showSparkProperties(spark)
      spark
    } { spark =>
      logger.info(s"###### Using Already Created Spark Session with $env support ##########")
      spark
    }
}
