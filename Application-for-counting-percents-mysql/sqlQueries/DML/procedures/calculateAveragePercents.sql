USE destinationDB;
DROP PROCEDURE IF EXISTS calculateAveragePercents;

DELIMITER //
CREATE PROCEDURE calculateAveragePercents()
BEGIN
      INSERT INTO destinationDB.average_per_years_increase_in_stocks_Bid (stock_name,
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
      																	 avg_total)
      SELECT stock_name,
      	     algorithm_type,
      	     AVG(january),
      	     AVG(february),
      	     AVG(march),
      	     AVG(april),
      	     AVG(may),
      	     AVG(june),
      	     AVG(july),
      	     AVG(august),
      	     AVG(september),
      	     AVG(october),
      	     AVG(november),
      	     AVG(december),
      	     AVG(total_per_year)
      FROM destinationDB.destination_table_increase_in_stocks_Bid
      GROUP BY stock_name, algorithm_type;

END//
DELIMITER ;