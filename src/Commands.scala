import akka.util.ByteString

sealed trait Command

sealed trait BytesCommand {
  def bytes: Int
}

sealed trait TimeoutCommand {
  import scala.concurrent.duration._
  def exptime: Long

  def duration: Duration = {
    if(exptime == 0) Duration.Inf
    else if(exptime > System.currentTimeMillis() / 1000) (exptime - System.currentTimeMillis() / 1000) seconds
    else exptime seconds
  }
}

sealed trait KeyCommand {
  def key: String
}

case class CasCommand(key: String, flags: Int, exptime: Long, bytes: Int, cas: Long, noreply: Boolean) extends Command
  with BytesCommand with TimeoutCommand with KeyCommand

case class SetCommand(key: String, flags: Int, exptime: Long, bytes: Int, noreply: Boolean) extends Command
  with BytesCommand with TimeoutCommand with KeyCommand

case class AddCommand(key: String, flags: Int, exptime: Long, bytes: Int, noreply: Boolean) extends Command
  with BytesCommand with TimeoutCommand with KeyCommand

case class ReplaceCommand(key: String, flags: Int, exptime: Long, bytes: Int, noreply: Boolean) extends Command
  with BytesCommand with TimeoutCommand with KeyCommand

case class AppendCommand(key: String, flags: Int, exptime: Long, bytes: Int, noreply: Boolean) extends Command
  with BytesCommand with KeyCommand

case class PrependCommand(key: String, flags: Int, exptime: Long, bytes: Int, noreply: Boolean) extends Command
  with BytesCommand with KeyCommand

case class DeleteCommand(key: String, noreply: Boolean) extends Command with KeyCommand

case class TouchCommand(key: String, exptime: Long, noreply: Boolean) extends Command with TimeoutCommand with KeyCommand

case object FlushAllCommand extends Command


case class IncrementCommand(key: String, value: Long, noreply: Boolean) extends Command with KeyCommand {
  require(value > 0, "invalid numeric delta argument")
}

case class DecrementCommand(key: String, value: Long, noreply: Boolean) extends Command with KeyCommand {
  require(value > 0, "invalid numeric delta argument")
}

case class GetCommand(keys: Seq[String], withCas: Boolean = false) extends Command

case class GetsCommand(keys: Seq[String]) extends Command

case class GetInternalCommand(key: String, withCas: Boolean = false) extends  KeyCommand

case object QuitCommand extends Command

sealed trait Response {
  def asByteString: ByteString
}

case object Stored extends Response {
  val asByteString = ByteString("STORED\r\n")
}

case object NotStored extends Response {
  val asByteString = ByteString("NOT_STORED\r\n")
}

case object Exists extends Response {
  val asByteString = ByteString("EXISTS\r\n")
}

case object NotFound extends Response {
  val asByteString = ByteString("NOT_FOUND\r\n")
}

case class Value(key: String, value: ByteString, flags: Int, cas: Option[Long]) extends Response {
  val asByteString =
    ByteString(s"VALUE $key $flags ${value.length} ${cas.getOrElse("")}\r\n") ++ value ++ ByteString("\r\n")
}

case class OnlyValue(value: ByteString) extends Response {
  val asByteString = value ++ ByteString("\r\n")
}

case object Ok extends Response {
  val asByteString = ByteString("OK\r\n")
}

case object End extends Response {
  val asByteString = ByteString("END\r\n")
}

case object Deleted extends Response {
  val asByteString = ByteString("DELETED\r\n")
}

case class IncrResponse(newValue: Long) extends Response {
  val asByteString = ByteString(s"$newValue\r\n")
}

case object Touched extends Response {
  val asByteString = ByteString("TOUCHED\r\n")
}

case object Error extends Response {
  val asByteString = ByteString("ERROR\r\n")
}

case class ClientError(message: String) extends Response {
  val asByteString = ByteString(s"CLIENT_ERROR $message\r\n")
}

case class ServerError(message: String) extends Response {
  val asByteString = ByteString(s"SERVER_ERROR $message\r\n")
}