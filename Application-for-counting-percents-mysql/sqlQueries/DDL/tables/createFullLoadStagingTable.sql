USE stagingDB;
DROP TABLE IF EXISTS first_last_prices_per_month_for_each_year;
CREATE TABLE IF NOT EXISTS first_last_prices_per_month_for_each_year(
    stock_name VARCHAR(50) NOT NULL,
    date_time DATETIME NOT NULL,
    open_price DECIMAL(10, 4),
    close_price DECIMAL(10, 4),
    PRIMARY KEY (stock_name, date_time)
);
