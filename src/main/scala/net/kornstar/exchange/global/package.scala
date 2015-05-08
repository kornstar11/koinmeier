package net.kornstar.exchange
import com.typesafe.config.{ConfigFactory, Config}
/**
 * Created by Ben Kornmeier on 5/7/2015.
 */
package object global {
  val config = ConfigFactory.load()

}
