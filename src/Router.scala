import akka.actor.{Terminated, ActorRef, Props, Actor}

import scala.collection.mutable

object Router {
  def props() = Props[Router]
}

class Router extends Actor {
  val keysMap: mutable.HashMap[String, ActorRef] = mutable.HashMap.empty
  val refsMap: mutable.HashMap[ActorRef, String] = mutable.HashMap.empty

  def receive = {
    case x: SetCommand =>
      val key = x.key
    case x: GetCommand =>
      sender() ! Value(x.key, None)

    case Terminated(ref) =>
      refsMap.get(ref).foreach { key =>
        refsMap -= ref
        keysMap -= key
      }
  }

  def createActorForKey(key: String) = {

  }

}
