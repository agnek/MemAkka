import akka.actor._
import akka.util.ByteString
import Entry._

object Entry {
  def props(key: String) = Props(new Entry(key))

  sealed trait EntryData
  case class InitializedData(key: String, flags: Int, exptime: Long, cas: Long, data: ByteString, timeoutTask: Option[Cancellable]) extends EntryData
  case object UninitializedData extends EntryData
  
  sealed trait EntryState
  case object Uninitialized extends EntryState
  case object Initialized extends EntryState

  case object TimeoutCommand

}


class Entry(key: String) extends Actor with FSM[EntryState, EntryData] {
  println(s"Started actor for key: $key")

  startWith(Uninitialized, UninitializedData)

  when(Uninitialized) {
    case Event((x: SetCommand, bytes: ByteString), _) =>
      val newState = InitializedData(x.key, x.flags, x.exptime, 1l, bytes, None)
      sender() ! Stored
      goto(Initialized) using newState

    case Event((x: AddCommand, bytes: ByteString), _) =>
      val newState = InitializedData(x.key, x.flags, x.exptime, 1l, bytes, None)
      sender() ! Stored
      goto(Initialized) using newState

    case _ =>
      sender() ! Error
      stay()
  }

  when(Initialized) {
    case Event(x: GetCommand, state: InitializedData) =>
      sender() ! Value(state.key, state.data, state.flags, None)
      stay()

    case Event((x: SetCommand, bytes: ByteString), _) =>
      val newState = InitializedData(x.key, x.flags, x.exptime, 1l, bytes, None)
      sender() ! Stored
      stay using newState

    case Event((x: AddCommand, _), _) =>
      sender() ! NotStored
      stay()

    case Event((x: ReplaceCommand, bytes: ByteString), state: InitializedData) =>
      sender() ! Stored
      //TODO:: add updating flags and timeout
      stay() using state.copy(data = bytes)

    case Event((x: AppendCommand, bytes: ByteString), state: InitializedData) =>
      sender() ! Stored
      //TODO:: add updating flags and timeout
      stay() using state.copy(data = state.data ++ bytes)

    case Event((x: PrependCommand, bytes: ByteString), state: InitializedData) =>
      sender() ! Stored
      //TODO:: add updating flags and timeout
      stay() using state.copy(data = bytes ++ state.data)
  }

  whenUnhandled {
    case Event(TimeoutCommand, _) =>
      self ! Kill
      stay()
  }

  initialize()
}
