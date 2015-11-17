import net.spy.memcached.MemcachedClient
import org.specs2._

class AppendCommandSpec extends mutable.Specification with MemAkkaContext {
  "Append command" should {
    "return false in case appending for nonexisting key" >> { client: MemcachedClient =>
      client.append("tappend", "asd").get() must beEqualTo(false)
    }

    "correct work when appending to existing key" >> { client: MemcachedClient =>
      client.set("tappend", 100, "test").get()

      client.append("tappend", "test").get() must beEqualTo(true)
      client.get("tappend") must beEqualTo("testtest")
    }

  }


}
