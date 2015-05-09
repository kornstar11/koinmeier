package net.kornstar.exchange.collection

import org.specs2.mutable.Specification
/**
 * Created by Ben Kornmeier on 5/8/2015.
 */
class BankTest extends Specification{
  sequential

  "MemoryBank" should {
    "correctly handle deposits" in {
      val bank1 = Bank().depositBaseCurrency(1,10.0).depositOtherCurrency(2,10)

      bank1.getAccountFor(1).foreach(_.baseCurrencyAmount must be equalTo(10.0))
      bank1.getAccountFor(2).foreach(_.otherCurrencyAmount must be equalTo(10.0))



      success
    }

    "correctly handle orders" in {
      val bank1 = Bank().depositBaseCurrency(1,10.0).depositOtherCurrency(2,10)

      val orderFor1 = Order(1,0,10101L,true,10,0,1.0,settledPrice = 1.0)
      val orderFor2 = Order(2,0,10101L,false,10,0,1.0,settledPrice = 1.0)

      val bank2 = bank1.settleOrders(orderFor1,orderFor2)

      val acct1 = bank2.getAccountFor(1).get
      val acct2 = bank2.getAccountFor(2).get

      acct1.otherCurrencyAmount mustEqual(10.0)
      acct2.baseCurrencyAmount mustEqual(10.0)

      acct1.getLedger.count(_.orderOpt.map(_ == orderFor1).getOrElse(false)) mustEqual(1)
      acct2.getLedger.count(_.orderOpt.map(_ == orderFor2).getOrElse(false)) mustEqual(1)



      success
    }
  }

}
