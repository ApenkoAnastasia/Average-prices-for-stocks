USE stagingDB;
DROP TABLE IF EXISTS stagingDB.stagingTableWithPreviousOpenAndTwoClosePrice;
CREATE TABLE IF NOT EXISTS stagingDB.stagingTableWithPreviousOpenAndTwoClosePrice(
    stock_name STRING,
    current_day SMALLINT,
    current_month SMALLINT,
    current_year SMALLINT,
    time_utc TIMESTAMP,
    open_price DECIMAL(8,4),
    close_price DECIMAL(8,4),
    prev_open DECIMAL(8,4),
    prev_close DECIMAL(8,4),
    prev_prev_close  STRING
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ';'
LINES TERMINATED BY '\n'
STORED AS TEXTFILE;