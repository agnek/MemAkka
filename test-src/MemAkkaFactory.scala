import akka.actor.ActorSystem

object MemAkkaFactory {

  lazy val system: ActorSystem = {
      Memakka.createSystem()
  }

}
