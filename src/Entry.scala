import akka.actor._
import akka.util.ByteString
import Entry._
import scala.util.{Success, Try}
import akka.cluster.sharding._
import scala.concurrent.duration._

object Entry {
  def props() = Props(new Entry)

  sealed trait EntryData
  case class InitializedData(key: String, flags: Int, cas: Long, data: ByteString) extends EntryData
  case object UninitializedData extends EntryData
  
  sealed trait EntryState
  case object Uninitialized extends EntryState
  case object Initialized extends EntryState

  val idExtractor: ShardRegion.ExtractEntityId = {
    case command: KeyCommand => (command.key, command)
    case (command: KeyCommand, data) => (command.key, (command, data))
  }

  val shardResolver: ShardRegion.ExtractShardId = {
    case command: KeyCommand => (Math.abs(command.key.hashCode) % 1024).toString
    case (command: KeyCommand, data) => (Math.abs(command.key.hashCode) % 1024).toString
  }
}


class Entry extends Actor with FSM[EntryState, EntryData] {
  startWith(Uninitialized, UninitializedData)

  when(Uninitialized, 5 seconds) {
    case Event((x: SetCommand, bytes: ByteString), _) =>
      val newState = InitializedData(x.key, x.flags, 1l, bytes)
      if(!x.noreply) sender() ! Stored
      goto(Initialized) forMax x.duration using newState

    case Event((x: AddCommand, bytes: ByteString), _) =>
      val newState = InitializedData(x.key, x.flags, 1l, bytes)
      if(!x.noreply) sender() ! Stored
      goto(Initialized) forMax x.duration using newState

    case Event(x: GetInternalCommand, _) =>
      sender() ! NotFound
      stay()

    case Event((x: ReplaceCommand, _), _) =>
      if(!x.noreply) sender() ! NotStored
      stay()

    case Event((x: AppendCommand, _), _) =>
      if(!x.noreply) sender() ! NotStored
      stay()

    case Event((x: CasCommand, _), _) =>
      if(!x.noreply) sender() ! NotFound
      stay()

    case Event((x: PrependCommand, _), _) =>
      if(!x.noreply) sender() ! NotStored
      stay()

    case Event(x: DeleteCommand, _) =>
      if(!x.noreply) sender() ! NotFound
      stay()

    case Event(x: IncrementCommand, _) =>
      if(!x.noreply) sender() ! NotFound
      stay()

    case Event(x: DecrementCommand, _) =>
      if(!x.noreply) sender() ! NotFound
      stay()

    case Event(x: TouchCommand, _) =>
      if(!x.noreply) sender() ! NotFound
      stay()
  }

  when(Initialized) {
    case Event(x: GetInternalCommand, state: InitializedData) =>
      sender() ! Value(state.key, state.data, state.flags, if(x.withCas) Some(state.cas) else None)
      stay()

    case Event(x: TouchCommand, _) =>
      if(!x.noreply) sender() ! Touched
      stay() forMax x.duration

    case Event((x: SetCommand, bytes: ByteString), oldState: InitializedData) =>
      val newState = InitializedData(x.key, x.flags, oldState.cas + 1, bytes)
      if(!x.noreply) sender() ! Stored
      stay forMax x.duration using newState

    case Event((x: AddCommand, _), _) =>
      if(!x.noreply) sender() ! NotStored
      stay()

    case Event((x: ReplaceCommand, bytes: ByteString), state: InitializedData) =>
      if(!x.noreply) sender() ! Stored
      stay() forMax x.duration using state.copy(data = bytes, cas = state.cas + 1, flags = x.flags)

    case Event((x: AppendCommand, bytes: ByteString), state: InitializedData) =>
      if(!x.noreply) sender() ! Stored
      stay() using state.copy(data = state.data ++ bytes, cas = state.cas + 1)

    case Event((x: PrependCommand, bytes: ByteString), state: InitializedData) =>
      if(!x.noreply) sender() ! Stored
      stay() using state.copy(data = bytes ++ state.data, cas = state.cas + 1)

    case Event(x: DeleteCommand, _) =>
      if(!x.noreply) sender() ! Deleted
      stop()

    case Event((x: CasCommand, bytes: ByteString), state: InitializedData) =>
      if(state.cas == x.cas) {
        if(!x.noreply) sender() ! Stored
        stay() forMax x.duration using state.copy(data = bytes, cas = state.cas + 1)
      }
      else {
        if(!x.noreply) sender() ! Exists
        stay()
      }

    case Event(x: IncrementCommand, state: InitializedData) =>
      val valueTry = Try { state.data.utf8String.toLong }

      valueTry match {
        case Success(long) =>
          val newValue = long + x.value
          val newBufferValue = ByteString.fromString(newValue.toString)
          if(!x.noreply) sender() ! OnlyValue(newBufferValue)
          stay() using state.copy(data = newBufferValue, cas = state.cas + 1)

        case util.Failure(_) =>
          if(!x.noreply) sender() ! ServerError("Cannot parse value as long")
          stay()
      }

    case Event(x: DecrementCommand, state: InitializedData) =>
      val valueTry = Try { state.data.utf8String.toLong }

      valueTry match {
        case Success(long) =>
          val newValue = if(long > x.value) long - x.value else 0
          val newBufferValue = ByteString.fromString(newValue.toString)
          if(!x.noreply) sender() ! OnlyValue(newBufferValue)
          stay() using state.copy(data = newBufferValue, cas = state.cas + 1)

        case util.Failure(_) =>
          if(!x.noreply) sender() ! ServerError("Cannot parse value as long")
          stay()
      }
  }

  whenUnhandled {
    case Event(StateTimeout, _) =>
      stop(FSM.Normal)

    case _ =>
      sender() ! Error
      stay()
  }

  initialize()
}
