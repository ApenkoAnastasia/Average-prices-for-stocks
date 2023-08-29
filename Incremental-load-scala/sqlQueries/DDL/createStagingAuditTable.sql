USE staging_auditDB;
DROP TABLE IF EXISTS staging_file_system_audit;
CREATE TABLE IF NOT EXISTS staging_file_system_audit(
    ID BIGINT  UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE,
    file_name VARCHAR(255) NOT NULL,
    stock_name VARCHAR(50) NOT NULL,
    current_year SMALLINT NOT NULL,
    file_type VARCHAR(15) NOT NULL,
    md5_sum VARCHAR(150) NOT NULL,
    start_load_date TIMESTAMP,
    end_load_date TIMESTAMP,
    number_of_processed_rows INT,
    load_with_error BOOL,
    PRIMARY KEY (ID)
);
