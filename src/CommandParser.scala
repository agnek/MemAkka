import scala.util.Try

object CommandParser {
  def parse(str: String): Option[Command] = {
    val tokens = str.split(' ')

    val commandTry = Try {
      tokens(0) match {
        case "set" => SetCommand(tokens(1), tokens(2).toInt, tokens(3).toLong, tokens(4).toInt)
        case "add" => AddCommand(tokens(1), tokens(2).toInt, tokens(3).toLong, tokens(4).toInt)
        case "replace" => ReplaceCommand(tokens(1), tokens(2).toInt, tokens(3).toLong, tokens(4).toInt)
        case "append" => AppendCommand(tokens(1), tokens(2).toInt, tokens(3).toLong, tokens(4).toInt)
        case "prepend" => PrependCommand(tokens(1), tokens(2).toInt, tokens(3).toLong, tokens(4).toInt)
        case "get" => GetCommand(tokens.tail)
        case "gets" => GetCommand(tokens.tail, withCas = true)
        case "cas" => CasCommand(tokens(1), tokens(2).toInt, tokens(3).toLong, tokens(4).toInt, tokens(5).toLong)
        case "delete" => DeleteCommand(tokens(1))
        case "incr" => IncrementCommand(tokens(1), tokens(2).toLong)
        case "decr" => DecrementCommand(tokens(1), tokens(2).toLong)
        case "quit" => QuitCommand
      }
    }

    commandTry.toOption
  }
}