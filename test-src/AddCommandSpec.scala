import net.spy.memcached.MemcachedClient
import org.specs2.mutable

class AddCommandSpec extends mutable.Specification with MemAkkaContext {
  "Add command" should {
    "correct create new item" >> { client: MemcachedClient =>
      client.add("add", 100, "ttt").get()
      client.get("add") must beEqualTo("ttt")
    }

    "get error in case of creating existing item" >> { client: MemcachedClient =>
      client.add("add2", 100, "ttt").get()
      client.get("add2") must beEqualTo("ttt")

      client.add("add2", 100, "aaa").get() must beEqualTo(false)
      client.get("add2") must beEqualTo("ttt")
    }


  }
}
