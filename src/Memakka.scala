import akka.actor.ActorSystem

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Memakka {

  def main (args: Array[String]) {
    println("AAAA")
    val system = ActorSystem.create("memakka")
    val portToListen = 11211

    system.actorOf(TcpServer.props("localhost", portToListen))
    system.actorOf(Router.props(), "router")

    Await.result(system.whenTerminated, Duration.Inf)
  }
}
