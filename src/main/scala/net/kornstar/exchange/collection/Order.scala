package net.kornstar.exchange.collection

/**
 * Created by Ben Kornmeier on 5/6/2015.
 */
case class Order(id:Int,timeCreated:Long,isBid:Boolean,amount:Int,remainingAmount:Int,price:Double,settledPrice:Double = 0.0,timeSettled:Option[Long] = None){
  def hashcode:Int = id
}

object Order {
  def apply(id:Int,timeCreated:Long,isBid:Boolean,amount:Int,price:Double):Order = {
    Order(id,timeCreated,isBid,amount,amount,price)
  }
}
