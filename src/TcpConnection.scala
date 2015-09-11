import akka.actor._
import akka.io.Tcp
import akka.io.Tcp._
import akka.util.ByteString
import TcpConnection._

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
  case class CommandData(data: DataCommand) extends StateData

  case class ConnectionState(buffer: ByteString, stateData: StateData)

  case object CheckBuffer
}
//https://github.com/memcached/memcached/blob/master/doc/protocol.txt
class TcpConnection(val connection: ActorRef) extends Actor with FSM[State, ConnectionState]  {
  import TcpConnection._

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
          case Some(x: DataCommand) =>
            //TODO:: send message to router
            goto(WaitingForData) using ConnectionState(newStateBytes, CommandData(x))
          case Some(x: Command) =>
            //TODO:: send message to router
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

      //Check if we have enought data to try parse bytes
      if(buffer.length >= commandBytesLength + 2) {
        if(buffer(commandBytesLength) == rByte && buffer(commandBytesLength + 1) == nByte) {
          val commandData = buffer.take(commandBytesLength)
          val connectionBuffer = buffer.drop(commandBytesLength + 2)
          //TODO:: send message to router
          //router ! (command, commandData)

          goto(WaitingForResponse) using ConnectionState(connectionBuffer, EmptyData)
        }
        else {
          //TODO:: send error to cliend
          val connectionBuffer = buffer.drop(commandBytesLength + 2)

          //TODO:: checn buffer for new command
          goto(WaitingForData) using ConnectionState(connectionBuffer, EmptyData)
        }
      }
      else
        stay() using ConnectionState(buffer, command)
  }

  when(WaitingForResponse) {
    case Event(x: Response, stateData) =>
      val response = x.toString + "\r\n"
      connection ! Tcp.Write(ByteString(response))
      goto(WaitingForCommand) using stateData
  }

  whenUnhandled {
    case Event(Received(data), x: ConnectionState) =>
      self ! CheckBuffer
      stay() using x.copy(buffer = x.buffer ++ data)
  }

  onTransition {
    case _ -> WaitingForData =>
      self ! CheckBuffer
    case _ -> WaitingForCommand =>
      self ! CheckBuffer
  }

  initialize()
}
