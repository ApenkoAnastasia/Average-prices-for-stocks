USE stagingDB;

SET processing_method;
SET hivevar:processing_method;

WITH
    castLastColumn AS
	(
        SELECT stock_name,
               current_day,
               current_month,
               current_year,
               time_utc,
               open_price,
               close_price,
               prev_open,
               prev_close,
               CAST(REPLACE(prev_prev_close,',','.') as decimal(8,4)) AS prev_prev_close
        FROM stagingDB.stagingTableWithPreviousOpenAndTwoClosePrice
    ),
    countRiseInStocks AS
  	(
  		SELECT stock_name,
  	           current_day,
  	           current_month,
  	           current_year,
  	           time_utc,
  	           CASE 
  	           		WHEN '${hivevar:processing_method}' = "o-c" THEN (close_price - prev_open)* 100/ prev_open
  	           		WHEN '${hivevar:processing_method}' = "c-c" THEN (close_price - prev_prev_close)* 100/ prev_prev_close
  	            END AS rise
  		FROM castLastColumn
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
  		SELECT stock_name,
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
INSERT INTO destinationDB.destination_table_increase_in_stocks_Bid
SELECT r.stock_name,
        r.current_year,
        '${hivevar:processing_method}',
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