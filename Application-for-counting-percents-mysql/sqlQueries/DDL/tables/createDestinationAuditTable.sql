USE destinationDB;
DROP TABLE IF EXISTS audit_increase_in_stocks;
CREATE TABLE IF NOT EXISTS audit_increase_in_stocks(
    ID BIGINT  UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE,
    stock_name VARCHAR(50) NOT NULL,
    algorithm_type VARCHAR(5) NOT NULL,
    current_year SMALLINT NOT NULL,
    file_type VARCHAR(15) NOT NULL,
    task_name VARCHAR(50) NOT NULL,
    start_load_date TIMESTAMP,
    end_load_date TIMESTAMP,
    number_of_processed_rows INT,
    load_with_error BOOL,
    PRIMARY KEY (ID)
);
