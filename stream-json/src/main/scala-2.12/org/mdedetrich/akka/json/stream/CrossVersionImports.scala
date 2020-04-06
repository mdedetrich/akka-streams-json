package org.mdedetrich.akka.json.stream

import org.typelevel.jawn.Facade

private[this] object CrossVersionImports {
  type JFacade[T] = Facade[T]
}
