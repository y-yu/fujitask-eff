package config.di

import com.google.inject.AbstractModule
import infra.ec.ExecutionContextProvider
import repository.UserRepository
import repository.impl.jdbc.UserRepositoryImpl
import scala.concurrent.ExecutionContext

class DefaultModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[ExecutionContext]).toProvider(classOf[ExecutionContextProvider])

    bind(classOf[UserRepository]).to(classOf[UserRepositoryImpl])
  }
}
