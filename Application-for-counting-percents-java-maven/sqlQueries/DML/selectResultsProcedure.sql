USE stock_price_incrementDB;

DROP PROCEDURE IF EXISTS getAveragePercents;

DELIMITER //
CREATE PROCEDURE getAveragePercents(
IN stock_name VARCHAR(25)
)
BEGIN

    SELECT *
    FROM increase_in_stocks
    WHERE increase_in_stocks.stockName = stock_name
    ORDER BY currentYear;

    SELECT *
    FROM average_per_month
    WHERE average_per_month.stockName = stock_name;

END//
DELIMITER ;