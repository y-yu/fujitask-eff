package infra.ec

import com.google.inject.{Provider, Singleton}

import scala.concurrent.ExecutionContext

@Singleton
class ExecutionContextProvider extends Provider[ExecutionContext] {
  private val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def get(): ExecutionContext = ec
}