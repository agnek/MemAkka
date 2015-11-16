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
    case command @ (x: SetCommand, bytes) =>
      val entryRef = getActorForKey(x.key).getOrElse(createActorForKey(x.key))
      entryRef forward command

    case command @ (x: AddCommand, bytes) =>
      val entryRef = getActorForKey(x.key).getOrElse(createActorForKey(x.key))
      entryRef forward command

    case command @ (x: ReplaceCommand, bytes: ByteString) =>
      val refOpt = getActorForKey(x.key)

      refOpt match {
        case None => sender() ! NotStored
        case Some(ref) => ref forward command
      }

    case command @ (x: AppendCommand, bytes: ByteString) =>
      val refOpt = getActorForKey(x.key)

      refOpt match {
        case None => sender() ! NotStored
        case Some(ref) => ref forward command
      }

    case command @ (x: PrependCommand, bytes: ByteString) =>
      val refOpt = getActorForKey(x.key)

      refOpt match {
        case None => sender() ! NotStored
        case Some(ref) => ref forward command
      }

    case x: GetCommand =>
      val existingKeys = x.keys.filter(actorForKeyExists)
      context.actorOf(GetRequestHolder.props(existingKeys, sender()))

    case DeleteCommand(key) =>
      getActorForKey(key) match {
        case None => sender() ! NotFound
        case Some(actor) =>
          context.stop(actor)
          sender() ! Deleted
      }

    case x: IncrementCommand =>
      getActorForKey(x.key) match {
        case None => sender() ! NotFound
        case Some(ref) => ref forward x
      }

    case x: DecrementCommand =>
      getActorForKey(x.key) match {
        case None => sender() ! NotFound
        case Some(ref) => ref forward x
      }

    case x => sender() ! ServerError(s"Cannot execute command: $x")
  }

  def getActorForKey(key: String): Option[ActorRef] = {
    context.child(key)
  }

  def actorForKeyExists(key: String): Boolean = {
    context.child(key).isDefined
  }

  def createActorForKey(key: String): ActorRef = {
    val entryRef = context.actorOf(Entry.props(key), key)

    entryRef
  }
}
