import akka.actor._
import akka.util.ByteString

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
    case (command: SetCommand, bytes) =>
      val key = command.key
      val entryRef = getActorForKey(key).getOrElse(createActorForKey(key))

      entryRef forward (command: SetCommand, bytes)

    case (command: AddCommand, bytes) =>
      val key = command.key
      val entryRef = getActorForKey(key).getOrElse(createActorForKey(key))

      entryRef forward (command: AddCommand, bytes)

    case x: GetCommand =>

      getActorForKey(x.key) match {
        case None => sender() ! NoValue
        case Some(ref) => ref forward x
      }

    case Terminated(ref) =>
      refsMap.get(ref).foreach { key =>
        refsMap -= ref
        keysMap -= key
      }

    case x => sender() ! ServerError(s"Cannot execute command: $x")
  }

  def getActorForKey(key: String): Option[ActorRef] = {
    keysMap.get(key)
  }

  def createActorForKey(key: String): ActorRef = {
    val entryRef = context.actorOf(Entry.props(key))

    keysMap += (key -> entryRef)
    refsMap += (entryRef -> key)

    entryRef
  }
}
