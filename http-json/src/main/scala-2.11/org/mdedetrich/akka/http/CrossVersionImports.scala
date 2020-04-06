package org.mdedetrich.akka.http

import org.typelevel.jawn.RawFacade

private[this] object CrossVersionImports {
  type JFacade[T] = RawFacade[T]
}
