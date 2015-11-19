import java.net.InetSocketAddress

import akka.actor.{ActorLogging, Props, Actor}
import akka.dispatch.UnboundedMailbox
import akka.io.Tcp.{CommandFailed, Bound, Register, Connected}
import akka.io.{Tcp, IO}

import scala.concurrent.Promise

object TcpServer {
  def props(host: String, port: Int, onStart: Promise[Unit]) = Props(new TcpServer(host, port, onStart))
}

class TcpServer(host: String, port: Int, onStart: Promise[Unit]) extends Actor with ActorLogging {



  IO(Tcp)(context.system) ! Tcp.Bind(self, new InetSocketAddress(host, port))
  //TODO:: add check for already used port
  def receive = {
    case Bound(local) =>
      onStart.success()
      log.info(s"Bounded to iface: ${local.getHostName} and port: ${local.getPort}")

    case Connected(remote, local) =>
      val connectionActor = context.actorOf(TcpConnection.props(sender()))
      sender() ! Register(connectionActor)

    case CommandFailed(x: Tcp.Bind) =>
      onStart.failure(new Error("Cannot start"))
      log.error(s"Cannot bind to iface: $host and port $port")
      context.system.terminate()
  }
}
