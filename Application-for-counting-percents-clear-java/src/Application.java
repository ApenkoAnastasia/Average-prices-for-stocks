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

        Option filePath = new Option("fp", "file_path", true, "file path");
        filePath.setArgName("file_path");
        filePath.setRequired(true);
        options.addOption(filePath);

        Option delimiter = new Option("dl", "delimiter", true, "delimeter in file");
        delimiter.setArgName("delimiter");
        delimiter.setRequired(true);
        options.addOption(delimiter);

        Option algType = new Option("at", "algorithm_type", true, "algorithm type");
        algType.setArgName("algorithm_type");
        algType.setRequired(true);
        options.addOption(algType);

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
            cmd.getOptionValue("file_path"),
            cmd.getOptionValue("delimiter"),
            cmd.getOptionValue("algorithm_type")
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
        String stockName = filePath.split("/")[2];

        return stockName;
    }
    
    /**
     * Method for counting prices from files and getting result array
     * @param arguments provided command line arguments
     * @return array of double values containing percents per each month (0.0 if don't have)
     */
    public static double[] countPerMonth(String[] arguments) throws IOException, ParseException, FileNotFoundException, Exception{
       String[] parsedArguments = ParseArgs(arguments);
       String filePath = parsedArguments[0];
       String delimiter = parsedArguments[1];
       Application.algorithmType = parsedArguments[2];
       Application.stockName = getStockName(filePath);

        BufferedReader reader = new BufferedReader(new FileReader(filePath));
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
     * Method for connecting to MySQL database, wich were created earlier
     * @return Connection object with url, user, password from database.properties
     */
    public static Connection getConnection() throws SQLException, IOException{
        Properties props = new Properties();
        try(InputStream in = Files.newInputStream(Paths.get("database.properties"))){
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
                
            System.out.printf("%d rows added", rows);

    }

    /**
     * Program entry point.
     * @param args provided command line arguments, which will be parsed.
     */
    public static void main(String[] args){
        try{ 
            double[] yearPercents = countPerMonth(args);
            double totalYearPercent = getYearTotal(yearPercents);

            try (Connection conn = getConnection()){
                insertIntoMySQL(conn, yearPercents, totalYearPercent);
                conn.close();
            }
            catch(Exception ex){
                System.out.println("Connection failed...");
                System.out.println(ex);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    
    }
}