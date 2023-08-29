package org.apenko.app

import java.sql.{Connection, DriverManager, ResultSet, SQLException, Statement, PreparedStatement, Types, Timestamp}
import java.io.{File, FileInputStream, FileNotFoundException, IOException}
import java.security.{DigestInputStream, MessageDigest}

import scala.io.Source
import scala.collection.mutable.{ListBuffer, ArrayBuffer}
import com.typesafe.config.{Config, ConfigFactory}

/**
 * @autor Anastasiya Apenko
 * @version 1.0
 */
object LoadDataToStaging {
  var numberOfProcessedRows: Int = 0
  var errorFlag = false

  /**
   * Program entry point.
   * @param args provided command line arguments, which will be parsed.
   */
  def main(args: Array[String]): Unit = {
    // get arguments, list of files, config variables, connection to DB
    val (delimiter, folderPath) = getParsedArguments(args)
    var filesList = getListOfFiles(folderPath)
    val (destinationConfig, stagingConfig, filesConfig) = getProperties
    val connectionToStagingDB = getConnection(stagingConfig)

    processData(connectionToStagingDB, filesList, delimiter)

    connectionToStagingDB.close()
  }

  /**
   * Method for parsing input arguments
   * @param args provided command line arguments
   * @return tuple of strings with arguments <b>file_path</b>, <b>delimiter</b>
   */
  def getParsedArguments(args: Array[String]): (String, String) = {
    val helpBox = """
    Usage: Application
    -dl,--delimiter <delimiter>   delimiter for parsing rows (";" - default)
    -fl,--folder_path <folder_path>   path to folder with files
    """
    if (args.length != 4) {
      throw new IllegalArgumentException(helpBox)
    } else {
      var currentDelimiter = ""
      var currentFolder = ""
      args.sliding(2, 2).toList.collect {
        case Array(key: String, argDelimiter: String) if(key == "-dl" || key == "--delimiter") => currentDelimiter = argDelimiter
        case Array(key: String, argFolder: String) if(key == "-fl" || key == "--folder_path") => currentFolder = argFolder
        case _ => throw new IllegalArgumentException("Unknown option." + helpBox)
      }
      (currentDelimiter, currentFolder)
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
   *Method for parsing row from file
   * @param row string from file
   * @param delimiter string from arguments
   * @return tuple of date with time - string, double - open price, double - close price
   */
  def parseRow(row: String, delimiter: String): (String, Int, Double, Double) = {
    val fields = row.split(delimiter)

    val dateWithTime = fields(0)
    val currentMonth = dateWithTime.split(" ")(0).split("-")(1).toInt

    val openPrice = fields(1).replace(',', '.').toDouble
    val closePrice = fields(4).replace(",", ".").toDouble
    (dateWithTime, currentMonth, openPrice, closePrice)
  }

  /**
   * Method for split file path
   * @param path file path
   * @return array of string with splitted path
   */
  def getSplittedPath(path: String): Array[String] = {
    val splittedPath = path.split("/")

    splittedPath
  }

  /**
   * Compute a hash of a file (md5 sum)
   * @param path path to the current file
   * @return md5 sum for such file as string
   */
  def computeHash(path: String): String = {
    val buffer = new Array[Byte](8192)
    val md5 = MessageDigest.getInstance("MD5")

    val dis = new DigestInputStream(new FileInputStream(new File(path)), md5)
    try { while (dis.read(buffer) != -1) { } } finally { dis.close() }

    md5.digest.map("%02x".format(_)).mkString
  }

  /**
   * Method which get md5 sum of file, read & process them and insert it into staging table
   * @param connection Connection object with url, user, password from application.conf
   * @param filesList list of strings with filenames
   * @param delimiter string from arguments for splitting
   */
  def processData(connection: Connection, filesList: List[File], delimiter: String): Unit = {
    for (path <- filesList) {
      val splittedPath = getSplittedPath(path.toString)
      val currentStockName = splittedPath(2)
      val currentYear = splittedPath(3).toInt

      // get audit information about file
      val startTime = System.currentTimeMillis
      // count hash sum of each file
      var md5Hash = computeHash(path.toString)

      numberOfProcessedRows = 0

      val stagingRows = readAndProcessFile(path.toString, delimiter, currentStockName)

      val endTime = System.currentTimeMillis

      insertIntoStagingAuditTable(connection, currentStockName,
                                  currentYear, path.toString,
                          "Bid", md5Hash,
                                    startTime, endTime,
                                    numberOfProcessedRows, errorFlag)

      for(row <- stagingRows){
        insertIntoStagingDB(connection, row._1, row._2, row._3, row._4)
      }
    }
  }

  /**
   * Method for read file and get first and last row of each month, then insert them into result array
   * @param splittedPath
   * @param filePath
   * @param delimiter
   */
  def readAndProcessFile(filePath: String, delimiter: String, stockName: String): Array[(String, String, Double, Double)] = {
    val bufferedSource = Source.fromFile(filePath)
    var resultRows = Array[(String, String, Double, Double)]()

    var firstIter = 0
    var previousMonth = 0
    var previousDate = ""
    var previousOpenPrice = 0.0
    var previousClosePrice = 0.0

    for (line <- bufferedSource.getLines.drop(1)) {
      var (dateWithTime, currentMonth, openPrice, closePrice) = parseRow(line, delimiter)
      numberOfProcessedRows += 1

      if (currentMonth == previousMonth) {
        firstIter += 1
        previousDate = dateWithTime
        previousOpenPrice = openPrice
        previousClosePrice = closePrice
      }
      else{
        if (firstIter == 0){ // for first iteration on each year
          resultRows = resultRows :+ (stockName, dateWithTime, openPrice, closePrice)
        }
        else {
          resultRows = resultRows :+ (stockName, previousDate, previousOpenPrice, previousClosePrice)
          resultRows = resultRows :+ (stockName, dateWithTime, openPrice, closePrice)
          previousDate = dateWithTime
          previousOpenPrice = openPrice
          previousClosePrice = closePrice
        }
        previousMonth = currentMonth
      }
    }
    resultRows = resultRows :+ (stockName, previousDate, previousOpenPrice, previousClosePrice) // add last row
    bufferedSource.close

    resultRows
  }

  /**
   * Method for getting configuration data
   * @return tuple of MySQL configs, files configs
   */
  def getProperties: (Config, Config, Config) = {
    try {
      val config = ConfigFactory.load("application.conf")
      val destinationConf = config.getConfig("Application.mysqlDestinationConf")
      val stagingConf = config.getConfig("Application.mysqlStagingConf")
      val filesConf = config.getConfig("Application.filesConf")

      (destinationConf, stagingConf, filesConf )
    }
    catch {
      case e: FileNotFoundException => throw new FileNotFoundException(s"Can't find config file. Error: ${e.getMessage}")
    }
  }

  /**
   * Method for connecting to MySQL database
   * @param dbConfig config object
   * @return Connection object with url, user, password from application.conf
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

  /**
   * Method for insert data into staging table for further processing it with mysql tools
   * @param connection
   * @param stockName
   * @param currentDate
   * @param openPrice
   * @param closePrice
   */
  def insertIntoStagingAuditTable(connection: Connection,
                                  stockName: String,
                                  currentYear: Int,
                                  filePath: String,
                                  fileType: String,
                                  md5Sum: String,
                                  startDate: Long,
                                  endDate: Long,
                                  numberRows: Int,
                                  errors: Boolean): Unit = {
    val insertSql = """
                      |insert into staging_audit
                      |values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.stripMargin
    val startTimestamp = new Timestamp(startDate)
    val endTimestamp = new Timestamp(endDate)
    val fileName = filePath.split('/')(4)

    var preparedStatement: PreparedStatement = null

    try {
      preparedStatement = connection.prepareStatement(insertSql)

      preparedStatement.setString(1, fileName)
      preparedStatement.setString(2, stockName)
      preparedStatement.setInt(3, currentYear)
      preparedStatement.setString(4, fileType)
      preparedStatement.setString(5, md5Sum)
      preparedStatement.setTimestamp(6, startTimestamp)
      preparedStatement.setTimestamp(7, endTimestamp)
      preparedStatement.setInt(8, numberRows)
      preparedStatement.setBoolean(9, errors)

      preparedStatement.execute
      preparedStatement.close
    } catch {
      case sqle: SQLException => sqle.printStackTrace
      case e: Exception => e.printStackTrace
    }
  }

  /**
   * Method for insert data into staging table for further processing it with mysql tools
   * @param connection
   * @param stockName
   * @param currentDate
   * @param openPrice
   * @param closePrice
   */
  def insertIntoStagingDB(connection: Connection, stockName: String, currentDate: String, openPrice: Double, closePrice: Double): Unit = {
    val insertSql = """
                      |insert into first_last_prices_per_month_for_each_year
                      |values (?, ?, ?, ?)
                    """.stripMargin

    var preparedStatement: PreparedStatement = null

    try {
      preparedStatement = connection.prepareStatement(insertSql)

      preparedStatement.setString(1, stockName)
      preparedStatement.setString(2, currentDate)
      preparedStatement.setDouble(3, openPrice)
      preparedStatement.setDouble(4, closePrice)

      preparedStatement.execute
      preparedStatement.close
    } catch {
      case sqle: SQLException => sqle.printStackTrace
      case e: Exception => e.printStackTrace
    }
  }
}