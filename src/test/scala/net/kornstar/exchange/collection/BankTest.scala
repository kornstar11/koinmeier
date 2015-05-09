package net.kornstar.exchange.collection

import net.kornstar.exchange.collection.Transaction.TransactionType
import org.specs2.mutable.Specification

import scala.util.Try

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

      println(acct1.getLedger)
      println(acct2.getLedger)

      success
    }

    "creates a new order" in {
      val bank1 = Bank().depositBaseCurrency(1,10.0)

      val newOrderFail1 = Try {bank1.createOrder(1,11,1,true)}

      newOrderFail1.isFailure must be equalTo(true)

      println(newOrderFail1)

      val (bank2,order1) = bank1.createOrder(1,10,1,true)

      order1.amount mustEqual(1)
      order1.price mustEqual(10.0)
      order1.isBid mustEqual(true)

      val newAcct = bank2.getAccountFor(1).get
      newAcct.baseCurrencyAmount mustEqual(0.0)
      newAcct.otherCurrencyAmount mustEqual(0.0)

      val newLedger = newAcct.getLedger
      newLedger.size mustEqual(2)

      newLedger.count(_.transactionType == TransactionType.Buy) mustEqual(1)

      println(s"LEDGER: ${newLedger}")






      success
    }
  }

}
