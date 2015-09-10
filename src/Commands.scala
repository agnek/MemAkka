
sealed trait Command
sealed trait StoreCommand extends Command
sealed trait DataCommand {
  def bytes: Int
}

case class CasCommand(key: String, flags: Int, exptime: Long, bytes: Int, cas: String, data: Array[Byte]) extends StoreCommand
case class SetCommand(key: String, flags: Int, exptime: Long, bytes: Int, data: Array[Byte]) extends StoreCommand with DataCommand
case class AddCommand(key: String, flags: Int, exptime: Long, bytes: Int, data: Array[Byte]) extends StoreCommand with DataCommand
case class ReplaceCommand(key: String, flags: Int, exptime: Long, bytes: Int, data: Array[Byte]) extends StoreCommand with DataCommand
case class AppendCommand(key: String, flags: Int, exptime: Long, bytes: Int, data: Array[Byte]) extends StoreCommand with DataCommand
case class PrependCommand(key: String, flags: Int, exptime: Long, bytes: Int, data: Array[Byte]) extends StoreCommand with DataCommand

sealed trait RetrieveCommand extends Command
case class GetCommand(key: String) extends RetrieveCommand
case class GetsCommand(keys: Seq[String]) extends RetrieveCommand

sealed trait Response
case class Stored() extends Response
case class NotStored() extends Response
case class Exists() extends Response
case class NotFound() extends Response
case class Value(key: String, value: Option[String]) extends Response