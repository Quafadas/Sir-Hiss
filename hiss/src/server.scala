package hiss

import scala.concurrent.duration.*

import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.*
import org.http4s.implicits.*
import org.http4s.twirl.*

import com.comcast.ip4s.*

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.implicits.*
import smithy4s.http.UrlForm

object FormRoutes {

  // Decoder is derived from the smithy-generated CreatePersonInput schema —
  // the Smithy model is the single source of truth for field names and types.
  private val formDecoder =
    UrlForm
      .Decoder(
        ignoreUrlFormFlattened = false,
        capitalizeStructAndUnionMemberNames = false
      )
      .fromSchema(CreatePersonInput.schema)

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / "ui" =>
      Ok(html.people(PersonServiceImpl.people))

    case GET -> Root / "ui" / "people" / "rows" =>
      Ok(html.peopleRows(PersonServiceImpl.people))

    case GET -> Root / "ui" / "people" / IntVar(id) / "edit" =>
      PersonServiceImpl.people.find(_.id == id) match {
        case Some(p) => Ok(html.personEditRow(p))
        case None    => NotFound()
      }

    case req @ POST -> Root / "ui" / "people" =>
      for {
        body <- req.bodyText.compile.string
        resp <- UrlForm.parse(body).flatMap(formDecoder.decode) match {
          case Right(input) =>
            PersonServiceImpl.createPerson(input.name, input.town)
            Ok(html.peopleRows(PersonServiceImpl.people))
          case Left(err) =>
            BadRequest(err.toString)
        }
      } yield resp

    case req @ PUT -> Root / "ui" / "people" / IntVar(id) =>
      for {
        body <- req.bodyText.compile.string
        resp <- UrlForm.parse(body).flatMap(formDecoder.decode) match {
          case Right(input) =>
            PersonServiceImpl.updatePerson(id, input.name, input.town)
            Ok(html.peopleRows(PersonServiceImpl.people))
          case Left(err) =>
            BadRequest(err.toString)
        }
      } yield resp

    case DELETE -> Root / "ui" / "people" / IntVar(id) =>
      PersonServiceImpl.deletePerson(id)
      Ok(html.peopleRows(PersonServiceImpl.people))
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

  val docs: HttpRoutes[IO] = smithy4s.http4s.swagger.docs[IO](PersonService)

  // val all: Resource[IO, HttpRoutes[IO]] = example.map(_ <+> docs)
}

object Main extends IOApp.Simple {

  val routes = Routes.routed(
    Harness.routesIO(PersonServiceImpl),
    Resource.pure[IO, HttpRoutes[IO]](Routes.docs),
    Resource.pure[IO, HttpRoutes[IO]](FormRoutes.routes)
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
      IO.println("Server started at http://localhost:9000") *>
        IO.never
    )

}
