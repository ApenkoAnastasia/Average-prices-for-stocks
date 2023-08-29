USE destinationDB;
DROP PROCEDURE IF EXISTS calculatePercentsPerEachYear;

DELIMITER //
CREATE PROCEDURE calculatePercentsPerEachYear(
IN alg_type VARCHAR(5)
)
BEGIN
      INSERT INTO destinationDB.destination_table_increase_in_stocks_Bid (stock_name,
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
      																	 total_per_year)
      WITH
      	dataWithPreviousOpenPrice AS
      	(
      	    SELECT stock_name,
      	           date_time,
      	           open_price,
      	           close_price,
      	           LAG(open_price, 1, open_price)
      	            OVER(PARTITION BY stock_name
      	                 ORDER BY date_time) AS prev_open
      	    FROM stagingDB.first_last_prices_per_month_for_each_year
      	),
      	dataWithPreviousOpenAndClosePrice AS
      	(
      	    SELECT stock_name,
      	           date_time,
      	           open_price,
      	           close_price,
      	           prev_open,
      	           LAG(close_price, 1, open_price)
      	            OVER(PARTITION BY stock_name
      	                 ORDER BY date_time) AS prev_close
      	    FROM dataWithPreviousOpenPrice
      	),
      	dataWithPreviousOpenAndTwoClosePrice AS
      	(
      	    SELECT stock_name,
      	           date_time,
      	           open_price,
      	           close_price,
      	           prev_open,
      	           prev_close,
      	           LAG(prev_close, 1, open_price)
      	            OVER(PARTITION BY stock_name
      	                 ORDER BY date_time) AS prev_prev_close
      	    FROM dataWithPreviousOpenAndClosePrice
      	),
      	countRiseInStocks AS
      	(
      		SELECT stock_name,
      	           YEAR(date_time) AS current_year,
      	           MONTH(date_time) AS current_month,
      	           DAY(date_time) AS current_day,
      	           (CASE alg_type
      	           		WHEN "o-c" THEN (close_price - prev_open)* 100/ prev_open
      	           		WHEN "c-c" THEN (close_price - prev_prev_close)* 100/ prev_prev_close
      	            END) AS rise
      		FROM dataWithPreviousOpenAndTwoClosePrice
      	),
      	rowedStocks AS
      	(
      		SELECT stock_name,
      	           current_year,
      	           current_month,
      	           current_day,
      	           rise,
      	           ROW_NUMBER() OVER(PARTITION BY stock_name, current_year, current_month
      	           					 ORDER BY stock_name, current_year, current_month, current_day) rn
      		FROM countRiseInStocks
      	),
      	countTotal AS
      	(
      		SELECT
      			stock_name,
      			current_year,
      	        AVG(rise) as total
      		FROM rowedStocks
      		WHERE rn = 2
      	    GROUP BY current_year, stock_name
      	),
      	resultSet AS
      	(
      	    SELECT stock_name,
      		   current_year,
      		   SUM(CASE WHEN (current_month = 1) THEN rise ELSE NULL END) AS january,
      		   SUM(CASE WHEN (current_month = 2) THEN rise ELSE NULL END) AS february,
      		   SUM(CASE WHEN (current_month = 3) THEN rise ELSE NULL END) AS march,
      		   SUM(CASE WHEN (current_month = 4) THEN rise ELSE NULL END) AS april,
      		   SUM(CASE WHEN (current_month = 5) THEN rise ELSE NULL END) AS may,
      		   SUM(CASE WHEN (current_month = 6) THEN rise ELSE NULL END) AS june,
      		   SUM(CASE WHEN (current_month = 7) THEN rise ELSE NULL END) AS july,
      		   SUM(CASE WHEN (current_month = 8) THEN rise ELSE NULL END) AS august,
      		   SUM(CASE WHEN (current_month = 9) THEN rise ELSE NULL END) AS september,
      		   SUM(CASE WHEN (current_month = 10) THEN rise ELSE NULL END) AS october,
      		   SUM(CASE WHEN (current_month = 11) THEN rise ELSE NULL END) AS november,
      		   SUM(CASE WHEN (current_month = 12) THEN rise ELSE NULL END) AS december
      		FROM rowedStocks
      		WHERE rn = 2
      		GROUP BY stock_name, current_year
      	)
      SELECT r.stock_name,
      	     r.current_year,
      	     alg_type,
      	     r.january,
      	     r.february,
      	     r.march,
      	     r.april,
      	     r.may,
      	     r.june,
      	     r.july,
      	     r.august,
      	     r.september,
      	     r.october,
      	     r.november,
      	     r.december,
      	     c.total
      FROM resultSet AS r
      	 LEFT JOIN
      	 countTotal AS c
      		ON r.current_year = c.current_year AND r.stock_name = c.stock_name;

END//
DELIMITER ;