package org.mdedetrich.akka.http.support

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import io.circe.{Decoder, Encoder, Json, Printer}
import org.mdedetrich.akka.http.JsonSupport
import org.mdedetrich.akka.stream.support.{CirceStreamSupport, CirceSupportParser}

trait CirceHttpSupport extends JsonSupport {

  implicit def circeJsonUnmarshaller: FromEntityUnmarshaller[Json] =
    jsonUnmarshaller[Json](CirceSupportParser.facade)

  implicit def circeUnmarshaller[A: Decoder]: FromEntityUnmarshaller[A] =
    circeJsonUnmarshaller.map(CirceStreamSupport.decodeJson[A])

  implicit def circeJsonMarshaller(implicit P: Printer = Printer.noSpaces): ToEntityMarshaller[Json] =
    Marshaller.StringMarshaller.wrap(`application/json`)(P.pretty)

  implicit def circeMarshaller[A](implicit A: Encoder[A], P: Printer = Printer.noSpaces): ToEntityMarshaller[A] =
    circeJsonMarshaller.compose(A.apply)
}

object CirceHttpSupport extends CirceHttpSupport
