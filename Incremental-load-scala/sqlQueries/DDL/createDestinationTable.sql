USE destinationDB;
DROP TABLE IF EXISTS destination_table_increase_in_stocks_Bid;
CREATE TABLE IF NOT EXISTS destination_table_increase_in_stocks_Bid(
    stock_name VARCHAR(25) NOT NULL,
    current_year SMALLINT NOT NULL,
    algorithm_type VARCHAR(5) NOT NULL,
    january DECIMAL(9, 6),
    february DECIMAL(9, 6),
    march DECIMAL(9, 6),
    april DECIMAL(9, 6),
    may DECIMAL(9, 6),
    june DECIMAL(9, 6),
    july DECIMAL(9, 6),
    august DECIMAL(9, 6),
    september DECIMAL(9, 6),
    october DECIMAL(9, 6),
    november DECIMAL(9, 6),
    december DECIMAL(9, 6),
    total_per_year DECIMAL(10, 6),
    PRIMARY KEY (stock_name, current_year, algorithm_type)
);
