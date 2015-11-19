import net.spy.memcached.MemcachedClient
import org.specs2.mutable

class DecrementCommandSpec extends mutable.Specification with MemAkkaContext {
  "Decrement command" should {
    "correct work in good case" >> { client: MemcachedClient =>
      client.set("decr", 100, "200").get()
      client.decr("decr", 50) must beEqualTo(150)
    }

    "get error in case of incrementing non-existing key" >> { client: MemcachedClient =>
      client.decr("decr2", 10) must beEqualTo(-1)
    }

    "get error in case of incrementing non-number value" >> { client: MemcachedClient =>
      client.set("decr3", 100, "asdvasd").get()
      client.decr("decr3", 100) must beEqualTo(-1)
    }

    "get 0 when decrement value more than key value" >> { client: MemcachedClient =>
      client.set("decr4", 100, "200").get()
      client.decr("decr4", 250) must beEqualTo(0)
    }
  }
}
