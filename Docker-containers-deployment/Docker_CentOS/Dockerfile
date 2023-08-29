# Download base image ubuntu 20.04.
FROM centos:7

#For testing our containers
COPY /files_for_testing_container/test_docker.sh ./test_docker.sh

# Labels about this image.
LABEL Name="dockerproject_for_centos" 
LABEL Maintainer="nastyaapenko1497 on DockerHub" 
LABEL Version="0.0.1"
LABEL Description="Docker image with CentOS 7 and layers such as: \
Python, Java, Scala, Spark, Mongo, MySQL (also include MC, Git, Nano, some libraries, such as pip)"

# Disable Prompt During Packages Installation
ARG DEBIAN_FRONTEND=noninteractive

# Install first layer and zip, unzip, curl from ubuntu repository
RUN yum update -y && \
    yum install zip unzip -y && \
    yum install curl -y && \
    yum clean all

# Install Python layer (python 3)
RUN yum update -y && \ 
    yum install python3 -y && \
    yum install python3-pip -y && \
    yum clean all

#Install Java layer (with Maven)
RUN yum update -y && \ 
    yum install openjdk-8-jdk -y && \
    yum install openjdk-8-jre -y && \
    yum install maven -y && \
    yum clean all

#Install Scala layer
RUN yum update -y && \ 
    yum install scala -y && \
    yum clean all

#Install Spark layer
RUN yum update -y && \ 
    curl https://archive.apache.org/dist/spark/spark-3.1.1/spark-3.1.1-bin-hadoop3.2.tgz && \
    tar xvf spark-3.1.1-bin-hadoop3.2.tgz && \
    mv spark-3.1.1-bin-hadoop3.2/ /opt/spark && \
    rm spark-3.1.1-bin-hadoop3.2.tgz && \
    yum clean all

#Set Spark variables
ENV SPARK_HOME /opt/spark
ENV PATH $PATH:$SPARK_HOME/bin:$SPARK_HOME/sbin
RUN export SPARK_HOME
RUN export PATH

#Install MySQL layer
RUN curl http://repo.mysql.com/mysql-community-release-el7-5.noarch.rpm && \
    rpm -ivh mysql-community-release-el7-5.noarch.rpm && \
    yum update -y && \
    yum install -y mysql-server && \
    yum update -y mysql-community-release && \
    yum install -y mysql-connector-python && \
    yum clean all

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

#Install MongoDB layer
RUN yum update -y && \ 
    yum install -y mongodb-org && \
    pip3 install pymysql && \
    pip3 install pymongo && \
    yum clean all

#Install Midnight commander and Nano
RUN yum update -y && \ 
    yum install -y mc && \
    yum install -y nano && \
    yum clean all

# Set environment variables.
ENV HOME /root

# Start with bash.
ENTRYPOINT ["bash"]

# Define working directory.
WORKDIR /root