import net.spy.memcached.MemcachedClient

import org.specs2.mutable

class PrependCommandSpec extends mutable.Specification with MemAkkaContext {
  "Prepend command" should {
    "get error when storing data for non-existing key" >> { client: MemcachedClient =>
      client.prepend("prepend", "aaa").get() must beEqualTo(false)
    }

    "correct update existing key data" >> { client: MemcachedClient =>
      client.set("prepend2", 100, "data")
      client.prepend("prepend2", "test").get() must beEqualTo(true)
      client.get("prepend2") must beEqualTo("testdata")
    }

  }

}
