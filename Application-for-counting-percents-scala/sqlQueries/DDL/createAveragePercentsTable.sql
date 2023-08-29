USE destinationDB;
DROP TABLE IF EXISTS average_per_years_increase_in_stocks_Bid;
CREATE TABLE IF NOT EXISTS average_per_years_increase_in_stocks_Bid(
    stock_name VARCHAR(25) NOT NULL,
    algorithm_type VARCHAR(5) NOT NULL,
    avg_january DECIMAL(10, 6),
    avg_february DECIMAL(10, 6),
    avg_march DECIMAL(10, 6),
    avg_april DECIMAL(10, 6),
    avg_may DECIMAL(10, 6),
    avg_june DECIMAL(10, 6),
    avg_july DECIMAL(10, 6),
    avg_august DECIMAL(10, 6),
    avg_september DECIMAL(10, 6),
    avg_october DECIMAL(10, 6),
    avg_november DECIMAL(10, 6),
    avg_december DECIMAL(10, 6),
    avg_total DECIMAL(10, 6),
    PRIMARY KEY (stock_name, algorithm_type)
);
