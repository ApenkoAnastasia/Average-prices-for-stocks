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
    mysql -u "$USERNAME" -p"$PASSWORD" < ./sqlQueries/DDL/createStagingDB.sql
    mysql -u "$USERNAME" -p"$PASSWORD" < ./sqlQueries/DDL/createStagingAuditTable.sql
    mysql -u "$USERNAME" -p"$PASSWORD" < ./sqlQueries/DDL/createDestinationDB.sql
    mysql -u "$USERNAME" -p"$PASSWORD" < ./sqlQueries/DDL/createDestinationTable.sql
    mysql -u "$USERNAME" -p"$PASSWORD" < ./sqlQueries/DDL/createDestinationAuditTable.sql
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

mkdir output

# compile without sbt
#scalac -d output -cp ./lib/config-1.4.1.jar:./lib/mysql-connector-java-8.0.27.jar src/main/scala/Application.scala

# compile and make .jar with sbt
sbt clean
sbt assembly

# make .jar
#jar cfe AverageCoters.jar Application ./output/*.class

while IFS= read -r folderName; 
do
#  scala -cp ./output -cp ./lib/config-1.4.1.jar -cp ./lib/mysql-connector-java-8.0.27.jar org.apenko.app.Application "$DelimCMD" "$DELIMITER" "$AlgCMD" "$ALGORITHMTYPE" "$FolderCMD" "$folderName"

#  or run jar file
#  java -jar AverageCoters.jar "$DelimCMD" "$DELIMITER" "$AlgCMD" "$ALGORITHMTYPE" "$FolderCMD" "$folderName"
  java -jar target/scala-3.1.0/objectScala-assembly-0.1.jar "$DelimCMD" "$DELIMITER" "$AlgCMD" "$ALGORITHMTYPE" "$FolderCMD" "$folderName"

  currentStockName=`echo $folderName |cut -f2 -d "/"`
  mysql -u "$USERNAME" -p"$PASSWORD" -D "$DATABASE" -e "CALL getAveragePercents('$currentStockName')"

done < groupFilesNames.txt

#rm -rf groupFilesNames.txt
