# Download base image ubuntu 20.04.
FROM ubuntu:20.04

# Copy files for testing our containers
COPY /files_for_testing_container/test_docker.sh ./test_docker.sh

# Labels about this image.
LABEL Name="dockerproject_for_ubuntu" 
LABEL Maintainer="nastyaapenko1497 on DockerHub" 
LABEL Version="0.0.1"
LABEL Description="Docker image with Ubuntu 20.04 and layers such as: \
Python, Java, Scala, Spark, Mongo, MySQL (also include MC, Git, Nano, some libraries, such as pip)"

# Disable Prompt During Packages Installation.
ARG DEBIAN_FRONTEND=noninteractive

# Install first layer and zip, unzip, curl from ubuntu repository.
RUN apt-get update -y && \
    apt-get install -y apt-utils && \
    apt-get upgrade -y && \
    apt-get install -y zip unzip && \
    apt-get install -y curl && \
    apt-get clean

# Install Python layer (python 3).
RUN apt-get update -y && \ 
    apt-get upgrade -y && \
    apt-get install -y python3 && \
    apt-get install -y python3-pip && \
    apt-get clean

# Install Java layer (with Maven).
RUN apt-get update -y && \ 
    apt-get upgrade -y && \
    apt-get install -y openjdk-8-jdk && \
    apt-get install -y openjdk-8-jre && \
    apt-get install -y maven && \
    apt-get clean

# # Set environment variables for Java.
# RUN export JAVA_HOME=/usr/lib/jvm/java-8-oracle/ && \
#     export PATH=$PATH:$JAVA_HOME/bin

# Install Scala layer.
RUN apt-get update -y && \ 
    apt-get upgrade -y && \
    apt-get install -y scala && \
    apt-get clean

# Install Spark layer. 
RUN apt-get update -y && \ 
    apt-get upgrade -y && \
    curl -O https://archive.apache.org/dist/spark/spark-3.1.1/spark-3.1.1-bin-hadoop3.2.tgz && \
    tar xvzf spark-3.1.1-bin-hadoop3.2.tgz && \
    mv spark-3.1.1-bin-hadoop3.2/ /opt/spark && \
    rm spark-3.1.1-bin-hadoop3.2.tgz && \
    apt-get clean

# Set environment variables for Spark.
ENV SPARK_HOME /opt/spark
ENV PATH $PATH:$SPARK_HOME/bin:$SPARK_HOME/sbin
ENV PYSPARK_PYTHON /usr/bin/python3
RUN export SPARK_HOME && \
    export PATH && \
    export PYSPARK_PYTHON

# Install MySQL layer.
RUN apt-get update && \ 
    apt-get upgrade -y && \
    apt-get install -y mysql-server && \
    pip3 install pymysql && \
    pip3 install mysql-connector && \
    curl -LO https://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java_8.0.27-1ubuntu20.04_all.deb && \
    dpkg -i mysql-connector-java_8.0.27-1ubuntu20.04_all.deb && \
    rm mysql-connector-java_8.0.27-1ubuntu20.04_all.deb && \
    apt-get clean

# Arguments for creating users
ARG MySQLUser
ARG MySQLPassword
ARG MongoUser
ARG MongoPassword

# Configure MySQL user
RUN service mysql stop && \
	usermod -d /var/lib/mysql/ mysql && \
	service mysql start && \
    service --status-all && \
    mysql -ppassword -e "CREATE USER '${MySQLUser}'@'localhost' IDENTIFIED BY '${MySQLPassword}';\
                         GRANT ALL PRIVILEGES ON * . * TO '${MySQLUser}'@'localhost';\
                         FLUSH PRIVILEGES;SELECT user,authentication_string,plugin,host FROM mysql.user;" 

# Install MongoDB layer.
RUN apt-get update && \ 
    apt-get upgrade -y && \
    apt-get install -y mongodb-server && \
    pip3 install pymongo && \
    apt-get clean && \
    mkdir -p /data/db

# Install layer with Git.
RUN apt-get update -y && \ 
    apt-get upgrade -y && \
    apt-get install -y git && \
    apt-get clean

# Install MC, Nano and etc. layer.
RUN apt-get update -y && \ 
    apt-get upgrade -y && \
    apt-get install -y mc && \
    apt-get install -y nano && \
    apt-get clean

# Set environment variables for MC.
ENV TERM=xterm
RUN export TERM=xterm

# Set environment variables.
ENV HOME /root

# Start with bash.
ENTRYPOINT ["bash"]

# Define working directory.
WORKDIR /root