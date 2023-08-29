USE stagingDB;
DROP TABLE IF EXISTS stagingDB.stock_prices_per_each_year;
CREATE TABLE IF NOT EXISTS stagingDB.stock_prices_per_each_year(
    time_utc TIMESTAMP,
    open STRING,
    high STRING,
    low STRING,
    close STRING,
    volume STRING
)
PARTITIONED BY(
  stock_name STRING,
  current_year SMALLINT
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ';'
LINES TERMINATED BY '\n'
STORED AS TEXTFILE
tblproperties ("skip.header.line.count"="1");