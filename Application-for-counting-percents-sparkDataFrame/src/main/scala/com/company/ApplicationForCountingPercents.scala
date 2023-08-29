package com.company

import com.typesafe.config.{Config, ConfigException, ConfigFactory}

import java.io.{File, FileInputStream, FileNotFoundException, IOException}

import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._

/**
 * @author ${user.name}
 * Utility to count percents per each year and month using spark DataFrame.
 *
 * @version 1.0
 */
object ApplicationForCountingPercents {

  def main(args : Array[String]) {
    // get arguments, config variables
    val (delimiter, algType, folderPath) = getParsedArguments(args)
    val (sparkContextConf, filesConfig) = getProperties
    val (appName, master, readingFormat,
         writingFormat, header, dateFormat,
         inferSchema, pathToSave, mode) = sparkConfig(sparkContextConf)

    val sparkSession = getSparkSession(master, appName)

    val dfSource = getSourceData(sparkSession, readingFormat, delimiter, folderPath, header, dateFormat, inferSchema)
    val dfWithFirstLastPrices = getFirstLastPricesPerEachMonthYear(folderPath, dfSource)
    val dfWithMonthGrowthInPricesAndTotalGrowth = getGrowthInStockPrices(algType, dfWithFirstLastPrices)
    dfWithMonthGrowthInPricesAndTotalGrowth.sort("year").show

    writeResultDF(dfWithMonthGrowthInPricesAndTotalGrowth, delimiter, header, writingFormat, pathToSave, mode)
  }

  /**
   * Method for parsing input arguments.
   *
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
   * Method for getting configuration data.
   *
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
   * Method for getting Spark configuration data.
   *
   * @param sparkContextConf
   * @return master and application name and some options for writing&reading data to DF
   */
  def sparkConfig(sparkContextConf: Config): (String, String, String, String, String, String, String, String, String)= {
    try{
      val appName = sparkContextConf.getString("appName")
      val master = sparkContextConf.getString("master")
      val readingFormat = sparkContextConf.getString("readingFormat")
      val writingFormat = sparkContextConf.getString("writingFormat")
      val header = sparkContextConf.getString("header")
      val dateFormat = sparkContextConf.getString("dateFormat")
      val inferSchema = sparkContextConf.getString("inferSchema")
      val pathToSave =  sparkContextConf.getString("pathToSave")
      val mode =  sparkContextConf.getString("mode")

      ( appName, master, readingFormat,
        writingFormat, header, dateFormat,
        inferSchema, pathToSave, mode
      )
    } catch {
      case e: Exception => throw new ConfigException.Missing(s"Error in sparkConfig method: ${e.getMessage}")
    }
  }

  /**
   * Method for getting spark session.
   *
   * @param master
   * @param appName
   * @return spark session
   */
  def getSparkSession(master: String, appName: String): SparkSession = {
    SparkSession.builder
      .master(master)
      .appName(appName)
      .getOrCreate
  }

  /**
   * Method for getting DataFrame from files with format declared in .conf file.
   *
   * @param sparkSession
   * @param readingFormat
   * @param delimiter
   * @param folderPath
   * @param header
   * @param dateFormat
   * @param inferSchema
   * @return spark DataFrame of necessary schema
   */
  def getSourceData(sparkSession: SparkSession, readingFormat: String,
                    delimiter: String, folderPath:String,
                    header: String, dateFormat: String, inferSchema: String): DataFrame = {

    sparkSession.read
                .format(readingFormat)
                .options(Map("delimiter" -> delimiter,
                              "header" -> header,
                              "dateFormat" -> dateFormat,
                              "inferSchema" -> inferSchema))
                .load(folderPath)
  }

  /**
   * Method for getting spark DataFrame with first and last prices per each year and each month in it.
   *
   * @param folderPath for getting stockName from it
   * @param dfSource DataFrame with source data
   * @return DataFrame with first and last prices
   */
  def getFirstLastPricesPerEachMonthYear(folderPath:String, dfSource: DataFrame): DataFrame = {
    val dfWithNecessaryColumns = dfSource.select(year(col("Time (UTC)")).as("year"),
                                                 month(col("Time (UTC)")).as("month"),
                                                 col("Time (UTC)").as("timeUTC"),
                                                 col("Open").as("openPrice"),
                                                 col("Close").as("closePrice"))
                                        .withColumn("openPrice", regexp_replace(col("openPrice"), ",", "."))
                                        .withColumn("closePrice", regexp_replace(col("closePrice"), ",", "."))


    val partitionedWindow = Window.partitionBy( "year", "month")
                                  .orderBy("timeUTC")
                                  .rangeBetween(Window.unboundedPreceding, Window.unboundedFollowing)

    val dfWithFirstLastPrices = dfWithNecessaryColumns.withColumn("openPrice", first("openPrice").over(partitionedWindow))
                                                      .withColumn("closePrice", last("closePrice").over(partitionedWindow))
                                                      .drop("timeUTC")
                                                      .dropDuplicates
                                                      .withColumn("stockName", lit(folderPath.split("/")(1)))
    dfWithFirstLastPrices
  }

  /**
   * Method for getting spark DataFrame with counted percents with standard formula per each month.
   *
   * @param algType
   * @param dfWithFirstLastPrices
   * @return DataFrame with calculated metrics
   */
  def getGrowthInStockPrices(algType: String, dfWithFirstLastPrices:DataFrame): DataFrame = {
    val partitionedWindow = Window.partitionBy( "stockName")
                                  .orderBy("year", "month")

    val dfWithTwoPreviousClosePrice = dfWithFirstLastPrices.select(col("stockName"),
                                                                         col("year"),
                                                                         col("month"),
                                                                         col("openPrice"),
                                                                         col("closePrice"))
                                                                  .withColumn("algorithm", lit(algType))
//                                                                  .withColumn("prevOpenPrice", lag("openPrice", 1, 0.0)
//                                                                                                            .over(partitionedWindow))
                                                                  .withColumn("prevClosePrice", lag("closePrice", 1, 1569.093)
                                                                                                            .over(partitionedWindow))
                                                                  .withColumn("prevPrevClosePrice", lag("prevClosePrice", 1,1569.093)
                                                                                                                .over(partitionedWindow))
                                                                  .withColumn("rightVariable", algType match {
                                                                                                            case "o-c" => lit(col("openPrice"))
                                                                                                            case "c-c" => lit(col("prevPrevClosePrice"))
                                                                                                          })

    val dfWithCalculatedPrices = dfWithTwoPreviousClosePrice.withColumn("growthPrice", (col("closePrice") - col("rightVariable"))
                                                                                                    / col("rightVariable") * 100)
                                                            .select(col("stockName"),
                                                                    col("year"),
                                                                    col("month"),
                                                                    col("algorithm"),
                                                                    col("growthPrice"))
                                                            .withColumn("totalGrowth", avg(col("growthPrice"))
                                                              .over( Window.partitionBy("stockName","year")))

    val renamedColumns = Map(
      "1" -> "january", "2" -> "february", "3" -> "march", "4" -> "april", "5" -> "may",
      "6" -> "june", "7" -> "july", "8" -> "august", "9" -> "september", "10" -> "october",
      "11" -> "november", "12" -> "december"
    )

    val pivotDF = dfWithCalculatedPrices.groupBy("stockName", "algorithm", "year", "totalGrowth")
                                        .pivot("month")
                                        .sum("growthPrice")

    val resultDF = pivotDF.select(
                                   pivotDF.columns.map(column => pivotDF(column).as(renamedColumns.get(column).getOrElse(column))): _*
                                  )
    resultDF
  }

  /**
   * Method for writing and saving calculated metrics.
   *
   * @param resultDF DataFrame with calculated metrics
   * @param delimiter
   * @param header
   * @param writingFormat
   * @param pathToSave
   * @param mode
   */
  def writeResultDF(resultDF: DataFrame, delimiter: String, header: String, writingFormat: String, pathToSave: String, mode: String): Unit = {
    resultDF.write
            .format(writingFormat)
            .options(Map("delimiter" -> delimiter,
                         "header" -> header,
                          "nullValue" -> "NULL"
                        )
                    )
            .mode(mode)
            .save(pathToSave)
  }
}