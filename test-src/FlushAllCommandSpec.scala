import net.spy.memcached.MemcachedClient
import org.specs2.mutable

class FlushAllCommandSpec extends mutable.Specification with MemAkkaContext {
  "Flush all command" should {
    "delete everything" >> { client: MemcachedClient =>
      client.set("aa", 100, "bbbb").get()

      client.flush().get()
      
      client.get("aa") must beNull
    }
  }

}
