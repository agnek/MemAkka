import java.net.InetSocketAddress
import java.util.Properties
import java.util.logging.{Level, Logger}

import net.spy.memcached.MemcachedClient
import org.specs2.execute.{Result, AsResult}
import org.specs2.specification.ForEach

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait MemAkkaContext extends ForEach[MemcachedClient] {

  def foreach[R: AsResult](f: (MemcachedClient) => R): Result = {
    val (port, memAkka) = MemAkkaFactory.createSystem()

    Logger.getLogger("net.spy.memcached").setLevel(Level.WARNING)
    val systemProperties = System.getProperties
    systemProperties.put("net.spy.log.LoggerImpl", "net.spy.memcached.compat.log.SunLogger")
    System.setProperties(systemProperties)


    val memcachedClient = new MemcachedClient(new InetSocketAddress("localhost", port))

    println(s"Started actor system on port $port")

    try
      AsResult(f(memcachedClient))
    finally {
      Await.result(memAkka.terminate(), Duration.Inf)
      println(s"Stopped actor system on port $port")
    }

  }
}
