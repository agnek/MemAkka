import akka.actor.{Props, ActorRef, Actor}
import akka.cluster.sharding.ClusterSharding

object GetRequestHolder {
  def props(keys: Seq[String], connection: ActorRef, withCas: Boolean = false) =
    Props(new GetRequestHolder(keys, connection, withCas))
}

class GetRequestHolder(keys: Seq[String], connection: ActorRef, withCas: Boolean) extends Actor {
  val keysRouter = ClusterSharding(context.system).shardRegion("keys")

  if(keys.isEmpty) {
    connection ! Values(Seq.empty)
    context.stop(self)
  }

  keys.foreach { key =>
    keysRouter ! GetCommand(Seq(key), withCas)
  }

  def receive = waitingForKeys(keys.length, Seq.empty)

  def waitingForKeys(valuesToWait: Int, values: Seq[Value]): Receive = {
    case NotFound =>
      if(valuesToWait == 1) {
        connection ! Values(values)
        context.stop(self)
      }
      else
        context.become(waitingForKeys(valuesToWait - 1, values))

    case x: Value =>
      if(valuesToWait == 1) {
        connection ! Values(values :+ x)
        context.stop(self)
      }
      else
        context.become(waitingForKeys(valuesToWait - 1, values :+ x))
  }
}
