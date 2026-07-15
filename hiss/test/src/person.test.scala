package hiss

class PersonSuite extends munit.FunSuite {

  // Fresh instance per test — no shared mutable state
  def svc: PersonServiceT = new PersonServiceT {}

  // ── List ────────────────────────────────────────────────────────────────────

  test("allPeople returns all seed people") {
    val result = svc.allPeople()
    assertEquals(result.people.length, 3)
  }

  // ── Get ─────────────────────────────────────────────────────────────────────

  test("getPerson finds by id") {
    val p = svc.getPerson(1)
    assertEquals(p.name, "John")
    assertEquals(p.town, Some("Doe"))
  }

  test("getPerson throws for unknown id") {
    intercept[Exception](svc.getPerson(999))
  }

  // ── Create ──────────────────────────────────────────────────────────────────

  test("createPerson assigns next id and appends") {
    val s = svc
    val p = s.createPerson("Bob", Some("Bath"))
    assertEquals(p.id, 4)
    assertEquals(p.name, "Bob")
    assertEquals(p.town, Some("Bath"))
    assertEquals(s.allPeople().people.length, 4)
  }

  test("createPerson with no town stores None") {
    val s = svc
    val p = s.createPerson("Eve", None)
    assertEquals(p.town, None)
  }

  test("createPerson ids increment across multiple creates") {
    val s = svc
    val first  = s.createPerson("A", None)
    val second = s.createPerson("B", None)
    assertEquals(second.id, first.id + 1)
  }

  // ── Update ──────────────────────────────────────────────────────────────────

  test("updatePerson changes name and town") {
    val s = svc
    val updated = s.updatePerson(2, "Janet", Some("York"))
    assertEquals(updated.name, "Janet")
    assertEquals(updated.town, Some("York"))
    assertEquals(s.getPerson(2).name, "Janet")
  }

  test("updatePerson does not affect other rows") {
    val s = svc
    s.updatePerson(1, "Johnny", None)
    assertEquals(s.allPeople().people.length, 3)
    assertEquals(s.getPerson(2).name, "Jane")
  }

  // ── Patch ───────────────────────────────────────────────────────────────────

  test("patchPerson changes town without affecting id or name") {
    val s = svc
    val original = s.getPerson(2)
    val patched = s.patchPerson(2, "York")
    assertEquals(patched.id, original.id)
    assertEquals(patched.name, original.name)
    assertEquals(patched.town, Some("York"))
    assertEquals(s.getPerson(2), patched)
  }

  test("patchPerson does not affect other rows") {
    val s = svc
    s.patchPerson(1, "Bath")
    assertEquals(s.allPeople().people.length, 3)
    assertEquals(s.getPerson(2), Person(2, "Jane", Some("Doe")))
  }

  // ── Delete ──────────────────────────────────────────────────────────────────

  test("deletePerson removes the person") {
    val s = svc
    s.deletePerson(1)
    assertEquals(s.allPeople().people.length, 2)
    intercept[Exception](s.getPerson(1))
  }

  test("deletePerson with unknown id is a no-op") {
    val s = svc
    s.deletePerson(999)
    assertEquals(s.allPeople().people.length, 3)
  }

  // ── Round-trip ──────────────────────────────────────────────────────────────

  test("create then get round-trips correctly") {
    val s = svc
    val created = s.createPerson("Carol", Some("Chester"))
    val fetched = s.getPerson(created.id)
    assertEquals(fetched, created)
  }

  test("create, update, delete leaves original count") {
    val s = svc
    val p = s.createPerson("Temp", None)
    s.updatePerson(p.id, "Temp Updated", Some("Anywhere"))
    s.deletePerson(p.id)
    assertEquals(s.allPeople().people.length, 3)
  }
}
