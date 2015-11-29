import scala.util.Try

object CommandParser {
  def parse(str: String): Option[Command] = {
    Try { parseUnsafe(str) }.toOption
  }

  def parseUnsafe(str: String): Command = {
    val tokens = str.split(' ')

    def hasNoreply(num: Int): Boolean = {
      tokens.length == (num + 1) && tokens(num) == "noreply"
    }

    tokens(0) match {
      case "set" => SetCommand(tokens(1), tokens(2).toInt, tokens(3).toLong, tokens(4).toInt, hasNoreply(5))
      case "add" => AddCommand(tokens(1), tokens(2).toInt, tokens(3).toLong, tokens(4).toInt, hasNoreply(5))
      case "replace" => ReplaceCommand(tokens(1), tokens(2).toInt, tokens(3).toLong, tokens(4).toInt, hasNoreply(5))
      case "append" => AppendCommand(tokens(1), tokens(2).toInt, tokens(3).toLong, tokens(4).toInt, hasNoreply(5))
      case "prepend" => PrependCommand(tokens(1), tokens(2).toInt, tokens(3).toLong, tokens(4).toInt, hasNoreply(5))
      case "get" => GetCommand(tokens.tail)
      case "gets" => GetCommand(tokens.tail, withCas = true)
      case "cas" => CasCommand(tokens(1), tokens(2).toInt, tokens(3).toLong, tokens(4).toInt, tokens(5).toLong, hasNoreply(6))
      case "delete" => DeleteCommand(tokens(1), hasNoreply(2))
      case "incr" => IncrementCommand(tokens(1), tokens(2).toLong, hasNoreply(3))
      case "decr" => DecrementCommand(tokens(1), tokens(2).toLong, hasNoreply(3))
      case "quit" => QuitCommand
      case "flush_all" => FlushAllCommand
    }
  }

}