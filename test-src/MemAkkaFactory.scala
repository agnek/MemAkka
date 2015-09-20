import akka.actor.ActorSystem

object MemAkkaFactory {

  var startPort = 21211

  def createSystem(): (Int, ActorSystem) = {
    synchronized {
      val port = startPort
      val system = Memakka.createSystem(port)
      startPort += 1
      (port, system)
    }

  }

}
