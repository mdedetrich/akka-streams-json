package org.mdedetrich.akka.json.stream

import akka.NotUsed
import akka.stream.Attributes.name
import akka.stream.scaladsl.{Flow, Keep, Sink}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream._
import akka.util.ByteString
import org.typelevel.jawn.AsyncParser.ValueStream
import org.typelevel.jawn._

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.util.Try
import java.nio.ByteBuffer

object JsonStreamParser {

  private[this] val jsonStream = name("json-stream")

  def apply[J: RawFacade]: Graph[FlowShape[ByteString, J], NotUsed] =
    apply[J](ValueStream)

  def apply[J: RawFacade](mode: AsyncParser.Mode): Graph[FlowShape[ByteString, J], NotUsed] =
    new JsonStreamParser(mode)

  def flow[J: RawFacade]: Flow[ByteString, J, NotUsed] =
    Flow.fromGraph(apply[J]).withAttributes(jsonStream)

  def flow[J: RawFacade](mode: AsyncParser.Mode): Flow[ByteString, J, NotUsed] =
    Flow.fromGraph(apply[J](mode)).withAttributes(jsonStream)

  def head[J: RawFacade]: Sink[ByteString, Future[J]] =
    flow.toMat(Sink.head)(Keep.right)

  def head[J: RawFacade](mode: AsyncParser.Mode): Sink[ByteString, Future[J]] =
    flow(mode).toMat(Sink.head)(Keep.right)

  def headOption[J: RawFacade]: Sink[ByteString, Future[Option[J]]] =
    flow.toMat(Sink.headOption)(Keep.right)

  def headOption[J: RawFacade](mode: AsyncParser.Mode): Sink[ByteString, Future[Option[J]]] =
    flow(mode).toMat(Sink.headOption)(Keep.right)

  def parse[J: RawFacade](bytes: ByteString): Try[J] =
    Parser.parseFromByteBuffer(bytes.asByteBuffer)

  private final class ParserLogic[J: RawFacade](parser: AsyncParser[J], shape: FlowShape[ByteString, J])
      extends GraphStageLogic(shape) {
    private[this] val in      = shape.in
    private[this] val out     = shape.out
    private[this] val scratch = new ArrayBuffer[J](64)

    setHandler(out, new OutHandler {
      override def onPull(): Unit             = pull(in)
      override def onDownstreamFinish(): Unit = downstreamFinish()
    })
    setHandler(in, new InHandler {
      override def onPush(): Unit           = upstreamPush()
      override def onUpstreamFinish(): Unit = finishParser()
    })

    private def upstreamPush(): Unit = {
      scratch.clear()
      val input = grab(in).asByteBuffers
      emitOrPullLoop(input.iterator, scratch)
    }

    private def downstreamFinish(): Unit = {
      parser.finish()
      cancel(in)
    }

    private def finishParser(): Unit =
      parser.finish() match {
        case Left(ParseException("exhausted input", _, _, _)) => complete(out)
        case Left(e)                                          => failStage(e)
        case Right(jsons)                                     => emitMultiple(out, jsons.iterator, () => complete(out))
      }

    @tailrec
    private[this] def emitOrPullLoop(bs: Iterator[ByteBuffer], results: ArrayBuffer[J]): Unit =
      if (bs.hasNext) {
        val next   = bs.next()
        val absorb = parser.absorb(next)
        absorb match {
          case Left(e) => failStage(e)
          case Right(jsons) =>
            if (jsons.nonEmpty) {
              results ++= jsons
            }
            emitOrPullLoop(bs, results)
        }
      } else {
        if (results.nonEmpty) {
          emitMultiple(out, results.iterator)
        } else {
          pull(in)
        }
      }
  }
}

final class JsonStreamParser[J: RawFacade] private (mode: AsyncParser.Mode)
    extends GraphStage[FlowShape[ByteString, J]] {
  private[this] val in  = Inlet[ByteString]("Json.in")
  private[this] val out = Outlet[J]("Json.out")
  override val shape    = FlowShape(in, out)
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new JsonStreamParser.ParserLogic[J](AsyncParser[J](mode), shape)
}
