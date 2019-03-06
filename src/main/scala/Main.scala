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
    import fujitask.eff.FujitaskEffect._
    val userRepository: UserRepository = injector.getInstance(classOf[UserRepository])

    // `<-`でカスタムした`flatMap`がつかいたい……。
    val eff = for {
      user1 <- userRepository.read(1L)
      user2 <- userRepository.create("test")
      user3 <- userRepository.read(1L)
    } yield user3.get

    val f1 = new FujitaskEffImplicit(userRepository.read(1L))
      .flatMap(_ =>
        new FujitaskEffImplicit(userRepository.create("test")).flatMap(_ =>
          new FujitaskEffImplicit(userRepository.read(1L)).map(user3 =>
            user3.get
          )
        )
      )

    Fujitask.run(f1).flatMap(user =>
      Fujitask.run(
        userRepository.read(user.id).map(_.get)
      )
    )
  }

  def main(args: Array[String]): Unit = {
    setUp()

    println(values(userEff()))

    tearDown()
  }
}
