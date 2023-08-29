USE destinationDB;
DROP TABLE IF EXISTS destinationDB.average_per_years_increase_in_stocks_Bid;
CREATE TABLE IF NOT EXISTS destinationDB.average_per_years_increase_in_stocks_Bid(
    stock_name STRING,
    algorithm_type STRING,
    avg_january DECIMAL(8,4),
    avg_february DECIMAL(8,4),
    avg_march DECIMAL(8,4),
    avg_april DECIMAL(8,4),
    avg_may DECIMAL(8,4),
    avg_june DECIMAL(8,4),
    avg_july DECIMAL(8,4),
    avg_august DECIMAL(8,4),
    avg_september DECIMAL(8,4),
    avg_october DECIMAL(8,4),
    avg_november DECIMAL(8,4),
    avg_december DECIMAL(8,4),
    avg_total DECIMAL(8,4)
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ';'
LINES TERMINATED BY '\n'
STORED AS TEXTFILE;