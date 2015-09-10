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
}
//https://github.com/memcached/memcached/blob/master/doc/protocol.txt
class TcpConnection(val connection: ActorRef) extends Actor with FSM[State, ConnectionState]  {
  import TcpConnection._

  startWith(WaitingForCommand, ConnectionState(ByteString.empty, EmptyData))

  when(WaitingForCommand) {
    case Event(Received(buffer), ConnectionState(stateBuffer, data)) =>
      //TODO:: add check for buffer length
      val newBuffer = stateBuffer ++ buffer
      val rPosition = newBuffer.indexOf(rByte)

      if(rPosition > 0 && newBuffer(rPosition + 1) == nByte){
        val commandBytes = newBuffer.take(rPosition)
        val newStateBytes = newBuffer.drop(rPosition + 2)

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
        stay() using ConnectionState(newBuffer, data)
  }


  when(WaitingForData) {
    case Event(Received(buffer), ConnectionState(stateBuffer, command: CommandData)) =>
      val newBuffer = stateBuffer ++ buffer
      val commandBytesLength = command.data.bytes

      //Check if we have enought data to try parse bytes
      if(newBuffer.length >= commandBytesLength + 2) {
        if(newBuffer(commandBytesLength) == rByte && newBuffer(commandBytesLength + 1) == nByte) {
          val commandData = newBuffer.take(commandBytesLength)
          val connectionBuffer = newBuffer.drop(commandBytesLength + 2)
          //TODO:: send message to router
          //router ! (command, commandData)

          goto(WaitingForResponse) using ConnectionState(connectionBuffer, EmptyData)
        }
        else {
          //TODO:: send error to cliend
          val connectionBuffer = newBuffer.drop(commandBytesLength + 2)

          //TODO:: checn buffer for new command
          goto(WaitingForData) using ConnectionState(connectionBuffer, EmptyData)
        }
      }
      else
        stay() using ConnectionState(newBuffer, command)
  }

  when(WaitingForResponse) {
    case Event(x: Response, stateData) =>
      val response = x.toString + "\r\n"
      connection ! Tcp.Write(ByteString(response))
      goto(WaitingForCommand) using stateData
  }

  whenUnhandled {
    case Event(Received(data), x: ConnectionState) =>
      stay() using x.copy(buffer = x.buffer ++ data)
  }

  initialize()

}
