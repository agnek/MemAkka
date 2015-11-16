import net.spy.memcached.MemcachedClient
import org.specs2._


class GetCommandSpec extends mutable.Specification with MemAkkaContext {
  "Get command" should {
    "return correct value" >> { client: MemcachedClient =>
      client.set("aa", 100, "1234")
      client.get("aa") must beEqualTo("1234")
    }

    "return null in case of non-existing key" >> { client: MemcachedClient =>
      client.get("bb") must beNull
    }

    "return correct values is asking two keys" >> { client: MemcachedClient =>
      client.set("a1", 100, "123").get() must beEqualTo(true)
      client.set("a2", 100, "234").get() must beEqualTo(true)

      val values = client.getBulk("a1", "a2")

      values.get("a1") must beEqualTo("123")
      values.get("a2") must beEqualTo("234")
    }

  }
}
