$version: "2"

namespace hiss

use alloy#simpleRestJson

structure Person {
  @required
  id: Integer,
  @required
  name: String,
  town: String
}

list PeopleList {
  member: Person
}

@simpleRestJson
service PersonService {
  version: "1.0.0",
  operations: [CreatePerson, GetPerson, UpdatePerson, DeletePerson, AllPeople]
}

@http(method: "POST", uri: "/people", code: 201)
operation CreatePerson {
  input := {
    @required
    name: String,
    town: String
  },
  output: Person
}

@readonly
@http(method: "GET", uri: "/people/{id}", code: 200)
operation GetPerson {
  input := {
    @httpLabel
    @required
    id: Integer
  }
  output: Person
}

@idempotent
@http(method: "PUT", uri: "/people/{id}", code: 200)
operation UpdatePerson {
  input := {
    @required
    @httpLabel
    id: Integer,
    @required
    name: String,
    town: String
  },
  output: Person
}

@idempotent
@http(method: "DELETE", uri: "/people/{id}", code: 204)
operation DeletePerson {
  input := {
    @httpLabel
    @required
    id: Integer
  }
}

@readonly
@http(method: "GET", uri: "/people", code: 200)
operation AllPeople {
  output := {
    @required
    people: PeopleList
  }
}