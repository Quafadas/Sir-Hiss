package hiss

import scala.concurrent.duration.*

import org.http4s.*
import org.http4s.ember.server.*
import org.http4s.implicits.*

import com.comcast.ip4s.*

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.implicits.*

object Routes {
  // private val example: Resource[IO, HttpRoutes[IO]] =
  //   SimpleRestJsonBuilder.routes(HelloWorldImpl.transform(toIO)).resource

  // val arg = HelloWorldImpl.transform(toIO)

  def routed(
      routes: Resource[IO, HttpRoutes[IO]]*
  ): Resource[IO, HttpRoutes[IO]] =
    routes.toList.sequence.map(_.reduceLeft(_ <+> _))

  val docs: HttpRoutes[IO] = smithy4s.http4s.swagger.docs[IO](PersonService)

  // val all: Resource[IO, HttpRoutes[IO]] = example.map(_ <+> docs)
}

object Main extends IOApp.Simple {

  val routes = Routes.routed(
    Harness.routesIO(PersonServiceImpl),
    Resource.pure[IO, HttpRoutes[IO]](Routes.docs),
    Resource.pure[IO, HttpRoutes[IO]](FormRoutes.routes(PersonServiceImpl))
  )

  val run = routes
    .flatMap { routes =>
      EmberServerBuilder
        .default[IO]
        .withPort(port"9000")
        .withHost(host"localhost")
        .withHttpApp(routes.orNotFound)
        .withShutdownTimeout(10.millis)
        .build
    }
    .use(_ =>
      IO.println("Server started at http://localhost:9000. Try http://localhost:9000/ui and http://localhost:9000/docs") *>
        IO.never
    )

}
