package org.apenko.app

import java.io._

import scala.io.Source
//import scala.collection.mutable.ListBuffer
import scala.sys.process._

/**
 * @autor Anastasiya Apenko
 * @version 1.0
 */
object SplitFileByMonth {
  // global variables
  var previousYearClosePrice: Double = Double.NaN
  var previousYear: Int = 0 // for c-c algorithm
  var stockName = ""

  /**
   * Program entry point.
   * @param args provided command line arguments, which will be parsed.
   */
  def main(args: Array[String]): Unit = {
    // get arguments, list of files
    val (delimiter, folderPath) = getParsedArguments(args)
    val filesList = getListOfFiles(folderPath)

    processData(filesList, delimiter)

  }

  /**
   * Method for parsing input arguments
   * @param args provided command line arguments
   * @return tuple of strings with arguments <b>file_path</b>, <b>delimiter</b>, <b>algorithm_type</b>
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
   * @return tuple of int - year, int - month, double - open price, double - close price
   */
  def parseRow(row: String, delimiter: String): (String, String) = {
    var fields = row.split(delimiter)

    var dateWithTime = fields(0).split(" ")
    var clearDate = dateWithTime(0)
    var currentYear = clearDate.split("-")(0)
    var currentMonth = clearDate.split("-")(1)

    (currentYear, currentMonth)
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
   *
   * @param filesList
   * @param delimiter
   */
  def processData(filesList: List[File], delimiter: String): Unit = {
    for (path <- filesList) {
      val splittedPath = getSplittedPath(path.toString)
      val currentStockName = splittedPath(2)
      val currentYear = splittedPath(3).toInt
      readAndSplitFile(splittedPath, path.toString, delimiter)


      stockName = currentStockName
      println(stockName, currentYear)
    }

  }

  /**
   *
   * @param splittedPath
   * @param filePath
   * @param delimiter
   */
  def readAndSplitFile(splittedPath: Array[String], filePath: String, delimiter: String) = {
    val bufferedSource = Source.fromFile(filePath)
    var writer: PrintWriter = null

    var currentMonth = 0
    var previousMonth = 0

    for (line <- bufferedSource.getLines.drop(1)) {
      var (strCurrentYear, strCurrentMonth) = parseRow(line, delimiter)
      currentMonth = strCurrentMonth.toInt

      // name and path to new files per each month
      val newPath = createNewPath(splittedPath, "/", strCurrentMonth)
      val newFileName = createNewFileName(splittedPath, strCurrentMonth)
      val pathToNewFile = newPath + "/" + newFileName
      val folder = new File(newPath)
      val newFile = new File(pathToNewFile)


      if (currentMonth == previousMonth) {
        writeIntoFile(writer, line)
      }
      else{
        if (folder.exists && folder.isDirectory){
          println(s"$folder exists.")
          writeIntoFile(writer, line)
        }
        else {
          if (writer != null) writer.close
          println(s"$folder DOESN'T exists.")
          createNewFolder(newPath)
          createNewFile(pathToNewFile)

          writer = new PrintWriter(newFile)
          writeIntoFile(writer, line)
        }
        previousMonth = currentMonth
      }
    }
    writer.close
    bufferedSource.close

  }

  /**
   *
   * @param splittedPath
   * @param delimiterForNewFilePath
   * @param month
   * @return
   */
  def createNewPath(splittedPath: Array[String], delimiterForNewFilePath: String, month: String): String = {
    try{
      val newPath = splittedPath(0) + delimiterForNewFilePath +
                    splittedPath(1) + delimiterForNewFilePath +
                    splittedPath(2) + delimiterForNewFilePath +
                    splittedPath(3) + delimiterForNewFilePath +
                    month
      newPath
    }
    catch {
      case e: Exception => throw new Exception("Exception in createNewPath method: " + e.getMessage)
    }
  }

  /**
   *
   * @param splittedPath
   * @param month
   * @return
   */
  def createNewFileName(splittedPath: Array[String], month: String): String = {
    val delimiter = "_"
    val extension = ".csv"
    try{
      val newFileName = splittedPath(2) + delimiter +
                        splittedPath(3) + delimiter +
                        month + extension
      newFileName
    }
    catch {
      case e: Exception => throw new Exception("Exception in createNewFileName method: " + e.getMessage)
    }
  }

  /**
   *
   * @param newPath
   */
  def createNewFolder(newPath: String): Unit = {
    Seq("mkdir", newPath).!
  }

  /**
   *
   * @param pathToNewFile
   */
  def createNewFile(pathToNewFile: String):Unit = {
    Seq("touch", pathToNewFile).!
  }

  /**
   *
   * @param writer
   * @param line
   */
  def writeIntoFile(writer: PrintWriter, line: String): Unit = {
    //(s"echo $line" #>> newFile ).!  or through bash
    writer.write(line+"\n")
  }

}

