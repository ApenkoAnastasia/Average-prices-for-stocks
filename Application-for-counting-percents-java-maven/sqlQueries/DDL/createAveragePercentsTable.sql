USE stock_price_incrementDB;
DROP TABLE IF EXISTS average_per_month;
CREATE TABLE IF NOT EXISTS average_per_month(
    stockName VARCHAR(25) NOT NULL,
    algorithmType VARCHAR(5) NOT NULL,
    avgJanuary DECIMAL(10, 6),
    avgFebruary DECIMAL(10, 6),
    avgMarch DECIMAL(10, 6),
    avgApril DECIMAL(10, 6),
    avgMay DECIMAL(10, 6),
    avgJune DECIMAL(10, 6),
    avgJuly DECIMAL(10, 6),
    avgAugust DECIMAL(10, 6),
    avgSeptember DECIMAL(10, 6),
    avgOctober DECIMAL(10, 6),
    avgNovember DECIMAL(10, 6),
    avgDecember DECIMAL(10, 6),
    avgTotal DECIMAL(10, 6),
    PRIMARY KEY (stockName, algorithmType)
);
