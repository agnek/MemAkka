import net.spy.memcached.MemcachedClient
import org.specs2.mutable

class AddCommandSpec extends mutable.Specification with MemAkkaContext {
  "Add command" should {
    "correct create new item" >> { client: MemcachedClient =>
      client.add("test", 100, "ttt")
      client.get("test") must beEqualTo("ttt")
    }

    "get error in case of creating existing item" >> { client: MemcachedClient =>
      client.add("test", 100, "ttt")
      client.get("test") must beEqualTo("ttt")

      client.add("test", 100, "aaa").get() must beEqualTo(false)
      client.get("test") must beEqualTo("ttt")
    }


  }
}
