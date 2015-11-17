import net.spy.memcached.MemcachedClient
import org.specs2._

class GetsCommandSpec extends mutable.Specification with MemAkkaContext {
  "Gets command" should {
    "return value with cas" >> { client: MemcachedClient =>
      client.set("castest", 100, "123").get()

      client.gets("castest").getValue must beEqualTo("123")
      client.gets("castest").getCas should beGreaterThan(0l)
    }

  }
}
