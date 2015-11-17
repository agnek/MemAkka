import akka.actor.{Props, ActorRef, Actor}

object GetRequestHolder {
  def props(keys: Seq[String], connection: ActorRef, withCas: Boolean = false) =
    Props(new GetRequestHolder(keys, connection, withCas))
}

class GetRequestHolder(keys: Seq[String], connection: ActorRef, withCas: Boolean) extends Actor {
  if(keys.isEmpty) {
    connection ! End
    context.stop(self)
  }

  keys.foreach { key =>
    context.actorSelection(context.parent.path / key) ! GetCommand(Seq(key), withCas)
  }

  def receive = waitingForKeys(keys, Seq.empty)

  def waitingForKeys(keys: Seq[String], values: Seq[Value]): Receive = {
    case x: Value =>
      val newKeys = keys.filter(_ != x.key)

      if(newKeys.isEmpty) {
        connection ! Values(values :+ x)
        context.stop(self)
      }
      else
        context.become(waitingForKeys(newKeys, values :+ x))
  }
}
