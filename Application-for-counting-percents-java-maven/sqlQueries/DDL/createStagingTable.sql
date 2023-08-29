USE stock_price_incrementDB;
DROP TABLE IF EXISTS increase_in_stocks;
CREATE TABLE IF NOT EXISTS increase_in_stocks(
    stockName VARCHAR(25) NOT NULL,
    currentYear SMALLINT NOT NULL,
    algorithmType VARCHAR(5) NOT NULL,
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
    totalPerYear DECIMAL(10, 6),
    PRIMARY KEY (stockName, currentYear, algorithmType)
);
