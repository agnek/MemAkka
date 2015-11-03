import net.spy.memcached.MemcachedClient
import org.specs2.mutable

class DeleteCommandSpec extends mutable.Specification with MemAkkaContext {
  "Delete command" should {
    "correct delete existing key" >> { client: MemcachedClient =>
      client.set("deletetest", 100, "asdas")
      client.delete("deletetest").get() must beEqualTo(true)

      client.get("deletetest") must beNull

    }

    "just work in case deliting non-existing key" >> { client: MemcachedClient =>
      client.delete("aaa").get() must beEqualTo(false)
    }

  }
}
