package org.mdedetrich.akka.http

import org.typelevel.jawn.Facade

private[this] object CrossVersionImports {
  type JFacade[T] = Facade[T]
}
