import akka.util.ByteString


sealed trait Command
sealed trait BytesCommand {
  def bytes: Int
}


case class CasCommand(key: String, flags: Int, exptime: Long, bytes: Int, cas: String) extends Command
case class SetCommand(key: String, flags: Int, exptime: Long, bytes: Int) extends Command with BytesCommand
case class AddCommand(key: String, flags: Int, exptime: Long, bytes: Int) extends Command with BytesCommand
case class ReplaceCommand(key: String, flags: Int, exptime: Long, bytes: Int) extends Command with BytesCommand
case class AppendCommand(key: String, flags: Int, exptime: Long, bytes: Int) extends Command with BytesCommand
case class PrependCommand(key: String, flags: Int, exptime: Long, bytes: Int) extends Command with BytesCommand
case class DeleteCommand(key: String) extends Command

case class IncrementCommand(key: String, value: Long) extends Command {
  require(value > 0, "invalid numeric delta argument")
}

case class DecrementCommand(key: String, value: Long) extends Command {
  require(value > 0, "invalid numeric delta argument")
}

sealed trait RetrieveCommand extends Command
case class GetCommand(keys: Seq[String]) extends RetrieveCommand
case class GetsCommand(keys: Seq[String]) extends RetrieveCommand

case object QuitCommand extends Command

sealed trait Response {
  def toByteString: ByteString
}

case object Stored extends Response {
  val toByteString = ByteString("STORED\r\n")
}

case object NotStored extends Response {
  val toByteString = ByteString("NOT_STORED\r\n")
}

case object Exists extends Response {
  val toByteString = ByteString("EXISTS\r\n")
}

case object NotFound extends Response {
  val toByteString = ByteString("NOT_FOUND\r\n")
}

case class Value(key: String, value: ByteString, flags: Int, cas: Option[Long]) extends Response {
  val toByteString =
    ByteString(s"VALUE $key $flags ${value.length} ${cas.getOrElse("")}\r\n") ++ value ++ ByteString("\r\n")
}

case class Values(values: Seq[Value]) extends Response {
  val toByteString = values.foldLeft(ByteString.empty)(_ ++ _.toByteString) ++ ByteString("END\r\n")
}

case class OnlyValue(value: ByteString) extends Response {
  val toByteString = value ++ ByteString("\r\n")
}

case object End extends Response {
  val toByteString = ByteString("END\r\n")
}

case object Deleted extends Response {
  val toByteString = ByteString("DELETED\r\n")
}

case class IncrResponse(newValue: Long) extends Response {
  val toByteString = ByteString(s"$newValue\r\n")
}

case object Touched extends Response {
  val toByteString = ByteString("TOUCHED\r\n")
}

case object Error extends Response {
  val toByteString = ByteString("ERROR\r\n")
}

case class ClientError(message: String) extends Response {
  val toByteString = ByteString(s"CLIENT_ERROR $message\r\n")
}

case class ServerError(message: String) extends Response {
  val toByteString = ByteString(s"SERVER_ERROR $message\r\n")
}