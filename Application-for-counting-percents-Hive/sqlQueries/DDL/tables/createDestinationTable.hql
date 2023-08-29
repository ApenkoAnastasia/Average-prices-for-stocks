USE destinationDB;
DROP TABLE IF EXISTS destinationDB.destination_table_increase_in_stocks_Bid;
CREATE TABLE IF NOT EXISTS destinationDB.destination_table_increase_in_stocks_Bid(
    stock_name STRING,
    current_year SMALLINT,
    algorithm_type STRING,
    january DECIMAL(8,4),
    february DECIMAL(8,4),
    march DECIMAL(8,4),
    april DECIMAL(8,4),
    may DECIMAL(8,4),
    june DECIMAL(8,4),
    july DECIMAL(8,4),
    august DECIMAL(8,4),
    september DECIMAL(8,4),
    october DECIMAL(8,4),
    november DECIMAL(8,4),
    december DECIMAL(8,4),
    total_per_year DECIMAL(8,4)
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ';'
LINES TERMINATED BY '\n'
STORED AS TEXTFILE;