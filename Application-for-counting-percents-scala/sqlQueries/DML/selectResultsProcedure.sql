USE destinationDB;

DROP PROCEDURE IF EXISTS getAveragePercents;

DELIMITER //
CREATE PROCEDURE getAveragePercents(
IN stockName VARCHAR(25)
)
BEGIN

    SELECT *
    FROM destination_table_increase_in_stocks_Bid
    WHERE destination_table_increase_in_stocks_Bid.stock_name = stockName
    ORDER BY current_year;

    SELECT *
    FROM average_per_years_increase_in_stocks_Bid
    WHERE average_per_years_increase_in_stocks_Bid.stock_name = stockName;

END//
DELIMITER ;