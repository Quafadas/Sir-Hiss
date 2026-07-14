$version: "2"

namespace hiss

use alloy#simpleRestJson

@simpleRestJson
service PersonService {
  version: "1.0.0",
  operations: [CreatePerson, GetPerson, UpdatePerson, DeletePerson, ListPeople]
}

@http(method: "POST", uri: "/people", code: 201)
operation CreatePerson {
  input: CreatePersonInput,
  output: Person
}

@readonly
@http(method: "GET", uri: "/people/{id}", code: 200)
operation GetPerson {
  input: PersonIdInput,
  output: Person
}

@idempotent
@http(method: "PUT", uri: "/people/{id}", code: 200)
operation UpdatePerson {
  input: UpdatePersonInput,
  output: Person
}

@idempotent
@http(method: "DELETE", uri: "/people/{id}", code: 204)
operation DeletePerson {
  input: PersonIdInput
}

@readonly
@http(method: "GET", uri: "/people", code: 200)
operation ListPeople {
  output: ListPeopleOutput
}

structure Person {
  @required
  id: Integer,
  @required
  name: String,
  town: String
}

structure PersonIdInput {
  @required
  @httpLabel
  id: Integer
}

structure CreatePersonInput {
  @required
  name: String,
  town: String
}

structure UpdatePersonInput {
  @required
  @httpLabel
  id: Integer,
  @required
  name: String,
  town: String
}

structure ListPeopleOutput {
  @required
  people: PeopleList
}

list PeopleList {
  member: Person
}