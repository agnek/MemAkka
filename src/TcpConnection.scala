import akka.actor._
import akka.io.Tcp
import akka.io.Tcp._
import akka.util.ByteString
import TcpConnection._
import scala.concurrent.duration._

object TcpConnection {
  def props(connection: ActorRef) = Props(new TcpConnection(connection))

  val rByte = '\r'.toByte
  val nByte = '\n'.toByte

  sealed trait State
  case object WaitingForCommand extends State
  case object WaitingForData extends State
  case object WaitingForResponse extends State

  sealed trait StateData
  case object EmptyData extends StateData
  case class CommandData(data: BytesCommand) extends StateData

  case class ConnectionState(buffer: ByteString, stateData: StateData)

  case object CheckBuffer
}
//https://github.com/memcached/memcached/blob/master/doc/protocol.txt
class TcpConnection(val connection: ActorRef) extends Actor with FSM[State, ConnectionState]  {
  import TcpConnection._

  val router = context.actorSelection("/user/keys")

  startWith(WaitingForCommand, ConnectionState(ByteString.empty, EmptyData))

  when(WaitingForCommand) {
    case Event(CheckBuffer, ConnectionState(buffer, data)) =>
      //TODO:: add check for buffer length
      val rPosition = buffer.indexOf(rByte)

      if(rPosition > 0 && buffer(rPosition + 1) == nByte){
        val commandBytes = buffer.take(rPosition)
        val newStateBytes = buffer.drop(rPosition + 2)

        val commandOpt = CommandParser.parse(commandBytes.utf8String)

        commandOpt match {
          case Some(QuitCommand) =>
            connection ! Tcp.Close
            stop()
          case Some(x: BytesCommand) =>
            goto(WaitingForData) using ConnectionState(newStateBytes, CommandData(x))
          case Some(command: Command) =>
            router ! command
            goto(WaitingForResponse) using ConnectionState(newStateBytes, EmptyData)
          case None =>
            connection ! Tcp.Write(ByteString("ERROR\r\n"))
            goto(WaitingForCommand) using ConnectionState(newStateBytes, EmptyData)
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

          router ! (command.data, commandData)

          goto(WaitingForResponse) using ConnectionState(connectionBuffer, EmptyData)
        }
        else {
          connection ! Error.toByteString

          val connectionBuffer = buffer.drop(commandBytesLength + 2)
          goto(WaitingForCommand) using ConnectionState(connectionBuffer, EmptyData)
        }
      }
      else
        stay() using ConnectionState(buffer, command)
  }

  when(WaitingForResponse, 100 milliseconds) {
    case Event(response: Response, stateData) =>
      connection ! Tcp.Write(response.toByteString)
      goto(WaitingForCommand) using stateData
    case Event(StateTimeout, stateData) =>
      connection ! Tcp.Write(ServerError("Timeout").toByteString)
      goto(WaitingForCommand) using stateData

    //case Event(CheckBuffer, _) => stay()
  }

  whenUnhandled {
    case Event(Received(data), x: ConnectionState) =>
      if(stateName != WaitingForResponse)
        self ! CheckBuffer

      stay() using x.copy(buffer = x.buffer ++ data)
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
