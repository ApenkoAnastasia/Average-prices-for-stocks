#!/bin/bash

############## create databases ###################
hdfs dfs -ls -C /user/DDL/databases > create_databases.txt

while read line; do
  hive -f "$line"
done < create_databases.txt
###################################################

############## create tables ###################
hdfs dfs -ls -C /user/DDL/tables > create_tables.txt

while read line; do
  hive -f "$line"
done < create_tables.txt
###################################################

############## run algorithms ###################

hdfs dfs -ls -C /user/staging_file_system > file_path.txt

while read line; do
  hive -hivevar file_path="$line" -hivevar group_name=`echo $line | cut -f3 -d "/" | cut -f1 -d "_"` \
  -hivevar cur_year=`echo $line | cut -f3 -d "/" | cut -f4 -d "_"| grep -Eo '[[:digit:]]{4}'` \
  -f "/user/DDL/loadDataIntoStagingTable.hql"
done < file_path.txt

 hive -f "/user/DDL/insertDataWithFirstLastRowsPerMonth.hql"

 hive -hivevar processing_method="c-c" -f "/user/DDL/countPercents.hql"

 hive -hivevar processing_method="o-c" -f "/user/DDL/countPercents.hql"

 hive -f "/user/DDL/countAveragePercents.hql"
###################################################