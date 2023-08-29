USE destinationDB;

DROP PROCEDURE IF EXISTS getResultData;

DELIMITER //
CREATE PROCEDURE getResultData(
IN stockName VARCHAR(25)
)
BEGIN

    SELECT stock_name,
         current_year,
         algorithm_type,
         january,
         february,
         march,
         april,
         may,
         june,
         july,
         august,
         september,
         october,
         november,
         december,
         total_per_year
    FROM destination_table_increase_in_stocks_Bid
    WHERE destination_table_increase_in_stocks_Bid.stock_name = stockName;

    SELECT stock_name,
         algorithm_type,
         avg_january,
         avg_february,
         avg_march,
         avg_april,
         avg_may,
         avg_june,
         avg_july,
         avg_august,
         avg_september,
         avg_october,
         avg_november,
         avg_december,
         avg_total
    FROM average_per_years_increase_in_stocks_Bid
    WHERE average_per_years_increase_in_stocks_Bid.stock_name = stockName;

END//
DELIMITER ;