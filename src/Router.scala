import akka.actor._

import scala.collection.mutable

object Router {
  def props() = Props[Router]
}

class Router extends Actor {
  println("Router started with path: " + self.path.toString)

  val keysMap: mutable.HashMap[String, ActorRef] = mutable.HashMap.empty
  val refsMap: mutable.HashMap[ActorRef, String] = mutable.HashMap.empty

  //TODO:: add description
  override def supervisorStrategy = SupervisorStrategy.stoppingStrategy

  def receive = {
    case x: SetCommand =>
      val key = x.key
    case x: GetCommand =>
      keysMap.get(x.key) match {
        case None => sender() ! NotFound
        case Some(ref) => ref forward x
      }

    case Terminated(ref) =>
      refsMap.get(ref).foreach { key =>
        refsMap -= ref
        keysMap -= key
      }
  }

  def createActorForKey(key: String) = {

  }

}
