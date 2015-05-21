package net.kornstar.exchange.collection

import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by Ben Kornmeier on 5/6/2015.
 */
case class Order(userId:Int,id:Int,timeCreated:Long,isBid:Boolean,amount:Int,remainingAmount:Int,price:Double,settledPrice:Double = 0.0,timeSettled:Option[Long] = None){
  val settledAmount = amount - remainingAmount
  val isValid = amount > 0 && price > 0
  def hashcode:Int = id
}

object Order {
  val idMaker = new AtomicInteger(0)
  def apply(userId:Int,isBid:Boolean,amount:Int,price:Double):Order = {
    apply(userId,idMaker.getAndIncrement,isBid,amount,price)
  }
  def apply(userId:Int,id:Int,isBid:Boolean,amount:Int,price:Double):Order = {
    apply(userId,id,System.currentTimeMillis(),isBid,amount,price)
  }
  def apply(userId:Int,id:Int,timeCreated:Long,isBid:Boolean,amount:Int,price:Double):Order = {
    Order(userId,id,timeCreated,isBid,amount,amount,price)
  }
}
