package etlflow.task

import gcp4zio.gcs._
import zio.{RIO, ZIO}
import java.nio.file.Paths

case class GCSPutTask(name: String, bucket: String, prefix: String, file: String) extends EtlTask[GCSEnv, Unit] {

  override protected def process: RIO[GCSEnv, Unit] = for {
    _    <- ZIO.succeed(logger.info("#" * 100))
    _    <- ZIO.succeed(logger.info(s"Input local path $file"))
    path <- ZIO.attempt(Paths.get(file))
    _    <- ZIO.succeed(logger.info(s"Output GCS path gs://$bucket/$prefix"))
    _ <- GCSApi
      .putObject(bucket, prefix, path, List.empty)
      .as(ZIO.succeed(logger.info(s"Successfully uploaded file $file in location gs://$bucket/$prefix")))
      .tapError(ex => ZIO.succeed(logger.error(ex.getMessage)))
    _ <- ZIO.succeed(logger.info("#" * 100))
  } yield ()

  override def getTaskProperties: Map[String, String] = Map("bucket" -> bucket, "prefix" -> prefix, "file" -> file)
}
