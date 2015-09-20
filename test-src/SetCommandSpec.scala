import net.spy.memcached.MemcachedClient
import org.specs2._

class SetCommandSpec extends mutable.Specification with MemAkkaContext {

  "Set command" should {
    "set value to non-existing key" >> { client: MemcachedClient =>
      client.set("testkey", 100, "123")
      val value = client.get("testkey")
      value must beEqualTo("123")
    }

    "correct update value of existing key" >> { client: MemcachedClient =>
      client.set("testkey", 100, "123")
      val value = client.get("testkey")
      value must beEqualTo("123")

      client.set("testkey", 100, "234")
      val newValue = client.get("testkey")
      newValue must beEqualTo("234")
    }

  }
}