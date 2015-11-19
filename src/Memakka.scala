import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.cluster.sharding.{ClusterShardingSettings, ClusterSharding}
import com.typesafe.config.{ConfigFactory, Config}
import scala.concurrent.{Promise, Await}
import scala.concurrent.duration.Duration

object Memakka {

  def main (args: Array[String]) {
    val system = createSystem()
    Await.result(system.whenTerminated, Duration.Inf)
  }

  def createSystem(portToListen: Int = 11211): ActorSystem = {
    val config = ConfigFactory.load("main.conf")
    val system = ActorSystem.create("memakka", config)

    val promise = Promise[Unit]()

    Cluster(system).registerOnMemberUp  {

      val keysRegion = ClusterSharding(system).start(
        "keys",
        entityProps = Entry.props(),
        settings = ClusterShardingSettings(system),
        extractEntityId = Entry.idExtractor,
        extractShardId = Entry.shardResolver
      )

      system.actorOf(TcpServer.props("127.0.0.1", portToListen, promise), "tcp")
    }


    Await.result(promise.future, Duration.Inf)

    system
  }

}
