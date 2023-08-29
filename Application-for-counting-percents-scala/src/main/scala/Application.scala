package org.apenko.app

import java.sql.{Connection, DriverManager, ResultSet, SQLException, Statement, PreparedStatement, Types}
import java.io.{File, FileNotFoundException, IOException}

import scala.io.Source
import scala.collection.mutable.ListBuffer
import com.typesafe.config.{Config, ConfigFactory}

/**
 * Application with global variables <b>stockName</b>, <b>previousYear</b>, <b>algorithmType</b> and <b>previousYearClosePrice</b>.
 * This is an application for obtaining changes in stock prices for a necessary year in a specific subgroup.
 * Expressed in percents.
 * It also calculates the average value for the current year.
 * @autor Anastasiya Apenko
 * @version 1.0
 */
object Application {
  // global variables
  var previousYearClosePrice: Double = Double.NaN
  var previousYear: Int = 0 // for c-c algorithm
  var algorithmType = ""
  var stockName = ""

  /**
   * Program entry point.
   * @param args provided command line arguments, which will be parsed.
   */
  def main(args: Array[String]): Unit = {
    // get arguments, list of files, config variables, connection to DB
    val (delimiter, algType, folderPath) = getParsedArguments(args)
    algorithmType = algType
    var filesList = getListOfFiles(folderPath)
    val (mysqlConfig, filesConfig) = getProperties
    val connectionToMySQL = getConnection(mysqlConfig)

    // dictionary with percents of whole years
    var resultDict: Map[Int, scala.collection.mutable.ListBuffer[Double]] = Map()
    resultDict = processData(connectionToMySQL, filesList, delimiter, resultDict)

    // count average percents
    var avgDict = countAveragePercents(resultDict).toArray.sortBy(_._1).map(el => el._2)
    var totalAvg = getYearTotal(avgDict)

    insertIntoAverageTable(connectionToMySQL, stockName, avgDict, totalAvg)

    //printResultsFromTable(connectionToMySQL)
    connectionToMySQL.close()
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
   * Method for getting necessary variable for algorithm
   * @param closeFirst first close price of current month
   * @param openFirst first open price of current month
   * @throws
   * @return  double variable for algorithm
   */
  @throws[Exception]
  def getCorrectVariable(closeFirst: Double, openFirst: Double): Double = {
    var variable = 0.0
    algorithmType match {
      case "o-c" =>
        variable = openFirst
      case "c-c" =>
        variable = closeFirst
      case _ =>
        variable = openFirst
    }
    variable
  }

  /**
   * Method for getting percents per current month
   * @param closeFirst first close price of current month
   * @param closeLast last close price of current month
   * @param openFirst first open price of current month
   * @return double value of percents per current month
   */
  def countPercents(closeFirst: Double, closeLast: Double, openFirst: Double): Double = {
    var monthResult = Double.NaN
    try monthResult = (closeLast - getCorrectVariable(closeFirst, openFirst)) * 100 / getCorrectVariable(closeFirst, openFirst)
    catch {
      case e: Exception =>
        System.out.println(e.getMessage)
    }
    monthResult
  }

  /**
   * Method for getting percents per whole current year
   * @param percents array of double values (percents) for each month in this year
   * @return double value of percents per whole year
   */
  def getYearTotal(percents: Array[Double]): Double= {
    var sum = 0.0
    var countNaN = 0
    for (percent <- percents) {
      if (percent.isNaN) {
        sum += 0.0
        countNaN += 1
      }
      else {
        sum += percent
      }
    }
    val total = sum / (percents.length - countNaN)

    total
  }

  /**
   *Method for parsing row from file
   * @param row string from file
   * @param delimiter string from arguments
   * @return tuple of int - year, int - month, double - open price, double - close price
   */
  def parseRow(row: String, delimiter: String): (Int, Int, Double, Double) = {
    var fields = row.split(delimiter)

    var dateWithTime = fields(0).split(" ")
    var clearDate = dateWithTime(0)
    var currentYear = clearDate.split("-")(0).toInt
    var currentMonth = clearDate.split("-")(1).toInt

    var openPrice = fields(1).replace(',', '.').toDouble
    var closePrice = fields(4).replace(",", ".").toDouble
    (currentYear, currentMonth, openPrice, closePrice)
  }

  /**
   * Method which transform data, counting percents and insert them into table
   * @param connection Connection object with url, user, password from application.conf
   * @param filesList list of strings with filenames
   * @param delimiter string from arguments for splitting
   * @param resultDict dictionary with results
   * @return dictionary with results
   */
  def processData(connection: Connection, filesList: List[File], delimiter: String, resultDict: Map[Int, scala.collection.mutable.ListBuffer[Double]]): Map[Int, scala.collection.mutable.ListBuffer[Double]] = {
    var resultDict: Map[Int, scala.collection.mutable.ListBuffer[Double]] = Map()
    for (i <- 0 until 12) {
      resultDict += (i -> ListBuffer())
    }

    for (path <- filesList) {
      var yearPercents = countPercentsPerMonth(path.toString, delimiter)
      var totalYearPercent = getYearTotal(yearPercents)
      var (currentStockName, currentYear) = parseStockNameAndYearFromPath(path.toString)

      resultDict = fillingDict(resultDict, yearPercents)

      insertIntoDestinationDB(connection, currentStockName, currentYear, yearPercents, totalYearPercent)
      stockName = currentStockName
    }

    resultDict
  }

  /**
   * Method for counting prices from files and getting result array
   * @param filePath path to the file
   * @param delimiter provided delimiter for files
   * @return array of double values containing percents per each month (NaN if don't have)
   */
  def countPercentsPerMonth(filePath: String, delimiter: String): Array[Double] = {
    val bufferedSource = Source.fromFile(filePath)

    var results = new Array[Double](12).map((el) => Double.NaN)
    var monthIndex = 0 // position inside results array
    var currentMonthIndex = 0 // for first line flag

    var currentMonth = 0
    var previousMonth = 0

    var currentMonthOpenPrice: Double = Double.NaN
    var currentMonthFirstClosePrice: Double = Double.NaN
    var currentMonthLastClosePrice: Double = previousYearClosePrice

    for (line <- bufferedSource.getLines.drop(1)) {
      var (currentYear, currentMonth, openPrice, closePrice) = parseRow(line, delimiter)

      if (previousYear == 0) // for first load & getting start year
        previousYear = currentYear

      if (currentMonthIndex == 0) {
        previousMonth = currentMonth
        currentMonthOpenPrice = openPrice
        currentMonthFirstClosePrice = closePrice
      }

      if (currentMonth == previousMonth) currentMonthIndex += 1
      else if (currentMonth - previousMonth > 1) { // when miss a few months
        results(monthIndex) = countPercents(currentMonthFirstClosePrice, currentMonthLastClosePrice, currentMonthOpenPrice)
        val missedMonthAmount = currentMonth - previousMonth
        for (i <- 0 until missedMonthAmount) {
          results(previousMonth + i) = Double.NaN
        }
        monthIndex += missedMonthAmount
        previousMonth = currentMonth
        currentMonthOpenPrice = openPrice
        currentMonthFirstClosePrice = closePrice
      }
      else {
        if (currentYear != previousYear) { // check for c-c algorithm
          results(monthIndex) = countPercents(previousYearClosePrice, currentMonthLastClosePrice, currentMonthOpenPrice)
          previousYear = currentYear
        }
        else
          results(monthIndex) = countPercents(currentMonthFirstClosePrice, currentMonthLastClosePrice, currentMonthOpenPrice)
        monthIndex += 1
        currentMonthOpenPrice = openPrice
        currentMonthFirstClosePrice = closePrice
        previousMonth = currentMonth
      }
      currentMonthLastClosePrice = closePrice
    }
    bufferedSource.close

    // append last results into array
    results(monthIndex) = countPercents(currentMonthFirstClosePrice, currentMonthLastClosePrice, currentMonthOpenPrice)

    // getting last close price in year
    previousYearClosePrice = currentMonthLastClosePrice
    results
  }

  /**
   * Method for getting name and year of group for counting
   * @param path file path
   * @return tuple of string - name, int - year
   */
  def parseStockNameAndYearFromPath(path: String): (String, Int) = {
    val pattern = "\\d{4}".r
    val splittedPath = path.split("/")
    var stockName = splittedPath(1)
    var year = pattern.findFirstIn(splittedPath(2)).getOrElse("No match for year.").toInt

    (stockName, year)
  }

  /**
   * Method for getting configuration data
   * @return tuple of MySQL configs, files configs
   */
  def getProperties: (Config, Config) = {
    try {
      val config = ConfigFactory.load("application.conf")

      (config.getConfig("Application.mysqlConf"), config.getConfig("Application.filesConf"))
    }
    catch {
      case e: FileNotFoundException => throw new FileNotFoundException(s"Can't find config file. Error: ${e.getMessage}")
    }
  }

  /**
   * Method for connecting to MySQL database
   * @param dbConfig config object
   * @return Connection object with url, user, password fromapplication.conf
   */
  def getConnection(dbConfig: Config): Connection = {

    val url = dbConfig.getString("url")
    val driver = dbConfig.getString("driver")
    val username = dbConfig.getString("user")
    val password = dbConfig.getString("password")

    try {
      Class.forName(driver)
      DriverManager.getConnection(url, username, password)
    }
    catch {
      case e: Exception => throw new InterruptedException(s"Error: ${e.getMessage}")
    }
  }

  def insertIntoStagingDB: Unit = ???

  /**
   * Method for inserting result values into Destination table from file application.conf
   * @param connection Connection object with url, user, password from application.conf
   * @param stockName current name from file
   * @param year current year from file
   * @param yearPercents array of double values, which are percents per each month
   * @param yearTotal double value of average meaning per current year
   */
  def insertIntoDestinationDB(connection: Connection, stockName: String, year: Int, yearPercents: Array[Double], yearTotal: Double): Unit = {
    val insertSql = """
                      |insert into destination_table_increase_in_stocks_Bid
                      |values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.stripMargin

    var preparedStatement: PreparedStatement = null

    try {
      preparedStatement = connection.prepareStatement(insertSql)

      preparedStatement.setString(1, stockName)
      preparedStatement.setInt(2, year)
      preparedStatement.setString(3, algorithmType)

      for (i <- 0 until yearPercents.length)
        if (yearPercents(i).isNaN)
          preparedStatement.setNull(i+4, Types.DECIMAL)
        else
          preparedStatement.setDouble(i+4, yearPercents(i))

      preparedStatement.setDouble(16, yearTotal)

      preparedStatement.execute
      preparedStatement.close
    } catch {
      case sqle: SQLException => sqle.printStackTrace
      case e: Exception => e.printStackTrace
    }
  }

  /**
   * Method for inserting result values into Average table from file application.conf
   * @param connection Connection object with url, user, password from application.conf
   * @param stockName current name from file
   * @param avgPercents array of double values, which are average percents per each year
   * @param avgTotal double value of average meaning for whole year
   */
  def insertIntoAverageTable(connection: Connection, stockName: String, avgPercents: Array[Double], avgTotal: Double): Unit = {
    val insertSql = """
                      |insert into average_per_years_increase_in_stocks_Bid
                      |values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.stripMargin

    var preparedStatement: PreparedStatement = null

    try {
      preparedStatement = connection.prepareStatement(insertSql)

      preparedStatement.setString(1, stockName)
      preparedStatement.setString(2, algorithmType)

      for (i <- 0 until avgPercents.length)
        if (avgPercents(i).isNaN)
          preparedStatement.setNull(i+3, Types.DECIMAL)
        else
          preparedStatement.setDouble(i+3, avgPercents(i))

      preparedStatement.setDouble(15, avgTotal)

      preparedStatement.execute
      preparedStatement.close
    } catch {
      case sqle: SQLException => sqle.printStackTrace
      case e: Exception => e.printStackTrace
    }
  }

  def getDataFromTable(connection: Connection): Unit  = {

  }

  /**
   * Method for filling dictionary with results per each year
   * @param resultDict dictionary with results
   * @param yearPercents array of double values containing percents per each month
   * @return dictionary with results per each year
   */
  def fillingDict(resultDict: Map[Int, scala.collection.mutable.ListBuffer[Double]], yearPercents: Array[Double]): Map[Int, scala.collection.mutable.ListBuffer[Double]] = {
    for (i <- 0 until yearPercents.length) {
      resultDict(i) += yearPercents(i)
    }
    resultDict
  }

  /**
   * Method for counting average percents per each year
   * @param resultDict dictionary with results per each year
   * @return dictionary with average percents per each year
   */
  def countAveragePercents(resultDict: Map[Int, scala.collection.mutable.ListBuffer[Double]]): Map[Int, Double] = {
    var sum = 0.0
    var cNan = 0.0
    var avgDict: Map[Int, Double] = Map()
    for (i <- 0 until 12) {
      avgDict += (i -> Double.NaN)
    }

    for(key <- resultDict.keys){
      for(value <- resultDict(key)){
        if(value.isNaN){
          sum += 0.0
          cNan += 1
        }
        else{
          sum += value
        }
      }
        avgDict = avgDict + (key -> sum/(resultDict(key).length - cNan))
        sum = 0.0
        cNan = 0.0
    }

    avgDict
  }

  // temporary method for testing results
  def printResultsFromTable(connection: Connection): Unit = {

    val statement = connection.createStatement()
    val resultSet = statement.executeQuery("SELECT * from average_per_month")

    // get meta data from table
    val rsmd = resultSet.getMetaData
    val columnsNumber = rsmd.getColumnCount
    while (resultSet.next) {
      for (i <- 1 to columnsNumber) {
        if (i > 1) System.out.print(",  ")
        val columnValue = resultSet.getString(i)
        System.out.print(columnValue + " " + rsmd.getColumnName(i))
      }
      System.out.println("")
    }
  }
}