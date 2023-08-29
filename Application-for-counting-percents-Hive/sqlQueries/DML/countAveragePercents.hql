USE destinationDB;
INSERT INTO destinationDB.average_per_years_increase_in_stocks_Bid
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