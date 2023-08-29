#!/bin/bash
FilePattern="Bid"
AlgCMD="-at"
DelimCMD="-dl"
FileCMD="-fp"
UrlCMD="-ur"
UserCMD="-us"
PasswordCMD="-ps"
FolderCMD="-fl"

# get variables from config.txt
. ./properties/config.txt

############# flags (false as default) ##############
DBSetup=0
TruncateTables=0
MoveToStaging=0
InitialLoad=0
FullLoad=0
IncrementalLoad=0

################# set up Databases ######################

function check_connection {
  if mysql -u "$USERNAME" -p"$PASSWORD" -e "USE $DESTINATION_DATABASE";
  then
    return 0
  else
      return 1
  fi
}

# create database & table
function setup_db {
  for FILE in ./sqlQueries/DDL/databases/*
  do
    mysql -u "$USERNAME" -p"$PASSWORD" < $FILE
  done

  for FILE in ./sqlQueries/DDL/tables/*
  do
    mysql -u "$USERNAME" -p"$PASSWORD" < $FILE
  done

  for FILE in ./sqlQueries/DML/procedures/*
  do
    mysql -u "$USERNAME" -p"$PASSWORD" < $FILE
  done
}

function truncateTables {
  if check_connection $CHECK_CONNECTION -eq 0 ;
  then
    for FILE in ./sqlQueries/DML/truncate/*
    do
      mysql -u "$USERNAME" -p"$PASSWORD" < $FILE
    done
  else
    echo "Can't connect to DB. Create DB."
  fi
}
################# functions creation ########################################

function moveSourceDataToStaging {
  # find all zip files with ZipName, unpack this files into necessary folder
  find -type f -name $ZIPNAME.zip > zipfiles.txt
  mkdir $DIRNAME

  while read zipfilePath
  do
    directorypath=${zipfilePath%/*}
    zipfile=${zipfilePath##*/}
    cd $directorypath
    unzip -q $zipfile "*$FilePattern*" -d ../$DIRNAME
  done < zipfiles.txt

  cd ../
  rm -rf zipfiles.txt

  # make file, where will lie files group names for args & move them into folder with their names
  touch groupFilesNames.txt
  FILES="./$DIRNAME/$ZIPNAME/"*csv
  for f in $FILES
  do
    filename="${f##*/}"
    groupFilesName=`echo $filename |cut -f1 -d "_"`
    fileYar=`echo $filename | cut -f4 -d "_"| grep -Eo '[[:digit:]]{4}'`
    year="$fileYar"
    i="./$DIRNAME/$groupFilesName/$year"

    if grep -Fxq "./$STAGING_NAME/$groupFilesName/$year" groupFilesNames.txt
      then
        :
      else
        echo "./$STAGING_NAME/$groupFilesName/$year" >> groupFilesNames.txt
    fi

    if ! [ -d "$i" ]
      then
          mkdir -p $STAGING_NAME/$groupFilesName/$year
          mv "./$DIRNAME/$ZIPNAME/$filename" $STAGING_NAME/$groupFilesName/$year
      else
          mv "./$DIRNAME/$ZIPNAME/$filename" $STAGING_NAME/$groupFilesName/$year
    fi
  done

  rm -r "./$DIRNAME"
}

function runInitialLoad {
#  mkdir output

  # compile without sbt
  #scalac -d output -cp ./lib/config-1.4.1.jar:./lib/mysql-connector-java-8.0.27.jar src/main/scala/LoadDataToStaging.scala

  # compile and make .jar with sbt
  sbt clean
  sbt assembly

  # make .jar
  #jar cfe AverageCoters.jar LoadDataToStaging ./output/*.class

  while IFS= read -r folderName;
  do
  #  scala -cp ./output -cp ./lib/config-1.4.1.jar -cp ./lib/mysql-connector-java-8.0.27.jar org.apenko.app.LoadDataToStaging "$DelimCMD" "$DELIMITER" "$FolderCMD" "$folderName"

  #  or run jar file
  #  java -jar AverageCoters.jar "$DelimCMD" "$DELIMITER" "$FolderCMD" "$folderName"
    java -jar target/scala-3.1.0/*.jar "$DelimCMD" "$DELIMITER" "$FolderCMD" "$folderName"

#    currentStockName=`echo $folderName |cut -f2 -d "/"`
#    mysql -u "$USERNAME" -p"$PASSWORD" -D "$DESTINATION_DATABASE" -e "CALL getResultData('$currentStockName')"

  done < groupFilesNames.txt

  # run procedures for counting percents
  mysql -u "$USERNAME" -p"$PASSWORD" -D "$DESTINATION_DATABASE" -e "CALL calculatePercentsPerEachYear('$ALGORITHMTYPE')"
  mysql -u "$USERNAME" -p"$PASSWORD" -D "$DESTINATION_DATABASE" -e "CALL calculateAveragePercents()"

  #rm -rf groupFilesNames.txt
}

function runFullLoad {
  echo  "We inside full load."
}

function runIncrementalLoad {
  echo  "We inside incremental load."
}

################# parsing arguments ########################################

while [ "$#" -gt 0 ];
do
    case $1 in
		-h | --help)
      echo "usage: run.sh
      -dbs, --dbsetup <number>  write 0 if don't want setup Database, 1 - in other case (0 as default)
      -tr, --truncate_tables <number>  write 0 if don't want truncate whole tables, 1 - in other case (0 as default)
      -mvs, --move_to_staging <number>  write 0 if don't want move data from source to staging, 1 - in other case (0 as default)
      -ini, --initial_load <number>  write 0 if don't want run initial load, 1 - in other case (0 as default)
      -fll, --full_load <number>  write 0 if don't want run full load, 1 - in other case (0 as default)
      -inc, --incremental_load <number>  write 0 if don't want run incremental load, 1 - in other case (0 as default)
      -dn,--dir_name <folder name>   name of folder, where files will be saved
      -zn,--zip_name <zip name>   name of zip file
      -at,--algorithm_type <algorithm_type>   type of algorithm, wich will be used
      -dl,--delimiter <delimiter>             delimiter for parsing rows ";
      exit;;

    -dbs | --dbsetup) DBSetup=$2; shift;;

    -tr | --truncate_tables) TruncateTables=$2; shift;;

    -mvs | --move_to_staging) MoveToStaging=$2; shift;;

    -ini | --initial_load) InitialLoad=$2; shift;;

    -fll | --full_load) FullLoad=$2; shift;;

    -inc | --incremental_load) IncrementalLoad=$2; shift;;

    -dn | --dir_name) DIRNAME="$2"; shift;;

		-zn | --zip_name) ZIPNAME="$2"; shift;;

    -at | --algorithm_type) AlgCMD="$1"; ALGORITHMTYPE="$2"; shift;;

    -dl | --delimiter) DelimCMD="$1"; DELIMITER="$2"; shift;;

        *) echo "Wrong parameters. Enter -h or --help for help."; exit ;;
    esac
    shift
done

################# running functions ########################################

if [ $DBSetup -eq 1 ];
then
  setup_db
  echo "setup db."
else
  echo "don't setup db."
fi

if [ $TruncateTables -eq 1 ];
then
  truncateTables
  echo "we truncate tables."
else
  echo "We don't truncate."
fi

if [ $MoveToStaging -eq 1 ];
then
  moveSourceDataToStaging
else
  echo "We don't move data from source to staging."
fi

if [ $InitialLoad -eq 1 ];
then
  runInitialLoad
else
  echo "We don't run initial load."
fi


if [ $FullLoad -eq 1 ];
then
  runInitialLoad
  echo "run full load."
else
  echo "We don't run full load."
fi

if [ $IncrementalLoad -eq 1 ];
then
  runInitialLoad
  echo "run incremental load."
else
  echo "We don't run incremental load."
fi
