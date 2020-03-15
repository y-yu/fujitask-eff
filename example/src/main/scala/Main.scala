import java.util.concurrent.TimeUnit
import com.google.inject.Guice
import config.di.DefaultModule
import domain.entity.UserId
import domain.exception.UserException
import domain.exception.UserException.NoSuchUserException
import domain.usecase.UpdateUserNameUseCase
import fujitask.eff.Fujitask
import infra.db.Database
import infra.ec.ExecutionContextProvider
import kits.eff.{Exc, Opt, Reader}
import org.slf4j.{Logger, LoggerFactory}
import repository.UserRepository
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
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
      _     <- userRepository.create("test")
      user2 <- userRepository.read(UserId(1L))
    } yield {
      logger.info(s"user2 is $user2")
    }

    val eff2 = for {
      user3 <- userRepository.read(UserId(1L))
    } yield {
      logger.info(s"user3 is $user3")
    }

    val eff3 = for {
      name <- Reader.ask[String]
      user <- userRepository.create(name)
      user4 <- userRepository.read(user.id)
    } yield {
      logger.info(s"user4 is $user4")
      user
    }
    val updateUserNameUseCase = injector.getInstance(classOf[UpdateUserNameUseCase])

    val eff4 = for {
      user <- userRepository.create("piyopiyo")
      _ = logger.info(s"$user name is piyopiyo.")
      updatedUser <- updateUserNameUseCase.updateUserName(
        user.id, "hogehoge!!!!!!!!!!!!!!!"
      )
      a <- Exc.raise[UserException](NoSuchUserException("aaa"))
    } yield {
      logger.info(s"user5 is $updatedUser")
    }

    values {
      for {
        _ <- Fujitask.run(Opt.run(eff1))
        _ <- Fujitask.run(Opt.run(eff2))
        _ <- Fujitask.run(Opt.run(Reader.run("piyo")(eff3)))
        _ = logger.info(s"There are ${Database.getAllUsers} users (1)")
        _ <- Fujitask.runWithEither(eff4)
      } yield ()
    }

    logger.info(s"There are ${Database.getAllUsers} users")
  }

  def main(args: Array[String]): Unit = {
    setUp()

    userEff()

    tearDown()
  }
}
