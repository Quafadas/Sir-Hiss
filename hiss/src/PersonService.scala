package hiss
import cats.Id
import cats.implicits.*

object PersonServiceImpl extends PersonService[Id] {

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

  override def deletePerson(id: Int): Unit =
    people = people.filterNot(_.id == id)

  override def listPeople(): ListPeopleOutput = ListPeopleOutput(people)
}
