package etlflow.spark

import etlflow.utils.ApplicationLogger
import org.apache.spark.sql.{Dataset, Row, SaveMode, SparkSession}
import zio.{Managed, Task, TaskLayer, UIO}
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.TypeTag

case class SparkImpl(spark: SparkSession) extends SparkApi.Service[Task] {

  override def getSparkSession: Task[SparkSession] = Task(spark)

  override def ReadDSProps[T <: Product: TypeTag](
      location: Seq[String],
      input_type: IOType,
      where_clause: String
  ): Task[Map[String, String]] =
    Task(ReadApi.DSProps[T](location, input_type))

  override def ReadDS[T <: Product: TypeTag](location: Seq[String], input_type: IOType, where_clause: String): Task[Dataset[T]] =
    Task(ReadApi.DS[T](location, input_type, where_clause)(spark))

  override def ReadStreamingDS[T <: Product: TypeTag](
      location: String,
      input_type: IOType,
      where_clause: String
  ): Task[Dataset[T]] =
    Task(ReadApi.StreamingDS[T](location, input_type, where_clause)(spark))

  override def ReadDF(
      location: Seq[String],
      input_type: IOType,
      where_clause: String,
      select_clause: Seq[String]
  ): Task[Dataset[Row]] =
    Task(ReadApi.DF(location, input_type, where_clause, select_clause)(spark))

  override def WriteDSProps[T <: Product: universe.TypeTag](
      output_type: IOType,
      output_location: String,
      save_mode: SaveMode,
      partition_by: Seq[String],
      output_filename: Option[String],
      compression: String,
      repartition: Boolean,
      repartition_no: Int
  ): Task[Map[String, String]] = Task(
    WriteApi.DSProps[T](
      output_type,
      output_location,
      save_mode,
      partition_by,
      output_filename,
      compression,
      repartition,
      repartition_no
    )
  )

  override def WriteDS[T <: Product: universe.TypeTag](
      input: Dataset[T],
      output_type: IOType,
      output_location: String,
      save_mode: SaveMode,
      partition_by: Seq[String],
      output_filename: Option[String],
      compression: String,
      repartition: Boolean,
      repartition_no: Int
  ): Task[Unit] = Task(
    WriteApi.DS[T](
      input,
      output_type,
      output_location,
      save_mode,
      partition_by,
      output_filename,
      compression,
      repartition,
      repartition_no
    )(spark)
  )
}

object SparkImpl extends ApplicationLogger {
  def live(spark: SparkSession): TaskLayer[SparkEnv] = Managed
    .make(Task(spark))(a =>
      UIO {
        logger.info("Stopping spark session")
        a.close()
      }
    )
    .map(SparkImpl(_))
    .toLayer
}
