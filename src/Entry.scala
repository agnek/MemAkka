import akka.actor._
import akka.util.ByteString
import Entry._
import scala.concurrent.duration._
import scala.util.{Success, Try}

object Entry {
  def props(key: String) = Props(new Entry(key))

  sealed trait EntryData
  case class InitializedData(key: String, flags: Int, cas: Long, data: ByteString) extends EntryData
  case object UninitializedData extends EntryData
  
  sealed trait EntryState
  case object Uninitialized extends EntryState
  case object Initialized extends EntryState
}


class Entry(key: String) extends Actor with FSM[EntryState, EntryData] {
  startWith(Uninitialized, UninitializedData)

  when(Uninitialized) {
    case Event((x: SetCommand, bytes: ByteString), _) =>
      val newState = InitializedData(x.key, x.flags, 1l, bytes)
      sender() ! Stored
      goto(Initialized) forMax x.duration using newState

    case Event((x: AddCommand, bytes: ByteString), _) =>
      val newState = InitializedData(x.key, x.flags, 1l, bytes)
      sender() ! Stored
      goto(Initialized) forMax x.duration using newState

    case _ =>
      sender() ! Error
      stay()
  }

  when(Initialized) {
    case Event(x: GetCommand, state: InitializedData) =>
      sender() ! Value(state.key, state.data, state.flags, if(x.withCas) Some(state.cas) else None)
      stay()

    case Event((x: SetCommand, bytes: ByteString), oldState: InitializedData) =>
      val newState = InitializedData(x.key, x.flags, oldState.cas + 1, bytes)
      sender() ! Stored
      stay forMax x.duration using newState

    case Event((x: AddCommand, _), _) =>
      sender() ! NotStored
      stay()

    case Event((x: ReplaceCommand, bytes: ByteString), state: InitializedData) =>
      sender() ! Stored
      stay() forMax x.duration using state.copy(data = bytes, cas = state.cas + 1, flags = x.flags)

    case Event((x: AppendCommand, bytes: ByteString), state: InitializedData) =>
      sender() ! Stored
      stay() using state.copy(data = state.data ++ bytes, cas = state.cas + 1)

    case Event((x: PrependCommand, bytes: ByteString), state: InitializedData) =>
      sender() ! Stored
      stay() using state.copy(data = bytes ++ state.data, cas = state.cas + 1)

    case Event((x: CasCommand, bytes: ByteString), state: InitializedData) =>
      if(state.cas == x.cas) {
        sender() ! Stored
        stay() forMax x.duration using state.copy(data = bytes, cas = state.cas + 1)
      }
      else {
        sender() ! Exists
        stay()
      }

    case Event(x: IncrementCommand, state: InitializedData) =>
      val valueTry = Try { state.data.utf8String.toLong }

      valueTry match {
        case Success(long) =>
          val newValue = long + x.value
          val newBufferValue = ByteString.fromString(newValue.toString)
          sender() ! OnlyValue(newBufferValue)
          stay() using state.copy(data = newBufferValue, cas = state.cas + 1)

        case util.Failure(_) =>
          sender() ! ServerError("Cannot parse value as long")
          stay()
      }

    case Event(x: DecrementCommand, state: InitializedData) =>
      val valueTry = Try { state.data.utf8String.toLong }

      valueTry match {
        case Success(long) =>
          val newValue = if(long > x.value) long - x.value else 0
          val newBufferValue = ByteString.fromString(newValue.toString)
          sender() ! OnlyValue(newBufferValue)
          stay() using state.copy(data = newBufferValue, cas = state.cas + 1)

        case util.Failure(_) =>
          sender() ! ServerError("Cannot parse value as long")
          stay()
      }
  }

  whenUnhandled {
    case Event(StateTimeout, _) =>
      stop(FSM.Normal)
  }

  initialize()
}
