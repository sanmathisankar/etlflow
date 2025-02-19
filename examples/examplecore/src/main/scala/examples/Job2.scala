package examples

import etlflow.task.GenericTask
import etlflow.log.ApplicationLogger
import zio.Task

@SuppressWarnings(Array("org.wartremover.warts.Throw"))
object Job2 extends zio.ZIOAppDefault with ApplicationLogger {

  override val bootstrap = zioSlf4jLogger

  def processData1(): String = {
    logger.info(s"Hello World")
    "Hello World"
  }

  private val task1 = GenericTask(
    name = "Task_1",
    function = processData1()
  )

  def processData2(): Unit = logger.info("Hello World")

  private val task2 = GenericTask(
    name = "Task_2",
    function = processData2()
  )

  def processData3(): Unit = {
    logger.info(s"Hello World")
    throw new RuntimeException("Error123")
  }

  private val task3 = GenericTask(
    name = "Task_3",
    function = processData3()
  )

  private val job = for {
    _ <- task1.execute
    _ <- task2.execute
    _ <- task3.execute
  } yield ()

  override def run: Task[Unit] = job.provideLayer(etlflow.audit.noLog)
}
