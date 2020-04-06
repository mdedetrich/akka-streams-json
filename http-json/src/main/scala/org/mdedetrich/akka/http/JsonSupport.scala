package org.mdedetrich.akka.http

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.http.scaladsl.util.FastFuture
import org.mdedetrich.akka.json.stream.JsonStreamParser
import CrossVersionImports.JFacade

trait JsonSupport {

  implicit def jsonUnmarshaller[J: JFacade]: FromEntityUnmarshaller[J] =
    Unmarshaller
      .withMaterializer[HttpEntity, J](_ =>
        implicit mat => {
          case HttpEntity.Strict(_, data) =>
            FastFuture(JsonStreamParser.parse[J](data))
          case entity => entity.dataBytes.runWith(JsonStreamParser.head[J])
      })
      .forContentTypes(`application/json`)
}

object JsonSupport extends JsonSupport
