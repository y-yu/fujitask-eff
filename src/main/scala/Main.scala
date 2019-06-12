import java.util.concurrent.TimeUnit

import com.google.inject.Guice
import config.di.DefaultModule
import fujitask.eff.Fujitask
import repository.UserRepository
import infra.db.Database
import infra.ec.ExecutionContextProvider
import kits.eff.Reader
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import repository.impl.jdbc._

object Main {
  val injector = Guice.createInjector(new DefaultModule)

  val logger: Logger = LoggerFactory.getLogger(Main.getClass)

  implicit val ec: ExecutionContext = injector.getInstance(classOf[ExecutionContextProvider]).get()

  def values[A](a: Future[A]): A = Await.result(a, Duration(10, TimeUnit.SECONDS))

  def setUp(): Unit = {
    Database.setUp()
    Database.createTable()
  }

  def tearDown(): Unit =
    Database.close()

  def userEff(): Unit = {
    val userRepository: UserRepository = injector.getInstance(classOf[UserRepository])

    val eff1 = for {
      user1 <- userRepository.read(1L)
      _     <- userRepository.create("test")
      user2 <- userRepository.read(1L)
    } yield {
      logger.info(s"user1 is $user1")
      logger.info(s"user2 is $user2")
    }

    val eff2 = for {
      user3 <- userRepository.read(1L)
    } yield {
      logger.info(s"user3 is $user3")
    }

    val eff3 = for {
      name <- Reader.ask[String]
      user <- userRepository.create(name)
      user4 <- userRepository.read(user.id)
    } yield {
      logger.info(s"user4 is $user4")
    }

    values {
      for {
        _ <- Fujitask.run(eff1)
        _ <- Fujitask.run(eff2)
        _ <- Fujitask.run(Reader.run("piyo")(eff3))
      } yield ()
    }
  }

  def main(args: Array[String]): Unit = {
    setUp()

    userEff()

    tearDown()
  }
}
