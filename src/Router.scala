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

    case FlushAllCommand =>
      context.children.foreach { ref =>
        context.stop(ref)
      }

      sender() ! Ok

    case x => sender() ! ServerError(s"Cannot execute command: $x")
  }
}
