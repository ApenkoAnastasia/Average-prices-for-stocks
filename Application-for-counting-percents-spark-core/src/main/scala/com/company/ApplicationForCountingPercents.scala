package com.company

import com.typesafe.config.{Config, ConfigException, ConfigFactory}

import java.io.{File, FileInputStream, FileNotFoundException, IOException}
import java.security.{DigestInputStream, MessageDigest}
import java.sql.{Connection, DriverManager, PreparedStatement, Date, Timestamp}

import scala.io.Source
import scala.collection.mutable.ListBuffer

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

/**
 * @author ${user.name}
 * Utility to count percents per each year and month using spark core.
 * @version 1.0
 */
object ApplicationForCountingPercents {

  def main(args : Array[String]) {
    // get arguments, list of files, config variables
    val (delimiter, algType, folderPath) = getParsedArguments(args)
    val filesList = getListOfFiles(folderPath)
    val (sparkContextConf, filesConfig) = getProperties
    val (appName, master) = sparkConfig(sparkContextConf)

    val sc = getSparkContext(appName, master)
    sc.setLogLevel("ERROR")

    val firstLastPricesRDD = getFirstLastPricesPerEachMonthYear(filesList, sc, delimiter)
    val monthGrowthInPricesRDD = getGrowthInStockPrices(algType, firstLastPricesRDD)
    monthGrowthInPricesRDD.sortByKey().collect().foreach(println)

    val totalPerEachYearRDD = getCalculatedTotal(monthGrowthInPricesRDD)
    totalPerEachYearRDD.sortByKey().collect().foreach(println)

    monthGrowthInPricesRDD.saveAsTextFile("./outputMonthGrowth")
    totalPerEachYearRDD.saveAsTextFile("./outputTotalPercents")

    sc.stop()
  }

  /**
   * Method for parsing input arguments
   * @param args provided command line arguments
   * @return tuple of strings with arguments <b>file_path</b>, <b>delimiter</b>, <b>algorithm_type</b>
   */
  def getParsedArguments(args: Array[String]): (String, String, String) = {
    val helpBox = """
    Usage: Application
    -dl,--delimiter <delimiter>   delimiter for parsing rows (";" - default)
    -at,--algorithm_type <algorithm_type>   type of algorithm, wich will be used ("o-c" - default)
    -fl,--folder_path <folder_path>   path to folder with files
    """
    if (args.length != 6) {
      throw new IllegalArgumentException(helpBox)
    } else {
      var currentDelimiter = ""
      var currentAlgorithm = ""
      var currentFolder = ""
      args.sliding(2, 2).toList.collect {
        case Array(key: String, argDelimiter: String) if(key == "-dl" || key == "--delimiter") => currentDelimiter = argDelimiter
        case Array(key: String, argAlgorithm: String) if(key == "-at" || key == "--algorithm_type") => currentAlgorithm = argAlgorithm
        case Array(key: String, argFolder: String) if(key == "-fl" || key == "--folder_path") => currentFolder = argFolder
        case _ => throw new IllegalArgumentException("Unknown option." + helpBox)
      }
      (currentDelimiter, currentAlgorithm, currentFolder)
    }
  }

  /**
   * Method for receiving list of files in current directory
   * @param dir string of current directory
   * @return list of files in such directory
   */
  def getListOfFiles(dir: String):List[File] = {
    val folder = new File(dir)
    if (folder.exists && folder.isDirectory) {
      folder.listFiles.filter(_.isFile).toList.sorted
    } else {
      List[File]()
    }
  }

  /**
   * Method for getting configuration data
   * @return tuple of spark context configs, files configs
   */
  def getProperties: (Config, Config) = {
    try {
      val config = ConfigFactory.load("application.conf")

      (config.getConfig("Application.sparkContextConf"), config.getConfig("Application.filesConf"))
    }
    catch {
      case e: FileNotFoundException => throw new FileNotFoundException(s"Can't find config file. Look at getProperties method. Error: ${e.getMessage}")
    }
  }

  /**
   * Method for getting Spark configuration data
   * @param sparkContextConf
   * @return master and application name
   */
  def sparkConfig(sparkContextConf: Config): (String, String)= {
    try{
      val appName = sparkContextConf.getString("appName")
      val master = sparkContextConf.getString("master")

      (appName, master)
    } catch {
      case e: Exception => throw new ConfigException.Missing(s"Error in sparkConfig method: ${e.getMessage}")
    }
  }

  /**
   * Method for getting Spark context
   * @param appName
   * @param master
   * @return spark context object
   */
  def getSparkContext(appName: String, master: String): SparkContext = {
    SparkContext.getOrCreate(new SparkConf().setAppName(appName)
                                            .setMaster(master))
  }

  /**
   * Method for getting spark RDD with first and last prices per each year and each month in it
   * @param filesList
   * @param sc
   * @param delimiter
   * @return spark RDD
   */
  def getFirstLastPricesPerEachMonthYear(filesList: List[File], sc: SparkContext, delimiter: String):  RDD[((Int, Int), (Double, Double))] = {
    var firstLastPricesRDD = sc.emptyRDD[((Int, Int), (Double, Double))]

    // process each file
    for (path <- filesList) {
      // in dataframe will be dataFrame = spark.read.format("CSV").option("header","true").load(csvfilePath)
      val fileRDD = sc.textFile(path.toString)
      val header = fileRDD.first

      val clearDataRDD = getDataWithoutHeader(header, fileRDD)

      val parsedRDD = clearDataRDD.map(line => parseRows(line))
                                  .keyBy(line => (line._1,line._2))
                                  .mapValues(values => (values._3,values._4))

      val resultRDD = parsedRDD.reduceByKey(getFirstLastPricesPerMonth)

      firstLastPricesRDD = firstLastPricesRDD.union(resultRDD)
    }
    firstLastPricesRDD
  }

  /**
   * Method for getting spark RDD without header
   * @param header
   * @param fileRDD
   * @return spark RDD
   */
  def getDataWithoutHeader(header: String, fileRDD: RDD[String]): RDD[String] = {
    var data = fileRDD
    if (header.startsWith("Time (UTC);"))
      data = fileRDD.filter(line => line != header)
    data
  }

  /**
   * Method for parsing each line
   * @param line
   * @return month, year, open price and close price of each month
   */
  def parseRows(line: String):(Int, Int, Double, Double) = {
    val Array(timeUTC, openPrice, highPrice, lowPrice, closePrice, volume)  = line.split(";")

    val dateWithTime = timeUTC.split(" ")
    val clearDate = dateWithTime(0)
    val currentYear = clearDate.split("-")(0).toInt
    val currentMonth = clearDate.split("-")(1).toInt

    (currentMonth, currentYear, openPrice.replace(",", ".").toDouble, closePrice.replace(",", ".").toDouble)
  }

  /**
   * Method for getting tuple of first and last prices per month through joining them
   * @param left
   * @param right
   * @return tuple of first and last prices
   */
  def getFirstLastPricesPerMonth(left: (Double, Double),
                              right: (Double, Double)): (Double, Double) = {
    val (leftOpenPrice, _) = left
    val (_, rightClosePrice) = right
    (leftOpenPrice, rightClosePrice)
  }

  /**
   * Method for getting spark RDD with counted percents per each month
   * First - get previous and prev previous prices (like LAG function)
   * Then - count percents with standard formula
   * @param algType
   * @param firstLastPricesRDD
   * @return spark RDD
   */
  def getGrowthInStockPrices(algType: String, firstLastPricesRDD: RDD[((Int, Int), (Double, Double))]): RDD[(Int, (Int, Double))] = {
    // add index to RDD for further calculations:
    val rddWithIndex = firstLastPricesRDD.sortBy(_._1._1).sortBy(_._1._2).zipWithIndex().map(_.swap)

    // create another RDD with indices increased by one, to later join each element with the previous one
    val previousPricesRDD = rddWithIndex.map { case (index, v) => (index + 1, v) }
    val prevPrevPricesRDD = rddWithIndex.map { case (index, v) => (index + 2, v) }

    // join RDDs with previous values
    val joinedWithPrevPrices = rddWithIndex.leftOuterJoin(previousPricesRDD)
    val clearRDDWithPrevPrices = joinedWithPrevPrices.map{
        case (idx, (((month, year),(currentOpen,currentClose)), newMetric)) => newMetric match {
        case Some(((oldMonth, oldYear), (prevOpen,prevClose))) => (idx, ((month, year),(currentOpen,currentClose),(prevOpen,prevClose)))
        case None => (idx,((month, year),(currentOpen,currentClose),(currentOpen, currentOpen)))
      }
    }

    val joinedWithPrevPrevPrices = clearRDDWithPrevPrices.leftOuterJoin(prevPrevPricesRDD)
    val clearRDDWithPrevPrevPrices = joinedWithPrevPrevPrices.map {
        case (idx, (((month, year),(currentOpen,currentClose),(prevOpen, prevClose)), newMetric)) => newMetric match {
        case Some(((oldMonth, oldYear), (prevPrevOpen,prevPrevClose))) => (idx, ((month, year),(currentOpen,currentClose),(prevOpen,prevClose),(prevPrevOpen,prevPrevClose)))
        case None => (idx, ((month, year),(currentOpen,currentClose),(prevOpen, prevClose),(currentOpen, currentOpen)))
      }
    }

    val rddWithCalculatedPrices = clearRDDWithPrevPrevPrices.map {
      case (idx, ((month, year),(currentOpen,currentClose),(prevOpen, prevClose),(prevPrevOpen,prevPrevClose))) =>
        (year, (month,(getCalculatedPrices(algType, currentClose, currentOpen, prevPrevClose))))
    }
    rddWithCalculatedPrices
  }

  /**
   * Method for getting percent
   * Using different formulas
   * @param algType
   * @param currentClose
   * @param currentOpen
   * @param prevPrevClose
   * @return double value of percent
   */
  def getCalculatedPrices(algType: String, currentClose: Double, currentOpen: Double, prevPrevClose: Double): Double = {
    var growthPricePerMonth = 0.0
    algType match {
      case "o-c" =>
        growthPricePerMonth = (currentClose - currentOpen) * 100 / currentOpen
      case "c-c" =>
        growthPricePerMonth = (currentClose - prevPrevClose) * 100 / prevPrevClose
      case _ =>
        growthPricePerMonth = (currentClose - currentOpen) * 100 / currentOpen
    }
    growthPricePerMonth
  }

  /**
   * Method for getting spark RDD of total percents for each year
   * @param percentsPerMonthRDD
   * @return
   */
  def getCalculatedTotal(percentsPerMonthRDD: RDD[(Int, (Int, Double))]): RDD[(Int, Double)] = {
    val totalPerYearsRDD = percentsPerMonthRDD.map {case (year, (month, percent)) => (year, (percent, 1))}
      .reduceByKey((v1,v2) => (v1._1+v2._1,v1._2+v2._2))
      .map {case (year, (percent, count)) => (year, percent / count)}

    totalPerYearsRDD
  }
}