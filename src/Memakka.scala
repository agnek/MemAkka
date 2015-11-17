import akka.actor.ActorSystem

import scala.concurrent.{Promise, Await}
import scala.concurrent.duration.Duration

object Memakka {

  def main (args: Array[String]) {
    val system = createSystem()
    Await.result(system.whenTerminated, Duration.Inf)
  }

  def createSystem(portToListen: Int = 11211): ActorSystem = {
    val system = ActorSystem.create("memakka")

    val promise = Promise[Unit]()

    system.actorOf(TcpServer.props("localhost", portToListen, promise), "tcp")
    system.actorOf(Router.props(), "keys")

    Await.result(promise.future, Duration.Inf)

    system
  }

}
