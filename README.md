[![Build Status][ci-img]][ci]
[![Coverage][coverage-img]][coverage]
[![Maven][maven-img]][maven]
[![Join at Gitter][gitter-img]][gitter]
[![Apache License][license-img]][license]

# Akka Streams Json Support

This library provides Json support for stream based applications using [jawn](https://github.com/non/jawn)
as a parser. It supports all backends that jawn supports with support for [circe](https://github.com/travisbrown/circe) provided as a example.

## Differences with [akka-stream-json](https://github.com/knutwalker/akka-stream-json)

This is a fork of [akka-stream-json](https://github.com/knutwalker/akka-stream-json) since it no longer
appears to be maintained. Hence the main differences between akka-stream-json and this project is.

* The project is called akka-streams-json rather than akka-stream-json. This is
just to differentiate the git repos and project names.
* This project aims to be updated to the latest versions of akka/akka-http/circe
as frequent as possible. PR's are also welcome to maintain the project
* We use [ScalaTest](http://www.scalatest.org/) rather then [Specs2](https://etorreborre.github.io/specs2/) for testing.
This is because ScalaTest has better `Future` support compared to Specs2
(in order to do tests against `Future` in Specs2 you have to block on the `Future`
which degrades performance a lot).

Apart from this, the actual initial code/implementation is exactly the same as the fork, the only difference
being the package names (i.e. using `org.mdedetrich` rather than `de.knutwalker`). The following contents of
README.md was also mainly copied from the original repo.

## Installation

There are two main modules, `akka-stream-json` and `akka-http-json`.
`akka-stream-json` is the basis and provides the stream-based parser while
`akka-http-json` enabled support to use the desired json library as an Unmarshaller.


```
libraryDependencies ++= List(
  "org.mdedetrich" %% "akka-stream-json" % "0.5.0",
  "org.mdedetrich" %% "akka-http-json" % "0.5.0"
)
```

`akka-streams-json` depends on `jawn-parser` at version `1.0.0` for the `0.5.x` series and `0.1.14` for the `0.4.x` series.
and is compiled against `akka-stream` at version `2.5.x`.
The circe submodule depends on version `0.13.x` of `circe-jawn` for `0.5.x` series and `0.12.x` for the `0.4.x` series
The Akka Http submodule depends on version `10.1.x` of `akka-http`

`akka-stream-json` is published for Scala 2.13, 2.12 and 2.11.

## Usage

The parser lives at `org.mdedetrich.akka.json.stream.JsonStreamParser`

Use one of the constructor methods in the companion object to create the parser at
various levels of abstraction, either a Stage, a Flow, or a Sink.
You just add the [jawn support facade](https://github.com/non/jawn#supporting-external-asts-with-jawn)
of your choice and you will can parsed into their respective Json AST.


For Http support, either `import org.mdedetrich.akka.http.JsonSupport._`
or mixin `... with org.mdedetrich.akka.http.JsonSupport`.

Given an implicit jawn facade, this enable you to decode into the respective Json AST
using the Akka HTTP marshalling framework. As jawn is only about parsing and does not abstract
over rendering, you'll only get an Unmarshaller.


### Circe

```
libraryDependencies ++= List(
  "org.mdedetrich" %% "akka-stream-circe" % "0.5.0",
  "org.mdedetrich" %% "akka-http-circe" % "0.5.0"
)
```

(Using circe 0.13.x)

Adding support for a specific framework is
[quite](support/stream-circe/src/main/scala/org/mdedetrich/akka/stream/support/CirceStreamSupport.scala)
[easy](support/http-circe/src/main/scala/org/mdedetrich/akka/http/support/CirceHttpSupport.scala).

These support modules allow you to directly marshall from/unmarshall into your data types
using circes `Decoder` and `Encoder` type classes.

Just mixin or import `org.mdedetrich.akka.http.support.CirceHttpSupport` for Http
or pipe your `Source[ByteString, _].via(org.mdedetrich.akka.stream.CirceStreamSupport.decode[A])`
to get a `Source[A, _]`.

This flow even supports parsing multiple json documents in whatever
fragmentation they may arrive, which is great for consuming stream/sse based APIs.

If there is an error in parsing the Json you can catch `org.mdedetrich.akka.http.support.CirceStreamSupport.JsonParsingException`.
The exception provides Circe cursor history, current cursor and the type hint of the error.

## Why jawn?

Jawn provides a nice interface for asynchronous parsing.
Most other Json marshalling provider will consume the complete entity
at first, convert it to a string and then start to parse.
With jawn, the json is incrementally parsed with each arriving data chunk,
using directly the underlying ByteBuffers without conversion.

## License

This code is open source software licensed under the Apache 2.0 License.

[ci-img]: https://img.shields.io/travis/mdedetrich/akka-streams-json/master.svg
[coverage-img]: https://img.shields.io/codecov/c/github/mdedetrich/akka-streams-json/master.svg
[maven-img]: https://img.shields.io/maven-central/v/org.mdedetrich/akka-stream-json_2.12.svg?label=latest
[gitter-img]: https://img.shields.io/badge/gitter-Join_Chat-1dce73.svg
[license-img]: https://img.shields.io/badge/license-APACHE_2-green.svg

[ci]: https://travis-ci.org/mdedetrich/akka-streams-json
[coverage]: https://codecov.io/github/mdedetrich/akka-streams-json
[maven]: http://search.maven.org/#search|ga|1|g%3A%22org.mdedetrich%22%20AND%20%28a%3Aakka-stream-*_2.11%20OR%20a%3Aakka-http-*_2.11%20OR%20a%3Aakka-stream-*_2.12%20OR%20a%3Aakka-http-*_2.12%29
[gitter]: https://gitter.im/mdedetrich/akka-streams-json?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge
[license]: https://www.apache.org/licenses/LICENSE-2.0
