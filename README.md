# Sir-Hiss
Sibilant Software a Simple Synchronous Scala Swirl Server Stack

An experiment in server side scala-development, using smithy4s, http4s, and twirl.

Smithy definitions used as the source of truth for the machine JSON API. Twirl and http4s routes used for forms for Humans. Smithy provides the decoding.

`./mill hiss.run` will start the server on port 9000. Try `http://localhost:9000/ui` and `http://localhost:9000/docs`.

`./mill hiss.test` will run the tests.