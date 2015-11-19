import akka.actor.ActorSystem

object MemAkkaFactory {

  var startPort = 21211

  lazy val system: (Int, ActorSystem) = {
      val port = startPort
      val system = Memakka.createSystem(port)
      //startPort += 1
      (port, system)
  }

}
