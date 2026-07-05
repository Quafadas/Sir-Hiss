$version: "2"

namespace smithy4s.example.hello

use alloy#simpleRestJson

@simpleRestJson
service HelloWorldService {
  version: "1.0.0",
  operations: [GetHello, Hello]
}

@readonly
@http(method: "GET", uri: "/name/{name}", code: 200)
operation GetHello {
  input: Person,
  output: Greeting
}


@http(method: "POST", uri: "/name/{name}", code: 200)
operation Hello {
  input: Person,
  output: Greeting
}

structure Person {
  @httpLabel
  @required
  name: String,

  @httpQuery("town")
  town: String
}

structure Greeting {
  @required
  message: String
}