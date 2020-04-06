package org.mdedetrich.akka.json.stream

import org.typelevel.jawn.RawFacade

private[this] object CrossVersionImports {
  type JFacade[T] = RawFacade[T]
}
