package etlflow.task

import com.google.cloud.bigquery.{JobInfo, Schema}
import etlflow.gcp._
import gcp4zio.bq.{BQApi, BQEnv, BQInputType}
import zio.{RIO, ZIO}

case class BQLoadTask(
    name: String,
    inputLocation: Either[String, Seq[(String, String)]],
    inputType: BQInputType,
    inputFileSystem: FSType = FSType.GCS,
    outputProject: Option[String] = None,
    outputDataset: String,
    outputTable: String,
    outputWriteDisposition: JobInfo.WriteDisposition = JobInfo.WriteDisposition.WRITE_TRUNCATE,
    outputCreateDisposition: JobInfo.CreateDisposition = JobInfo.CreateDisposition.CREATE_NEVER,
    schema: Option[Schema] = None
) extends EtlTask[BQEnv, Unit] {
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  var rowCount: Map[String, Long] = Map.empty

  override protected def process: RIO[BQEnv, Unit] = {
    logger.info("#" * 50)
    logger.info(s"Starting BQ Data Load Task: $name")

    val program: RIO[BQEnv, Unit] = inputFileSystem match {
      case FSType.LOCAL =>
        logger.info(s"FileSystem: $inputFileSystem")
        BQApi.loadTableFromLocalFile(inputLocation, inputType, outputDataset, outputTable)
      case FSType.GCS =>
        inputLocation match {
          case Left(value) =>
            logger.info(s"FileSystem: $inputFileSystem")
            BQApi
              .loadTable(
                value,
                inputType,
                outputProject,
                outputDataset,
                outputTable,
                outputWriteDisposition,
                outputCreateDisposition,
                schema
              )
              .map { x =>
                rowCount = x
              }
          case Right(value) =>
            logger.info(s"FileSystem: $inputFileSystem")
            BQApi
              .loadPartitionedTable(
                value,
                inputType,
                outputProject,
                outputDataset,
                outputTable,
                outputWriteDisposition,
                outputCreateDisposition,
                schema,
                10
              )
              .map { x =>
                rowCount = x
              }
        }
    }
    program *> ZIO.succeed(logger.info("#" * 50))
  }

  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  override def getTaskProperties: Map[String, String] = Map(
    "input_type" -> inputType.toString,
    "input_location" -> inputLocation.fold(
      source_path => source_path,
      source_paths_partitions => source_paths_partitions.mkString(",")
    ),
    "output_dataset"                  -> outputDataset,
    "output_table"                    -> outputTable,
    "output_table_write_disposition"  -> outputWriteDisposition.toString,
    "output_table_create_disposition" -> outputCreateDisposition.toString
    // ,"output_rows" -> row_count.foldLeft(0L)((a, b) => a + b._2).toString
    // "output_size" -> destinationTable.map(x => s"${x.getNumBytes / 1000000.0} MB").getOrElse("error in getting size")
    ,
    "output_rows" -> rowCount.map(x => x._1 + "<==>" + x._2.toString).mkString(",")
  )
}
