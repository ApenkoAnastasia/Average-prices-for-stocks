import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.text.ParseException;

/**
 * Class Candles with variables <b>time</b>, <b>open</b> and <b>close</b>.
 * @autor Anastasiya Apenko
 * @version 1.0
 */
public class Candles {
    private Calendar time;
    private Double open;
    private Double close;

    /**
     * Constructor of class
     * @see Candles#Candles(String, String, String)
     */
    public Candles(String time, String openPrice, String closePrice) throws ParseException{
        this.setTime(time);
        this.setOpen(openPrice);
        this.setClose(closePrice);
    }

    /**
     * Method for getting result of variable {@link Candles#time}
     * @return value of current time
     */
    public Calendar getTime() {
        return this.time;
    }

    /**
     * Method for setting value of variable {@link Candles#time}
     * @param columnTime - string with current time from read file
     */
    public void setTime(String columnTime) throws ParseException {
        Calendar date = GregorianCalendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        date.setTime(formatter.parse(columnTime));
        this.time = date;
    }

    /**
     * Method for getting result of variable {@link Candles#open}
     * @return value of current open price
     */
    public Double getOpen() {
        return this.open;
    }

    /**
     * Method for setting value of variable {@link Candles#open}
     * @param columnOpen - string with open price from read file
     */
    public void setOpen(String columnOpen) {
        try{
            this.open = Double.parseDouble(columnOpen.replace(",","."));
        } catch (Exception e) {

            System.out.println("Exception: " + e);

        }
    }

    /**
     * Method for getting result of variable {@link Candles#close}
     * @return value of current close price
     */
    public Double getClose() {
        return this.close;
    }

    /**
     * Method for setting value of variable {@link Candles#close}
     * @param columnClose - string with current close price from read file
     */
    public void setClose(String columnClose) {
        try{
            this.close = Double.parseDouble(columnClose.replace(",","."));
        } catch (Exception e) {

            System.out.println("Exception: " + e);

        }
    }
}