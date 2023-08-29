USE stagingDB;

SET file_path; 
SET hivevar:file_path;
SET group_name;
SET hivevar:group_name;
SET cur_year;
SET hivevar:cur_year;

LOAD DATA INPATH '${hivevar:file_path}'
INTO TABLE stagingDB.stock_prices_per_each_year
PARTITION(
  stock_name='${hivevar:group_name}',
  current_year='${hivevar:cur_year}'
);
