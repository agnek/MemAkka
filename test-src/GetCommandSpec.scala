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

  }
}
