package hiss

import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.twirl.*

import cats.Id
import cats.effect.IO
import cats.implicits.*
import smithy4s.http.UrlForm

object PersonServiceImpl extends PersonServiceT

trait PersonServiceT extends PersonService[Id] {

  var people: List[Person] = List(
    Person(1, "John", Some("Doe")),
    Person(2, "Jane", Some("Doe")),
    Person(3, "Alice", Some("Smith"))
  )

  private def nextId: Int = people.map(_.id).maxOption.getOrElse(0) + 1

  override def createPerson(name: String, town: Option[String]): Person = {
    val p = Person(nextId, name, town)
    people = people :+ p
    p
  }

  override def getPerson(id: Int): Person =
    people
      .find(_.id == id)      
      .getOrElse(throw new Exception(s"Person $id not found"))

  override def updatePerson(
      id: Int,
      name: String,
      town: Option[String]
  ): Person = {
    val updated = Person(id, name, town)
    people = people.map(p => if p.id == id then updated else p)
    updated
  }

  override def patchPerson(id: Int, town: String): Person = {
    val updated = getPerson(id).copy(town = Some(town))
    people = people.map(p => if p.id == id then updated else p)
    updated
  }

  override def deletePerson(id: Int): Unit =
    people = people.filterNot(_.id == id)

  override def allPeople(): AllPeopleOutput = AllPeopleOutput(people)
}

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

  def routes(svc: PersonServiceT): HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / "ui" =>
      Ok(html.people(svc.people))

    case GET -> Root / "ui" / "people" / "rows" =>
      Ok(html.peopleRows(svc.people))

    case GET -> Root / "ui" / "people" / IntVar(id) / "edit" =>
      svc.people.find(_.id == id) match {
        case Some(p) => Ok(html.personEditRow(p))
        case None    => NotFound()
      }

    case req @ POST -> Root / "ui" / "people" =>
      for {
        body <- req.bodyText.compile.string
        resp <- UrlForm.parse(body).flatMap(formDecoder.decode) match {
          case Right(input) =>
            svc.createPerson(input.name, input.town)
            Ok(html.peopleRows(svc.people))
          case Left(err) =>
            BadRequest(err.toString)
        }
      } yield resp

    case req @ PUT -> Root / "ui" / "people" / IntVar(id) =>
      for {
        body <- req.bodyText.compile.string
        resp <- UrlForm.parse(body).flatMap(formDecoder.decode) match {
          case Right(input) =>
            svc.updatePerson(id, input.name, input.town)
            Ok(html.peopleRows(svc.people))
          case Left(err) =>
            BadRequest(err.toString)
        }
      } yield resp

    case DELETE -> Root / "ui" / "people" / IntVar(id) =>
      svc.deletePerson(id)
      Ok(html.peopleRows(svc.people))
  }
}
