/*
 * Copyright 2015 – 2018 Paul Horn
 * Copyright 2018 – 2021 Matthew de Detrich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mdedetrich.akka.http

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.http.scaladsl.util.FastFuture
import org.mdedetrich.akka.json.stream.JsonStreamParser
import org.typelevel.jawn.Facade

trait JsonSupport {

  implicit def jsonUnmarshaller[J: Facade]: FromEntityUnmarshaller[J] =
    Unmarshaller
      .withMaterializer[HttpEntity, J](_ =>
        implicit mat => {
          case HttpEntity.Strict(_, data) =>
            FastFuture(JsonStreamParser.parse[J](data))
          case entity => entity.dataBytes.runWith(JsonStreamParser.head[J])
        }
      )
      .forContentTypes(`application/json`)
}

object JsonSupport extends JsonSupport
