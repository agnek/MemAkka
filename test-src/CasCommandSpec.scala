import net.spy.memcached.MemcachedClient
import org.specs2.mutable

class CasCommandSpec extends mutable.Specification with MemAkkaContext {
  "Cas command" should {
    "correct update value" >> { client: MemcachedClient =>
      client.set("cas1", 100, "123").get()
      val result = client.gets("cas1")
      client.cas("cas1", result.getCas, "234")

      val newResult = client.gets("cas1")
      newResult.getCas should beGreaterThan(result.getCas)
      newResult.getValue should beEqualTo("234")
    }

    "dont update value if cas changed" >> { client: MemcachedClient =>
      client.set("cas2", 100, "123").get()
      val result = client.gets("cas2")

      client.incr("cas2", 5)
      client.cas("cas2", result.getCas, "234")
      client.get("cas2") must beEqualTo("128")
    }

  }
}
