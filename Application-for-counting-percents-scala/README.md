# Scala application to get average prices per months and years for stocks

## Description

This Scala app get csv files with cotters from 2012 to 2020 and return table with their average and total price change in percents.
It also uses incremental loading to swap new data.


#### Features

- -at | --algorithm_type <algorithm_type>:    type of algorithm, wich will be used
- -dl | --delimiter <delimiter>:             delimiter for parsing rows, default is ;

## Installation
#### Requirements 

Requires JDK 11 to run, Scala 3+, sbt 1.5.5

## Usage

In command line interpreter start program.
Usage examples:

(1) To run with default variables (at = "o-c", dl = ; ):

```sh
$bash runInitialLoad.sh
```  
- To run with your delimiter:
```sh
$bash runInitialLoad.sh -dl ","
``` 
- To run with your algorithm type:
```sh
$sh runInitialLoad.sh -at "c-c"
```

(2) To change database properties change information in /properties/database.properties and /properties/config.txt

(3) Check the results of the program with the results in the folder /results.
