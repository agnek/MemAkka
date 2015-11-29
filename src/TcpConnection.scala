import akka.actor._
import akka.cluster.sharding.ClusterSharding
import akka.io.Tcp
import akka.io.Tcp._
import akka.util.ByteString
import TcpConnection._

object TcpConnection {
  def props(connection: ActorRef) = Props(new TcpConnection(connection))

  val rByte = '\r'.toByte
  val nByte = '\n'.toByte

  case object CheckBuffer

  sealed trait State
  case object WaitingForCommand extends State
  case object WaitingForData extends State
  case object WaitingForResponse extends State
  case object WaitingForGetResponse extends State

  sealed trait StateData
  case object EmptyData extends StateData
  case class CommandData(data: BytesCommand) extends StateData
  case class WaitingForGetData(responsesToWait: Int) extends StateData

  case class ConnectionState(buffer: ByteString, stateData: StateData)

}

//https://github.com/memcached/memcached/blob/master/doc/protocol.txt
class TcpConnection(val connection: ActorRef) extends Actor with FSM[State, ConnectionState]  {
  import TcpConnection._

  val keys = ClusterSharding(context.system).shardRegion("keys")

  startWith(WaitingForCommand, ConnectionState(ByteString.empty, EmptyData))

  when(WaitingForCommand) {
    case Event(CheckBuffer, ConnectionState(buffer, data)) =>
      val rPosition = buffer.indexOf(rByte)

      if(rPosition > 0 && buffer(rPosition + 1) == nByte){
        val commandBytes = buffer.take(rPosition)
        val newStateBytes = buffer.drop(rPosition + 2)

        val commandOpt = CommandParser.parseUnsafe(commandBytes.utf8String)

        commandOpt match {
          case QuitCommand =>
            connection ! Tcp.Close
            stop()
          case x: BytesCommand =>
            goto(WaitingForData) using ConnectionState(newStateBytes, CommandData(x))
          case command: GetCommand =>
            command.keys.foreach { key =>
              keys ! GetInternalCommand(key, command.withCas)
            }

            goto(WaitingForGetResponse) using ConnectionState(newStateBytes, WaitingForGetData(command.keys.length))
          case command: Command =>
            keys ! command
            goto(WaitingForResponse) using ConnectionState(newStateBytes, EmptyData)
        }
      }
      else
        stay() using ConnectionState(buffer, data)
  }


  when(WaitingForData) {
    case Event(CheckBuffer, ConnectionState(buffer, command: CommandData)) =>
      val commandBytesLength = command.data.bytes

      //Check if we have enough data to try parse bytes
      if(buffer.length >= commandBytesLength + 2) {
        if(buffer(commandBytesLength) == rByte && buffer(commandBytesLength + 1) == nByte) {
          val commandData = buffer.take(commandBytesLength)
          val connectionBuffer = buffer.drop(commandBytesLength + 2)

          keys ! (command.data, commandData)

          goto(WaitingForResponse) using ConnectionState(connectionBuffer, EmptyData)
        }
        else {
          connection ! Error.asByteString

          val connectionBuffer = buffer.drop(commandBytesLength + 2)
          goto(WaitingForCommand) using ConnectionState(connectionBuffer, EmptyData)
        }
      }
      else
        stay() using ConnectionState(buffer, command)
  }

  when(WaitingForResponse) {
    case Event(response: Response, stateData) =>
      connection ! Tcp.Write(response.asByteString)
      goto(WaitingForCommand) using stateData

    case Event(CheckBuffer, _) => stay()
  }

  when(WaitingForGetResponse) {
    case Event(value: Response, ConnectionState(connectionBuffer, x: WaitingForGetData)) =>
      if(value.isInstanceOf[Value]) connection ! Tcp.Write(value.asByteString)

      if(x.responsesToWait == 1) {
        connection ! Tcp.Write(End.asByteString)
        goto(WaitingForCommand) using ConnectionState(connectionBuffer, EmptyData)
      }
      else
        stay() using ConnectionState(connectionBuffer, x.copy(responsesToWait = x.responsesToWait - 1))
  }

  whenUnhandled {
    case Event(Received(data), x: ConnectionState) =>
      if(stateName != WaitingForResponse && stateName != WaitingForGetResponse) {
        self ! CheckBuffer
      }

      stay() using x.copy(buffer = x.buffer ++ data)

    //In some cases CheckBuffer can be already in mailbox and we should just ignore it
    case Event(CheckBuffer, _) =>
      stay()

    case Event(PeerClosed, _) =>
      context.stop(self)
      stay()
  }

  onTransition {
    case _ -> WaitingForData =>
      self ! CheckBuffer

    case _ -> WaitingForCommand =>
      self ! CheckBuffer
  }

  initialize()
}
