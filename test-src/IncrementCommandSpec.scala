import net.spy.memcached.MemcachedClient
import org.specs2.mutable

class IncrementCommandSpec extends mutable.Specification with MemAkkaContext  {
  "Increment command" should {
    "correct work in good case" >> { client: MemcachedClient =>
      client.set("test", 100, "200").get()
      client.incr("test", 50) must beEqualTo(250)
    }

    "get error in case of incrementing non-existing key" >> { client: MemcachedClient =>
      client.incr("asdfasdf", 10) must beEqualTo(-1)
    }

    "get error in case of incrementing non-number value" >> { client: MemcachedClient =>
      client.set("test2", 100, "asdvasd").get()
      client.incr("test2", 100) must beEqualTo(-1)
    }
  }
}
