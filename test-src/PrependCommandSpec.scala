import net.spy.memcached.MemcachedClient

import org.specs2.mutable

class PrependCommandSpec extends mutable.Specification with MemAkkaContext {
  "Prepend command" should {
    "get error when storing data for non-existing key" >> { client: MemcachedClient =>
      client.prepend("prep", "aaa").get() must beEqualTo(false)
    }

    "correct update existing key data" >> { client: MemcachedClient =>
      client.set("prep", 100, "data")
      client.prepend("prep", "test").get() must beEqualTo(true)
      client.get("prep") must beEqualTo("testdata")
    }

  }

}
