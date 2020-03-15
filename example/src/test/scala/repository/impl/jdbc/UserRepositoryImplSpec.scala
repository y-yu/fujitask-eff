package repository.impl.jdbc

import java.util.concurrent.TimeUnit
import com.google.inject.Guice
import config.di.DefaultModule
import domain.entity.{User, UserId}
import fujitask.eff.Fujitask
import infra.db.Database
import kits.eff.{Exc, Opt, Reader}
import org.scalatest.{DiagrammedAssertions, FlatSpec}
import repository.{ReadTransaction, ReadWriteTransaction, UserRepository}
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Try

class UserRepositoryImplSpec
  extends FlatSpec
    with DiagrammedAssertions
    with BeforeAndAfter
    with BeforeAndAfterAll {

  def values[A](a: Future[A]): A = Await.result(a, Duration(10, TimeUnit.SECONDS))

  trait SetUp {
    val injector = Guice.createInjector(new DefaultModule)

    val sut = injector.getInstance(classOf[UserRepository])
  }

  override protected def beforeAll(): Unit = {
    Database.setUp()
    Database.createTable()
  }

  override protected def afterAll(): Unit = {
    Database.close()
  }

  after {
    Database.deleteAllData()
  }

  "UserRepository" should "create/read/update/delete a user in ReadWrite transaction successfully" in new SetUp {
    val actual = for {
      user1 <- sut.create("test")
      user2Opt <- Opt.run(sut.read(user1.id))
      _ <- sut.update(user1.copy(name = "changed"))
      user3Opt <- Opt.run(sut.read(user1.id))
      _ <- sut.delete(user1.id)
      user4Opt <- Opt.run(sut.read(user1.id))
      i <- Fujitask.ask[ReadWriteTransaction, ScalikeJDBCWriteTransaction]
    } yield {
      assert(user1 == User(UserId(1L), "test"))
      assert(user2Opt.contains(User(UserId(1L), "test")))
      assert(user3Opt.contains(User(UserId(1L), "changed")))
      assert(user4Opt.isEmpty)
      assert(i.isInstanceOf[ScalikeJDBCWriteTransaction])
    }

    values(Fujitask.run(actual))
  }

  it should "read a user in Read only transaction successfully" in new SetUp {
    val actual = for {
      _ <- Opt.run(sut.read(UserId(1L)))
      i <- Fujitask.ask[ReadTransaction, ScalikeJDBCReadTransaction]
    } yield {
      assert(i.isInstanceOf[ScalikeJDBCReadTransaction])
    }

    values(Fujitask.run(actual))
  }

  it should "be able to use if the effect stack has an other effect(Reader)" in new SetUp {
    val name = "test"

    val actual = for {
      n <- Reader.ask[String]
      user1 <- sut.create(n)
    } yield {
      assert(user1.name == name)
    }

    values(Fujitask.run(Reader.run("test")(actual)))
  }

  it should "rollback if illegal queries was executed" in new SetUp {
    assert(Database.isEmpty)

    val actual = for {
      _ <- sut.create("test")
      // `name` has a not null constraint so that this is illegal.
      _ <- sut.create(null)
    } yield ()

    assert(Try(values(Fujitask.run(actual))).isFailure)
    assert(Database.isEmpty)
  }

  it should "rollback if Either.Left and using `runWithEither`" in new SetUp {
    assert(Database.isEmpty)

    val eff = for {
      _ <- sut.create("test")
      _ <- Exc.raise(new RuntimeException())
    } yield ()

    val actual = Try(values(Fujitask.runWithEither(eff)))

    assert(actual.isSuccess)
    assert(actual.get.isLeft)
    assert(Database.isEmpty)
  }

  it should "NOT rollback if Either.Left but using `run`" in new SetUp {
    assert(Database.isEmpty)

    val eff = for {
      _ <- sut.create("test")
      _ <- Exc.raise(new RuntimeException())
    } yield ()

    val actual = Try(values(Fujitask.run(Exc.run(eff))))

    assert(actual.isSuccess)
    assert(actual.get.isLeft)
    assert(!Database.isEmpty)
  }
}
