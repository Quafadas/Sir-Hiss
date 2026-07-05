import smithy4s.example.hello._
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.implicits._
import org.http4s.implicits._
import org.http4s.ember.server._
import org.http4s._
import com.comcast.ip4s._
import smithy4s.http4s.SimpleRestJsonBuilder
import cats.Id
import smithy4s.kinds.PolyFunction
import org.http4s.dsl.io._
import org.http4s.twirl._

object HelloWorldImpl extends HelloWorldService[Id] {

  def getHello(name: String, town: Option[String]): Greeting = {
    town match {
      case None    => Greeting(s"Hello $name!")
      case Some(t) => Greeting(s"Hello $name from $t!")
    }
  }
  def hello(name: String, town: Option[String]): Greeting = {
    town match {
      case None    => Greeting(s"Hello $name!")
      case Some(t) => Greeting(s"Hello $name from $t!")
    }
  }
}

object UI {
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] { case GET -> Root / "ui" =>
    Ok(html.hello("boo"))
  }
}

object Routes {
  // private val example: Resource[IO, HttpRoutes[IO]] =
  //   SimpleRestJsonBuilder.routes(HelloWorldImpl.transform(toIO)).resource

  // val arg = HelloWorldImpl.transform(toIO)

  def routed(
      routes: Resource[IO, HttpRoutes[IO]]*
  ): Resource[IO, HttpRoutes[IO]] =
    routes.toList.sequence.map(_.reduceLeft(_ <+> _))

  val docs: HttpRoutes[IO] = smithy4s.http4s.swagger.docs[IO](HelloWorldService)

  // val all: Resource[IO, HttpRoutes[IO]] = example.map(_ <+> docs)
}

object Main extends IOApp.Simple {

  private val toIO: PolyFunction[cats.Id, IO] = new PolyFunction[cats.Id, IO] {
    def apply[A](result: cats.Id[A]): IO[A] = IO.pure(result)
  }

  val routes = Routes.routed(
    SimpleRestJsonBuilder.routes(HelloWorldImpl.transform(toIO)).resource,
    Resource.pure[IO, HttpRoutes[IO]](Routes.docs),
    Resource.pure[IO, HttpRoutes[IO]](UI.routes)
  )

  val run = routes
    .flatMap { routes =>
      EmberServerBuilder
        .default[IO]
        .withPort(port"9000")
        .withHost(host"localhost")
        .withHttpApp(routes.orNotFound)
        .build
    }
    .use(_ => IO.never)

}