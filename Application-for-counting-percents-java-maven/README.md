# Java application to get average prices per monthes and years

## Description

This Java app get csv files with coters from 2012 to 2020 and return table with their average and total price change in percents.

This project use Maven to make all dependencies.

#### Features

- -at | --algorithm_type <algorithm_type>:    type of algorithm, wich will be used
- -dl | --delimiter <delimiter>:             delimiter for parsing rows, default is ;

## Installation
#### Requirements 

Requires JDK 11 to run, Maven 3.6.3

## Usage

In command line interpreter start program.
Usage examples:

(1) To run with default variables (at = "o-c", dl = ; ):

```sh
$bash run.sh
```  
- To run with your delimiter:
```sh
$bash run.sh -dl ","
``` 
- To run with your algorithm type:
```sh
$sh run.sh -at "c-c"
```
(2) To change database properties change information in /properties/database.properties and /properties/config.txt

(3) Check the results of the program with the results in the folder /results.
