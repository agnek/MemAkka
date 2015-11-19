import net.spy.memcached.MemcachedClient
import org.specs2.mutable

class IncrementCommandSpec extends mutable.Specification with MemAkkaContext  {
  "Increment command" should {
    "correct work in good case" >> { client: MemcachedClient =>
      client.set("incr", 100, "200").get()
      client.incr("incr", 50) must beEqualTo(250)
    }

    "get error in case of incrementing non-existing key" >> { client: MemcachedClient =>
      client.incr("incr2", 10) must beEqualTo(-1)
    }

    "get error in case of incrementing non-number value" >> { client: MemcachedClient =>
      client.set("incr3", 100, "asdvasd").get()
      client.incr("incr3", 100) must beEqualTo(-1)
    }
  }
}
