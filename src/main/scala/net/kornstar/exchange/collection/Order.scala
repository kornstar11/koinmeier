package net.kornstar.exchange.collection

/**
 * Created by Ben Kornmeier on 5/6/2015.
 */
case class Order(userId:Int,id:Int,timeCreated:Long,isBid:Boolean,amount:Int,remainingAmount:Int,price:Double,settledPrice:Double = 0.0,timeSettled:Option[Long] = None){
  val settledAmount = amount - remainingAmount
  def hashcode:Int = id
}

object Order {
  def apply(userId:Int,id:Int,timeCreated:Long,isBid:Boolean,amount:Int,price:Double):Order = {
    Order(userId,id,timeCreated,isBid,amount,amount,price)
  }
}
