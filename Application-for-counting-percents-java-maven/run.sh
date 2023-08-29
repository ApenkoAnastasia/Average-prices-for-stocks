#!/bin/bash
AlgCMD="-at"
DelimCMD="-dl"
FolderCMD="-fl"

# get variables from config.txt
. ./properties/config.txt

function check_connection {
  if mysql -u "$USERNAME" -p"$PASSWORD" -e "USE $DATABASE";
  then
    return 0
  else
      return 1
  fi
}

# create database & table
function setup_db {
  if check_connection $CHECK_CONNECTION -eq 0 ;
  then
    mysql -u "$USERNAME" -p"$PASSWORD" < ./sqlQueries/DML/truncateTables.sql
    mysql -u "$USERNAME" -p"$PASSWORD" < ./sqlQueries/DML/selectResultsProcedure.sql
  else
    mysql -u "$USERNAME" -p"$PASSWORD" < ./sqlQueries/DDL/createDB.sql
    mysql -u "$USERNAME" -p"$PASSWORD" < ./sqlQueries/DDL/createStagingTable.sql
    mysql -u "$USERNAME" -p"$PASSWORD" < ./sqlQueries/DDL/createAveragePercentsTable.sql
    mysql -u "$USERNAME" -p"$PASSWORD" < ./sqlQueries/DML/selectResultsProcedure.sql
  fi
}

setup_db;

# parse arguments from command line
while [ "$#" -gt 0 ];
do
    case $1 in
		-h | --help) 
      echo "usage: run.sh
      -at,--algorithm_type <algorithm_type>   type of algorithm, which will be used
      -dl,--delimiter <delimiter>             delimiter for parsing rows ";
      exit;;
    -at | --algorithm_type) AlgCMD="$1"; ALGORITHMTYPE="$2"; shift;;
    -dl | --delimiter) DelimCMD="$1"; DELIMITER="$2"; shift;;
        *) echo "Wrong parameters. Enter -h or --help for help."; exit ;;
    esac
    shift
done

mvn package
while IFS= read -r folderName; 
do
  java -jar target/Application-for-counting-percents-maven-1.0-SNAPSHOT.jar "$DelimCMD" "$DELIMITER" "$AlgCMD" "$ALGORITHMTYPE" "$FolderCMD" "$folderName"

  mysql -u "$USERNAME" -p"$PASSWORD" < ./sqlQueries/DML/insertAverage.sql
  currentStockName=`echo $folderName |cut -f2 -d "/"`
  mysql -u "$USERNAME" -p"$PASSWORD" -D "$DATABASE" -e "CALL getAveragePercents('$currentStockName')"

done < groupFilesNames.txt

#mvn clean
#rm -rf groupFilesNames.txt
