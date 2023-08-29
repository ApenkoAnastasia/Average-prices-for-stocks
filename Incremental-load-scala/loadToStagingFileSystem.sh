#!/bin/bash
FilePattern="Bid"
AlgCMD="-at"
DelimCMD="-dl"
FileCMD="-fp"
UrlCMD="-ur"
UserCMD="-us"
PasswordCMD="-ps"
FileName="XAUUSD_10 Secs_Bid_2020.01.01_2020.07.17.csv"

# get variables from config.txt
. ./properties/config.txt

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

##########################################################################

# parse arguments from command line
while [ "$#" -gt 0 ];
do
    case $1 in
		-h | --help)
      echo "usage: unzip_csv_files.sh
      -dn,--dir_name <folder name>   name of folder, where files will be saved
      -zn,--zip_name <zip name>   name of zip file
      -at,--algorithm_type <algorithm_type>   type of algorithm, wich will be used
      -dl,--delimiter <delimiter>             delimiter for parsing rows ";
      exit;;
	 -dbs|--dbsetup)
      if [ $2 -eq 1] 
        then
          db_setup
        else
          :
      fi
      shift;;
    -dn | --dir_name) DIRNAME="$2"; shift;;
		-zn | --zip_name) ZIPNAME="$2"; shift;;
    -at | --algorithm_type) AlgCMD="$1"; ALGORITHMTYPE="$2"; shift;;
    -dl | --delimiter) DelimCMD="$1"; DELIMITER="$2"; shift;;
        *) echo "Wrong parameters. Enter -h or --help for help."; exit ;;
    esac
    shift
done

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
  i="./$DIRNAME/$groupFilesName/$fileYear"

  if grep -Fxq "./$STAGING_NAME/$groupFilesName" groupFilesNames.txt
    then
      :
    else
      echo "./$STAGING_NAME/$groupFilesName" >> groupFilesNames.txt
  fi

  if ! [ -d "$i" ]
    then
        mkdir -p $STAGING_NAME/$groupFilesName/$fileYear
        mv "./$DIRNAME/$ZIPNAME/$filename" $STAGING_NAME/$groupFilesName/$fileYear
    else
        mv "./$DIRNAME/$ZIPNAME/$filename" $STAGING_NAME/$groupFilesName/$fileYear
  fi
done

rm -r "./$DIRNAME"


#################### split file into smaller for incremental load #################
cd ./sourceFiles
split $FileName -l 8641 "$FileName_" -d
cd ../