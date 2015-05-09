package net.kornstar.exchange.collection

import org.specs2.mutable.Specification
/**
 * Created by Ben Kornmeier on 5/8/2015.
 */
class BankTest extends Specification{
  "MemoryBank" should {
    "correctly file a transaction" in {
      val bank1 = Bank().depositBaseCurrency(1,10.0).depositOtherCurrency(2,10)

      val orderFor1 = Order(1,0,10101L,true,10,1.0)
      val orderFor2 = Order(2,0,10101L,false,10,1.0)

      bank1.getAccountFor(1).foreach(_.baseCurrencyAmount must be equalTo(10.0))
      bank1.getAccountFor(2).foreach(_.otherCurrencyAmount must be equalTo(10.0))



      success
    }
  }

}
