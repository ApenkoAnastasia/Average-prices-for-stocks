#!/bin/sh

# Variables for setup.
ImageName="ubuntu-image-test"
ContainerName="test-container "
MySQLUser="root"
MySQLPassword="12344321"
MongoUser="root"
MongoPassword="56788765"

while [ "$#" -gt 0 ]; 
do
    case $1 in
		-h | --help) echo "Run build_docker_image.sh -imn 'your image name' -cn 'your container name' -msu 'your mysql user' -msp 'your mysql password' -mu 'your mongo user' -mp 'your mongo password'. \
								Or --image_name , --container_name, --mysql_user, --mysql_password, --mongo_user, --mongo_password . "; exit;;
		-imn | --image_name) ImageName="$2"; shift;;
		-cn | --container_name) ContainerName="$2"; shift;;
		-msu | --mysql_user) MySQLUser="$2"; shift;;
		-msp | --mysql_password) MySQLPassword="$2"; shift;;
		-mu | --mongo_user) MongoUser="$2"; shift;;
		-mp | --mongo_password) MongoPassword="$2"; shift;;
        *) echo "Wrong parameters. Enter -h or --help for help."; exit ;;
    esac
    shift
done

sudo docker build . -t $ImageName \
							--build-arg MySQLUser=$MySQLUser \
							--build-arg MySQLPassword=$MySQLPassword \
							--build-arg MongoUser=$MongoUser \
							--build-arg MongoPassword=$MongoPassword #\
							#-q

#sudo docker ps -as

sudo docker run -it --name $ContainerName $ImageName #bash

#docker container exec -it $ContainerName bash

#docker container restart $ContainerName
