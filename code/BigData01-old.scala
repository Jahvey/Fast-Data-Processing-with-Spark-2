import org.apache.spark.SparkContext
//import org.apache.spark.SparkContext._ // for implicit conversations
//import org.apache.spark.sql._

object BigData01 {
  // register case class external to main
  case class Order(OrderID : String, CustomerID : String, EmployeeID : String,
    OrderDate : String, ShipCountry : String)
    //
  case class OrderDetails(OrderID : String, ProductID : String, UnitPrice : Float,
    Qty : Int, Discount : Float)
    //
  def main(args: Array[String]): Unit = {
    val sc = new SparkContext("local","Chapter 8")
    println(s"Running Spark Version ${sc.version}")
    //
    val sqlContext = new org.apache.spark.sql.SQLContext(sc)
    // import sqlContext.createSchemaRDD // to implicitly convert an RDD to a SchemaRDD.
    import sqlContext._
    import sqlContext.implicits._
    //
    val ordersFile = sc.textFile("/Volumes/sdxc-01/fdps-vii/data/NW-Orders-NoHdr.csv")
    println("Orders File has %d Lines.".format(ordersFile.count()))
    val orders = ordersFile.map(_.split(",")).
      map(e => Order( e(0), e(1), e(2),e(3), e(4) ))
     println(orders.count)
     orders.toDF().registerTempTable("Orders")
     var result = sqlContext.sql("SELECT * from Orders")
     result.take(10).foreach(println)
     //
    val orderDetFile = sc.textFile("/Volumes/sdxc-01/fdps-vii/data/NW-Order-Details-NoHdr.csv")
    println("Order Details File has %d Lines.".format(orderDetFile.count()))
    val orderDetails = orderDetFile.map(_.split(",")).
      map(e => OrderDetails( e(0), e(1), e(2).trim.toFloat,e(3).trim.toInt, e(4).trim.toFloat ))
     println(orderDetails.count)
     orderDetails.toDF().registerTempTable("OrderDetails")
     result = sqlContext.sql("SELECT * from OrderDetails")
     result.take(10).foreach(println)
     //
     // Now the interesting part
     //
     result = sqlContext.sql("SELECT OrderDetails.OrderID,ShipCountry,UnitPrice,Qty,Discount FROM Orders INNER JOIN OrderDetails ON Orders.OrderID = OrderDetails.OrderID")
     result.take(10).foreach(println)
     result.take(10).foreach(e=>println("%s | %15s | %5.2f | %d | %5.2f |".format(e(0),e(1),e(2),e(3),e(4))))
     //
     // Sales By Country
     //
     /// result = sqlContext.sql("SELECT ShipCountry, S u m (OrderDetails.UnitPrice * Qty * Discount )  AS ProductSales  FROM Orders INNER JOIN OrderDetails ON Orders.OrderID = OrderDetails.OrderID GROUP BY ShipCountry")
     /// result.take(10).foreach(println)
     // Need to try this
     // println(result.take(30).mkString(" | "))
     // result.take(30).foreach(e -> ... mkString(" | ")
     // probably this would work
     /// result.take(30).foreach(e=>println("%15s | %9.2f |".format(e(0),e(1))))
     //
     // Parquet Operations
     //
     val parquetFileOrders = orders.toDF().saveAsParquetFile("/Volumes/sdxc-01/fdps-vii/Orders.parquet-01")
     val parquetFileOrderDet = orderDetails.toDF().saveAsParquetFile("/Volumes/sdxc-01/fdps-vii/OrderDetails.parquet-01")
     //
     // Let us read back the file
     //
     val parquetOrders = sqlContext.parquetFile("/Volumes/sdxc-01/fdps-vii/Orders.parquet")
     parquetOrders.registerTempTable("ParquetOrders")
     result = sqlContext.sql("SELECT * from ParquetOrders")
     result.take(10).foreach(e=>println("%5s | %5s | %s | %10s | %15s |".format(e(0),e(1),e(2),e(3),e(4))))
     //
     // Save our Sales By Country Report as parquet
     //
     val salesByCountry = sqlContext.sql("SELECT ShipCountry, S u m (OrderDetails.UnitPrice * Qty * Discount )  AS ProductSales  FROM Orders INNER JOIN OrderDetails ON Orders.OrderID = OrderDetails.OrderID GROUP BY ShipCountry")
     salesByCountry.registerTempTable("SalesByCountry")
     result = sqlContext.sql("SELECt * from SalesByCountry")
     result.take(30).foreach(e=>println("%15s | %9.2f |".format(e(0),e(1))))
     val parquetSALES = salesByCountry.saveAsParquetFile("/Users/ksankar/fdps-vii/SalesByCountry.parquet") 
  }
}