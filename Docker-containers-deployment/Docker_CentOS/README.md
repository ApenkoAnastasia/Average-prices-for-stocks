# Dockerfile to build image with CentOS and other tools

## Description

Dockerfile download empty image with CentOS 
and then set up Java, Python, Scala, Mongo, Spark, MySQL (also include MC, Git, Nano, some libraries, such as pip).
Allows to adjust the necessary layers and not to adjust unnecessary packages.


#### Features
 - -imn | --image_name: "Set your image name. Default: ubuntu_image_test"
 - -cn | --container_name: "Set your cantainer name. Default: test_container" 
 - -h | --help: "Give help information"
 - -msu | --mysql_user: "Set your mysql user" 
 - -msp | --mysql_password: "Set your mysql password"
 - -mu | --mongo_user: "Set your mongo user"
 - -mp | --mongo_password: "Set your mongo password"

## Installation
#### Requirements 

Requires Docker to run.
## Usage

In command line interpreter start programm.
Usage examples:

(1) To start build image and create container:

```sh
$sh build_docker_image.sh -imn image_name -cn container_name -msu mysql_user -msp mysql_password -mu mongo_user -mp mongo_password
```  

(2) To test docker container run test_docker.sh from files_for_testing folder:

```sh
$sh /test_docker.sh
``` 
All necessary user requirements will be installed through building an image. But if you want to create new user for MySQL or MongoDB go to the next steps.

(3) To configure MySQL server open /instructions/MySQL_setup.txt and follow the instructions from the file in bash.

(4) To configure MongoDB open /instructions/MongoDB_setup.txt and follow the instructions from the file in bash.