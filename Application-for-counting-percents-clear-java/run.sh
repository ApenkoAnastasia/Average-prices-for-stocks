#!/bin/bash
FilePattern="Bid"
AlgCMD="-at"
DelimCMD="-dl"
FileCMD="-fp"
UrlCMD="-ur"
UserCMD="-us"
PasswordCMD="-ps"

# get variables from config.txt
. ./config.txt

# create database & table
mysql -u "$USERNAME" -p"$PASSWORD" < ./sqlQueries/DDL/createDB.sql
mysql -u "$USERNAME" -p"$PASSWORD" < ./sqlQueries/DDL/createStagingTable.sql
mysql -u "$USERNAME" -p"$PASSWORD" < ./sqlQueries/DDL/createAveragePercentsTable.sql

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
    -dn | --dir_name) DIRNAME="$2"; shift;;
		-zn | --zip_name) ZIPNAME="$2"; shift;;
    -at | --algorithm_type) AlgCMD="$1"; ALGORITHMTYPE="$2"; shift;;
    -dl | --delimiter) DelimCMD="$1"; DELIMITER="$2"; shift;;
        *) echo "Wrong parameters. Enter -h or --help for help."; exit ;;
    esac
    shift
done

# find all zip files with ZipName, unpake this files into necessary folder
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
  i="./$DIRNAME/$groupFilesName"  

  if grep -Fxq "./$groupFilesName" groupFilesNames.txt
  then
    :
  else
     echo "./$groupFilesName" >> groupFilesNames.txt 
  fi 
 
  if ! [ -d "$i" ]
    then
        mkdir -p $groupFilesName
        mv "./$DIRNAME/$ZIPNAME/$filename" $groupFilesName
    else
        mv "./$DIRNAME/$ZIPNAME/$filename" $groupFilesName    
  fi
done

rm -r "./$DIRNAME"

rm Candles.class Application.class 
javac Candles.java
javac -cp ./commons-cli-1.5.0.jar:./mysql-connector-java-8.0.27.jar:. Application.java

while IFS= read -r folderName; 
do
  cd $folderName
  for file in *
  do
    FilePath="$file";
    cd ../
    # run Application with args
    java -cp ./commons-cli-1.5.0.jar:./mysql-connector-java-8.0.27.jar:. Application "$FileCMD" "./$folderName/$FilePath" \
                                                                                     "$DelimCMD" "$DELIMITER" \
                                                                                     "$AlgCMD" "$ALGORITHMTYPE"
    cd $folderName
  done
  cd ../
done < groupFilesNames.txt

rm -rf groupFilesNames.txt

mysql -u "$USERNAME" -p"$PASSWORD" < ./sqlQueries/DML/getResults.sql
mysql -u "$USERNAME" -p"$PASSWORD" < ./sqlQueries/DML/insertAverage.sql
mysql -u "$USERNAME" -p"$PASSWORD" < ./sqlQueries/DML/getAverage.sql


# make .jar
#jar cfe AverageCoters.jar Application *.class

# run .jar
#java -jar AverageCoters.jar

# rebuild .jar
#jar uf AverageCoters.jar /home/nastyaapenko/Projects/JavaProject/PriceChange/???.class
