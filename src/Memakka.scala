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

  def createSystem(): ActorSystem = {
    val config = ConfigFactory.load("main.conf")
    val system = ActorSystem.create("memakka", config)

    val host = config.getString("memakka.host")
    val port = config.getInt("memakka.port")

    val promise = Promise[Unit]()

    Cluster(system).registerOnMemberUp  {

      ClusterSharding(system).start(
        "keys",
        entityProps = Entry.props(),
        settings = ClusterShardingSettings(system),
        extractEntityId = Entry.idExtractor,
        extractShardId = Entry.shardResolver
      )

      system.actorOf(TcpServer.props(host, port, promise), "tcp")
    }


    Await.result(promise.future, Duration.Inf)

    system
  }

}
