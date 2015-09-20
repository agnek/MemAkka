import akka.actor.ActorSystem

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Memakka {

  def main (args: Array[String]) {
    val system = createSystem()
    Await.result(system.whenTerminated, Duration.Inf)
  }

  def createSystem(portToListen: Int = 11211): ActorSystem = {
    println("Aaaaaaaaa")
    val system = ActorSystem.create("memakka")

    system.actorOf(TcpServer.props("localhost", portToListen), "tcp")
    system.actorOf(Router.props(), "keys")

    system
  }

}
