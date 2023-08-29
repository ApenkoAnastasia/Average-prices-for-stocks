import org.apache.commons.cli.*;
import java.sql.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.text.ParseException;

/**
 * Main class Application with global variables <b>stockName</b>, <b>currentYear</b> and <b>algorithmType</b>.
 * This is an application for obtaining changes in stock prices for a necessary year in a specific subgroup.
 * Expressed in percents.
 * It also calculates the average value for the current year.
 * This project use Maven to make all dependencies.
 * @autor Anastasiya Apenko
 * @version 1.0
 */
public class Application {
    public static String stockName;
    public static int currentYear = 0;
    public static String algorithmType;

    /**
     * Method for parsing input arguments
     * @param arguments provided command line arguments
     * @return array of strings with arguments <b>file_path</b>, <b>delimiter</b>, <b>algorithm_type</b>
     */
    public static String [] ParseArgs(String[] arguments){
        Options options = new Options();

        Option delimiter = new Option("dl", "delimiter", true, "delimeter in file");
        delimiter.setArgName("delimiter");
        delimiter.setRequired(true);
        options.addOption(delimiter);

        Option algType = new Option("at", "algorithm_type", true, "algorithm type");
        algType.setArgName("algorithm_type");
        algType.setRequired(true);
        options.addOption(algType);

        Option folderPath = new Option("fl", "folder_path", true, "path to folder with files");
        folderPath.setArgName("folder_path");
        folderPath.setRequired(true);
        options.addOption(folderPath);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, arguments);
        } catch (org.apache.commons.cli.ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Application", options);
            System.exit(1);
        }

        return new String[]{
                cmd.getOptionValue("delimiter"),
                cmd.getOptionValue("algorithm_type"),
                cmd.getOptionValue("folder_path")
        };
    }

    /**
     * Method for getting necessary variable for algorithm
     * @param closeFirst first close price of current month
     * @param openFirst first open price of current month
     * @return double value for algorithm
     */
    public static double getCorrectVariable(double closeFirst, double openFirst) throws Exception{
        double variable;
        switch(Application.algorithmType){
            case "o-c":
                variable = openFirst;
                break;
            case "c-c":
                variable = closeFirst;
                break;
            default:
                variable = openFirst;
        }

        return variable;
    }

    /**
     * Method for getting percents per current month
     * @param closeFirst first close price of current month
     * @param closeLast last close price of current month
     * @param openFirst first open price of current month
     * @return double value of percents per current month
     */
    public static double countPercents(double closeFirst, double closeLast, double openFirst){
        double monthResult = 0.0;
        try{
            monthResult = (closeLast - getCorrectVariable(closeFirst, openFirst)) * 100 / getCorrectVariable(closeFirst, openFirst);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return monthResult;
    }

    /**
     * Method for getting percents per whole current year
     * @param percents array of double values (percents) for each month in this year
     * @return double value of percents per whole year
     */
    public static double getYearTotal(double[] percents){
        double sum = 0.0;
        for(double percent: percents){
            sum += percent;
        }
        double total = sum / percents.length;

        return total;
    }

    /**
     * Method for getting name of group for counting
     * @param filePath string value containing path to the file
     * @return string value with group name
     */
    public static String getStockName(String filePath){
        String stockName = filePath.split("/")[1];

        return stockName;
    }

    /**
     * Method for counting prices from files and getting result array
     * @param delimiter provided delimiter for files
     * @param pathName path to the file
     * @return array of double values containing percents per each month (0.0 if don't have)
     */
    public static double[] countPerMonth(String delimiter, String pathName) throws IOException, ParseException, FileNotFoundException, Exception{

        BufferedReader reader = new BufferedReader(new FileReader(pathName));
        String line = null;
        // skip header
        String headerLine = reader.readLine();

        double[] results = new double[12];
        int monthIndex = 0; // position inside results array
        int currentMonthIndex = 0; // for first line flag

        int currentMonth = 0;
        int previousMonth = 0;

        double currentMonthOpenPrice = 0.0;
        double currentMonthFirstClosePrice = 0.0;
        double currentMonthLastClosePrice = 0.0;

        while ((line = reader.readLine()) != null) {
            String[] fields = line.split(delimiter);

            String time = fields[0];
            String openPrice = fields[1];
            String closePrice = fields[4];

            Candles candle = new Candles(time, openPrice, closePrice);

            currentMonth = candle.getTime().get(Calendar.MONTH);
            Application.currentYear = candle.getTime().get(Calendar.YEAR);

            if (currentMonthIndex == 0){
                previousMonth = currentMonth;
                currentMonthOpenPrice = candle.getOpen();
                currentMonthFirstClosePrice = candle.getClose();
            }

            if (currentMonth == previousMonth) {
                currentMonthIndex++;
            }
            else if (currentMonth - previousMonth > 1){  // when miss a few months
                results[monthIndex] = countPercents(currentMonthFirstClosePrice, currentMonthLastClosePrice, currentMonthOpenPrice);
                int missedMonthAmount = currentMonth - previousMonth;
                for (int i = 1; i <= missedMonthAmount; i++){
                    results[previousMonth+i] = 0.0;
                }
                monthIndex += missedMonthAmount;
                previousMonth = currentMonth;
            }
            else {
                results[monthIndex] = countPercents(currentMonthFirstClosePrice, currentMonthLastClosePrice, currentMonthOpenPrice);
                monthIndex++;
                currentMonthOpenPrice = candle.getOpen();
                currentMonthFirstClosePrice = candle.getClose();
                previousMonth = currentMonth;
            }
            currentMonthLastClosePrice = candle.getClose();
        }
        reader.close();
        results[monthIndex] = countPercents(currentMonthFirstClosePrice, currentMonthLastClosePrice, currentMonthOpenPrice);

        return results;
    }

    /**
     * Method for connecting to MySQL database, which were created earlier
     * @return Connection object with url, user, password from database.properties
     */
    public static Connection getConnection() throws SQLException, IOException{
        Properties props = new Properties();
        try(InputStream in = Files.newInputStream(Paths.get("./properties/database.properties"))){
            props.load(in);
        }
        String url = props.getProperty("url");
        String username = props.getProperty("user");
        String password = props.getProperty("password");

        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Method for inserting result values into MySQL table from file database.properties
     * @param connection Connection object with url, user, password from database.properties
     * @param yearPercents array of double values, which are percents per each month
     * @param totalYearPercent double value of average meaning per current year
     */
    public static void insertIntoMySQL(Connection connection, double[] yearPercents, double totalYearPercent) throws SQLException, IOException{
        String insertQuery = "INSERT INTO increase_in_stocks Values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);

        preparedStatement.setString(1, Application.stockName);
        preparedStatement.setInt(2, Application.currentYear);
        preparedStatement.setString(3, Application.algorithmType);
        for (int i = 4; i < 16; i++){
            preparedStatement.setDouble(i, yearPercents[i-4]);
        }
        preparedStatement.setDouble(16, totalYearPercent);

        int rows = preparedStatement.executeUpdate();

        //System.out.printf("%d rows added", rows);

    }

    /**
     * Program entry point.
     * @param args provided command line arguments, which will be parsed.
     */
    public static void main(String[] args){
        try{
            String[] parsedArguments = ParseArgs(args);
            String delimiter = parsedArguments[0];
            Application.algorithmType = parsedArguments[1];
            String folderPath = parsedArguments[2];
            Application.stockName = getStockName(folderPath);

            String[] pathNames;
            File fileDir = new File(folderPath);
            pathNames = fileDir.list();

            for (String pathName : pathNames) {
                pathName = folderPath + "/" + pathName;

                double[] yearPercents = countPerMonth(delimiter, pathName);
                double totalYearPercent = getYearTotal(yearPercents);

                try (Connection conn = getConnection()){
                    insertIntoMySQL(conn, yearPercents, totalYearPercent);
                }
                catch(Exception ex){
                    System.out.println("Connection failed...");
                    System.out.println(ex);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}