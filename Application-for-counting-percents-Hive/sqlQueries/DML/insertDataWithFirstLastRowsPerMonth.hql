USE stagingDB;
WITH
	parcedTimeWithPrices AS
	(
		SELECT DAY(time_utc) AS curr_day,
			   MONTH(time_utc) AS curr_month,
			   YEAR (time_utc) AS curr_year,
			   time_utc,
			   open,
			   close
	    FROM stagingDB.stock_prices_per_each_year
	),
	first_last_date AS
	(
		SELECT curr_year,
			   curr_month,
			   MAX(time_utc) as max_date,
			   MIN(time_utc) as min_date
	    FROM parcedTimeWithPrices
	    GROUP BY curr_year, curr_month
	),
	first_last_prices_per_month_for_each_year AS
	(
		SELECT stock_name,
			   DAY(time_utc) AS curr_day,
			   MONTH(time_utc) AS curr_month,
			   YEAR (time_utc) AS curr_year,
			   time_utc,
			   open,
			   close
	    FROM stagingDB.stock_prices_per_each_year
	    WHEN time_utc IN(select max_date from first_last_date) or time_utc IN(select min_date from first_last_date)
	),
	dataWithPreviousOpenPrice AS
  	(
  	    SELECT stock_name,
  	           curr_day,
  	           curr_month,
  	           curr_year,
  	           time_utc,
  	           open AS open_price,
  	           close AS close_price,
  	           LAG(open, 1, open)
  	            OVER(PARTITION BY stock_name
  	                 ORDER BY curr_year, curr_month, curr_day) AS prev_open
  	    FROM first_last_prices_per_month_for_each_year
  	),
    dataWithPreviousOpenAndClosePrice AS
  	(
  	    SELECT stock_name,
  	           curr_day,
  	           curr_month,
  	           curr_year,
  	           time_utc,
  	           open_price,
  	           close_price,
  	           prev_open,
  	           LAG(close_price, 1, open_price)
  	            OVER(PARTITION BY stock_name
  	                 ORDER BY curr_year, curr_month, curr_day) AS prev_close
  	    FROM dataWithPreviousOpenPrice
  	),
  	dataWithPreviousOpenAndTwoClosePrice AS
  	(
  	    SELECT stock_name,
  	           curr_day,
  	           curr_month,
  	           curr_year,
  	           time_utc,
  	           REPLACE(open_price,',','.') AS open_price,
  	           REPLACE(close_price,',','.') AS close_price,
  	           REPLACE(prev_open,',','.') AS prev_open,
  	           REPLACE(prev_close,',','.') AS prev_close,
  	           LAG(prev_close, 1, open_price)
  	                OVER(PARTITION BY stock_name
  	                    ORDER BY curr_year, curr_month, curr_day) AS prev_prev_close
  	    FROM dataWithPreviousOpenAndClosePrice
  	)
INSERT INTO stagingDB.stagingTableWithPreviousOpenAndTwoClosePrice
SELECT stock_name,
		curr_day,
		curr_month,
		curr_year,
		time_utc,
		open_price,
		close_price,
		prev_open,
		prev_close,
		prev_prev_close
from dataWithPreviousOpenAndTwoClosePrice;