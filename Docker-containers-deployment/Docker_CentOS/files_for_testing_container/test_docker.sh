#!/bin/sh

echo "Hello-docker! We are inside!"

#----- test OS tools
echo "Testing libraries: "

if [ -e /tmp/Datasets/ml-latest-small/movies.csv ]; then 
	echo "File csv exists."
elif [ -e /tmp/ml-latest-small.zip ]; then 
	unzip /tmp/ml-latest-small.zip \*movies -d /tmp/Datasets 
	rm /tmp/ml-latest-small.zip
else 
	curl -P /tmp https://files.grouplens.org/datasets/movielens/ml-latest-small.zip 
	unzip /tmp/ml-latest-small.zip \ml-latest-small/movies.csv -d /tmp/Datasets 
	rm /tmp/ml-latest-small.zip
fi 

#curl -o /tmp/small.zip https://files.grouplens.org/datasets/movielens/ml-latest-small.zip
#unzip /tmp/small.zip \*movies.csv -d /tmp/Datasets
cd /tmp/Datasets/ml-latest-small
head movies.csv 
#rm /tmp/small.zip 
#rm /tmp/Datasets

#----- test Python
echo "Testing Python: "
python3 -V
pip3 -V

#----- test Java
echo "Testing Java: "
java -version
javac -version

#----- test Maven
echo "Testing Maven: "
mvn -version

#----- test Scala
echo "Testing Scala: "
scala -version

#----- test Spark
#echo "Testing Spark: "
#spark-shell
#exit()
#pyspark
#quit()

#----- test MongoDB
#echo "Testing MongoDb: "
#systemctl status mongod
#mongo
#version()
#quit()
