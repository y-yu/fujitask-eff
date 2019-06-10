import java.util.concurrent.TimeUnit

import com.google.inject.Guice
import config.di.DefaultModule
import domain.entity.User
import fujitask.eff.Fujitask
import repository.UserRepository
import infra.db.Database
import infra.ec.ExecutionContextProvider

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import repository.impl.jdbc._

object Main {
  val injector = Guice.createInjector(new DefaultModule)

  implicit val ec: ExecutionContext = injector.getInstance(classOf[ExecutionContextProvider]).get()

  def values[A](a: Future[A]): A = Await.result(a, Duration(10, TimeUnit.SECONDS))

  def setUp(): Unit = {
    Database.setUp()
    Database.createTable()
  }

  def tearDown(): Future[Unit] =
    Future(Database.close())

  def userEff(): Future[User] = {
    val userRepository: UserRepository = injector.getInstance(classOf[UserRepository])

    val eff = for {
      user1 <- userRepository.read(1L)
      _     <- userRepository.create("test")
      user2 <- userRepository.read(1L)
      _     <- userRepository.create("test2")
    } yield user2.get

    Fujitask.run(eff)
  }

  def main(args: Array[String]): Unit = {
    setUp()

    println(values(userEff()))

    tearDown()
  }
}
