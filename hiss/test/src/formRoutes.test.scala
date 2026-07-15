package hiss

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.*
import org.http4s.implicits.*

class FormRoutesSuite extends munit.FunSuite {

  // Fresh service + routes per test — no shared mutable state
  def freshSvc: PersonServiceT             = new PersonServiceT {}
  def routes(svc: PersonServiceT)          = FormRoutes.routes(svc)
  def exec(req: Request[IO], svc: PersonServiceT): Response[IO] =
    routes(svc).run(req).value.unsafeRunSync().getOrElse(Response.notFound)
  def body(resp: Response[IO]): String =
    resp.bodyText.compile.string.unsafeRunSync()

  // ── GET /ui/people/rows ────────────────────────────────────────────────────

  test("GET /ui/people/rows returns 200 with all seed names") {
    val resp = exec(Request(Method.GET, uri"/ui/people/rows"), freshSvc)
    assertEquals(resp.status, Status.Ok)
    val b = body(resp)
    assert(b.contains("John"),  "expected John")
    assert(b.contains("Jane"),  "expected Jane")
    assert(b.contains("Alice"), "expected Alice")
  }

  // ── GET /ui/people/{id}/edit ───────────────────────────────────────────────

  test("GET /ui/people/1/edit returns 200 with edit form for John") {
    val resp = exec(Request(Method.GET, Uri.unsafeFromString("/ui/people/1/edit")), freshSvc)
    assertEquals(resp.status, Status.Ok)
    val b = body(resp)
    assert(b.contains("John"), "expected John in edit row")
    assert(b.contains("""hx-put="/ui/people/1""""), "expected PUT target")
  }

  test("GET /ui/people/999/edit returns 404 for unknown id") {
    val resp = exec(Request(Method.GET, Uri.unsafeFromString("/ui/people/999/edit")), freshSvc)
    assertEquals(resp.status, Status.NotFound)
  }

  // ── POST /ui/people ────────────────────────────────────────────────────────

  test("POST /ui/people creates person and returns updated rows") {
    val svc  = freshSvc
    val req  = Request[IO](Method.POST, uri"/ui/people")
      .withEntity("name=Dave&town=York")
    val resp = exec(req, svc)
    assertEquals(resp.status, Status.Ok)
    assert(body(resp).contains("Dave"), "expected Dave in response")
    assertEquals(svc.allPeople().people.length, 4)
  }

  test("POST /ui/people with no town creates person with no town") {
    val svc  = freshSvc
    val req  = Request[IO](Method.POST, uri"/ui/people").withEntity("name=Eve")
    val resp = exec(req, svc)
    assertEquals(resp.status, Status.Ok)
    assertEquals(svc.people.find(_.name == "Eve").flatMap(_.town), None)
  }

  test("POST /ui/people with missing required name returns 400") {
    val req  = Request[IO](Method.POST, uri"/ui/people").withEntity("town=York")
    val resp = exec(req, freshSvc)
    assertEquals(resp.status, Status.BadRequest)
  }

  // ── PUT /ui/people/{id} ────────────────────────────────────────────────────

  test("PUT /ui/people/1 updates name and returns updated rows") {
    val svc  = freshSvc
    val req  = Request[IO](Method.PUT, Uri.unsafeFromString("/ui/people/1"))
      .withEntity("name=Johnny&town=Bath")
    val resp = exec(req, svc)
    assertEquals(resp.status, Status.Ok)
    val b = body(resp)
    assert(b.contains("Johnny"), "expected updated name")
    assert(!b.contains(">John<"), "old name should be gone")
    assertEquals(svc.getPerson(1).name, "Johnny")
  }

  // ── DELETE /ui/people/{id} ─────────────────────────────────────────────────

  test("DELETE /ui/people/1 removes person and returns updated rows") {
    val svc  = freshSvc
    val req  = Request[IO](Method.DELETE, Uri.unsafeFromString("/ui/people/1"))
    val resp = exec(req, svc)
    assertEquals(resp.status, Status.Ok)
    assert(!body(resp).contains("John"), "John should be gone")
    assertEquals(svc.allPeople().people.length, 2)
  }

  test("DELETE /ui/people/1 then GET /ui/people/1/edit returns 404") {
    val svc = freshSvc
    exec(Request(Method.DELETE, Uri.unsafeFromString("/ui/people/1")), svc)
    val resp = exec(Request(Method.GET, Uri.unsafeFromString("/ui/people/1/edit")), svc)
    assertEquals(resp.status, Status.NotFound)
  }
}
