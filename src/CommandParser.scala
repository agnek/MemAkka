import scala.util.Try

object CommandParser {
  private val spaceByte = ' '.toByte

  def parse(str: String): Option[Command] = {
    val tokens = str.split(' ')

    val commandTry = Try {
      tokens(0) match {
        case "set" => SetCommand(tokens(1), tokens(2).toInt, tokens(3).toLong, tokens(4).toInt, Array.empty)
        case "add" => AddCommand(tokens(1), tokens(2).toInt, tokens(3).toLong, tokens(4).toInt, Array.empty)
        case "replace" => ReplaceCommand(tokens(1), tokens(2).toInt, tokens(3).toLong, tokens(4).toInt, Array.empty)
        case "append" => AppendCommand(tokens(1), tokens(2).toInt, tokens(3).toLong, tokens(4).toInt, Array.empty)
        case "prepend" => PrependCommand(tokens(1), tokens(2).toInt, tokens(3).toLong, tokens(4).toInt, Array.empty)
      }
    }

    commandTry.toOption
  }

}