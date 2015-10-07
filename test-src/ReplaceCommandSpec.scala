import net.spy.memcached.MemcachedClient
import org.specs2.mutable

class ReplaceCommandSpec extends mutable.Specification with MemAkkaContext {
  "Replace command" should {
    "get error when storing data for non-existing key" >> { client: MemcachedClient =>
      val result = client.replace("rep", 0, "test")
      result.get() must beEqualTo(false)
    }

    "correct update existing key data" >> { client: MemcachedClient =>
      client.set("rep2", 0, "data")
      client.replace("rep2", 0, "data2").get() must beEqualTo(true)

      client.get("rep2") must beEqualTo("data2")
    }

  }

}
