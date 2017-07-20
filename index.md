# Validatar

[![Build Status](https://travis-ci.org/yahoo/validatar.svg?branch=master)](https://travis-ci.org/yahoo/validatar) [![Coverage Status](https://coveralls.io/repos/yahoo/validatar/badge.svg?branch=master)](https://coveralls.io/r/yahoo/validatar?branch=master) [![Download](https://api.bintray.com/packages/yahoo/maven/validatar/images/download.svg)](https://bintray.com/yahoo/maven/validatar/_latestVersion)

## Table of Contents

* [What is Validatar?](#what-is-validatar)
* [Using Validatar](#using-validatar)
	* [Test File Format](#test-file-format)
	* [Assertions](#assertions)
		* [Assertion Format](#assertion-format)
		* [Examples](#examples)
	* [Parameter Substitution](#parameter-substitution)
* [Execution Engines](#execution-engines)
	* [Hive](#hive)
	* [Pig](#pig)
	* [REST](#rest)
	* [CSV](#csv-and-other-delimited-text-data)
* [How to Install](#how-to-install)
	* [Direct Download](#direct-download)
	* [Maven](#maven)
	* [Gradle](#gradle)
* [How to Run](#how-to-run)
	* [Running Hive Tests](#running-hive-tests)
	* [Running Pig Tests](#running-pig-tests)
* [Pluggability](#pluggability)
* [Help](#help)
* [Contributing](#contributing)
* [Changelog](#changelog)

## What is Validatar?

* A Functional Testing Framework for Big Data
* Lets you define how to read your data and what the tests are using a simple YAML file (or folder of files)
* Talks to various data sources through Hive, Pig, or REST based endpoints
* Reads and models data from highly variable datasources as a standard columnar (table) format
* Lets you write powerful assertions on this data. You can join, filter and run comparisons on your data
* Is fully typed and preserves the types of your data sources
* Generates test reports that can be published in CI environments (currently the JUnit format is supported)
* Is completely modular and pluggable. You can easily extend and add new datasources, input sources, output reports etc.

The data sources we currently support:

* Hive (HiveServer2)
* Pig (PigServer)
* Generic REST endpoint (for datasources like Druid etc)
* Static data (CSV, TSV, etc)

## Using Validatar

### Test file format

Test files are written in the YAML format. See examples of all different datasources in src/test/resources/. The schema is as follows:

```
name: String describing the Test Suite name
description: String describing the Test Suite
queries:
   - name: String containing the unique name for the query
     engine: String telling Validatar what execution engine to use such as "hive" or "pig" or "rest"
     value: String describing the engine specific method to get the data such as "SELECT COUNT(*) AS pv_count FROM page_data" for hive
     metadata:
        - key: String key of the metadata entry containing query specific options for the engine
          value: String value of the metadata entry containing query specific options for the engine
   ...
tests:
   - name: String describing the Test name
     description: String descrbing the Test
     asserts:
        - A String assertion statement referencing data from the queries. See below for exact details on Validatar assert statements.
   ...
```

Queries must have unique names. This name is used as a namespace for all the values returned from the query. In the above example, if the name of the query was "Analytics" and it stored a column called "count", then you would be able to use this in your test asserts as "Analytics.count".

Validatar can run a single test file or a folder of test files. Use the --help option to see more details or refer to the Help section below.

### Assertions

This section describes the asserts that you can write in the test section in a Validatar test.  Validatar assertions are quite flexible, allowing for the following operations on your data:

```
                   >  : greater than
                   >= : greater or equal to
                   <  : less than
                   <= : less or equal to
                   == : equal to
                   != : not equal to
                   +  : add
                   -  : subtract
                   *  : multiply
                   /  : divide
                   && : boolean and
                   || : boolean or
approx(a, b, percent) : true if a and b within percent difference (0.0 to 1.0) of each other.
```

#### Assertion format

A Validatar assertion is an expression similar to ones in C or Java where binary operations from above can combined with parantheses etc to produce an expression that evaluates to true or false. An assertion can optionally contain a ```where``` clause that can filter or join multiple datasets. This where clause is provided after the expression and its syntax is the same as the assert itself. So you can leverage the full power of Validatar's assertion expressions to filter and join your datasets as well. See below for [examples](#examples).

Validatar detects the datasets used in your assertion statement and performs automatic **cartesian products** for them. The resulting dataset is what is used for your asserts. The where section can be used to perform a filter on this resulting cartesian product. In other words, if you have a single dataset used in your assert, then including a where lets you perform a **filter** on the dataset. If you have multiple datasets, the where clause is letting you perform a **join** on the dataset.

Your assertion can omit the ```where``` clause and simply assert using the operations above. For the examples below, let us pretend we had the following two queries, A and B, that were run against Hive and produced the data as below.

#### Examples

Query: A

|   date   | country | views | clicks |
|----------|---------|-------|--------|
| 20170101 | us      | 10000 | 124    |
| 20170101 | uk      | 4340  | 14     |
| 20170101 | fr      | 4520  | 0      |
| 20170101 | cn      | 99999 | 1024   |
| 20170101 | eg      | 100   | 24     |
| 20170102 | us      | 9900  | 328    |
| 20170102 | uk      | 2340  | 13     |
| 20170102 | fr      | 4313  | 20     |
| 20170102 | cn      | 97345 | 2034   |
| 20170102 | eg      | 100   | 24     |
| 20170102 | sa      | 0     | 2      |

Query: B

| country | continent | threshold | expected |
|---------|-----------|-----------|----------|
| us      | na        | 0.01      | 10090    |
| uk      | eu        | 0.1       | 4100     |
| fr      | eu        | 0.0       | 4500     |
| cn      | as        | 0.05      | 100000   |
| eg      | af        | 0.15      | 110      |
| sa      | af        | 0.1       | 10       |
| au      | au        | 0.2       | 5        |

Validatar would be modeling the data as two tables, A and B with the columns and their values as shown above.

##### Example 1

```
    A.clicks >= 0 && A.views >= 0
```

This assert is making sure that our views and clicks columns only contain positive values. This would fail if any cell contained a negative number.

##### Example 2

```
    (A.clicks / (A.views + A.clicks)) * 100.0 <= 5 || A.clicks > 100 where A.date == "20170101"
```

This assert checks to see where the ratio of clicks to clicks and views is less than 5% or the clicks are greater than 100 for the 20170101 date. Only the A dataset
is used and the where clause is used as a way to filter the dataset to only use the rows where A.date is "20170101". For those rows, the assertion is applied.

##### Example 3

```
    approx(A.views, B.expected, B.threshold) where A.country == B.country && B.continent != "as"
```

This assert uses the where clause to perform a cartesian product of A and B and picks all the rows where the country is the same (inner join on country) and the continent is not "as". For these rows, it checks to see the value for A.views is within the corresponding value in B.expected by the corresponding B.threshold percentage. For example, "us" will have approx(10000, 10090, 0.01) performed, which is true.


The Validatar assertion grammar is written in ANTLR and can be found [here](https://github.com/yahoo/validatar/blob/master/src/main/antlr4/com/yahoo/validatar/assertion/Grammar.g4) if you're interested in the exact syntax.

### Parameter Substitution

You may want queries, asserts or query metadata to use a specific date column, or some other changing parameter. For this, we have a parameter substitution feature.

Simply pass `--parameter KEY=VALUE` in the CLI and the `KEY` will be replaced with `VALUE` in all queries, query metadata and test assertions. For example, to query June 23rd 2015, you could use `--parameter DATE=2015-06-23`. If the query uses `${DATE}` in the query it will be replaced before execution with `2015-06-23`.

## Execution Engines

### Hive

The query part of a Hive test is just a HiveSQL statement. We recommend that you push all the heavy lifting to the query - joins, aggregate results etc. We use Hive JDBC underneath to execute against HiveServer2 and fetch the results. We support hive settings at the execution level by passing in --hive-setting arguments to Validatar.

Some mock tests can be found in [src/test/resources/sample-tests/tests.yaml](https://github.com/yahoo/validatar/blob/master/src/test/resources/sample-tests/tests.yaml).

### Pig

The query part of a Pig test is a PigLatin script. You can register your UDFs etc as long as you register them with the full path to them at runtime. We use PigServer underneath to run the query. You can provide the alias in the script to fetch your results from (or leave it to the default). Setting the exec mode and other pig settings are supported.

Validatar is currently compiled against *Pig-0.14*. Running against an older or newer version may result in issues if interfaces have changed. These are relatively minor from experience and can be fixed with relatively minor fixes to engine code if absolutely needed. Feel free to raise issues or you can always tweak the Pig engine and plug it into Validatar.

Some mock tests can be found in [src/test/resources/pig-tests/sample.yaml](https://github.com/yahoo/validatar/blob/master/src/test/resources/pig-tests/sample.yaml).

### REST

The query part of a REST test is a Javascript function that processes the response from your HTTP endpoint into a standard table-like format - a Javascript object (dictionary) where the keys are the column names and the value is an array of the column values.
We execute the native Javascript via Nashorn. The function that takes a single argument - the string response from your endpoint. The name of this function is customizable if desired.

The metadata for the query is used to define the REST call. We currently support setting the method (defaults to GET), the body (if POST), timeout, retry and custom headers.

This execution engine exists essentially a catch-all for any other type of Big Data datasource that has a REST interface but is not natively supported in Validatar. But if you feel like it should be in Validatar, feel free to create an issue and we'll look into supporting it.

Some mock tests and examples can be found in [src/test/resources/rest-tests/sample.yaml](https://github.com/yahoo/validatar/blob/master/src/test/resources/rest-tests/sample.yaml).

### CSV (and other delimited text data)

This execution engine lets you load static data from a file or by defining it in your test YAML file. This is provided to make it easy for to load expected data to run assertions against your actual data. For instance, in the [examples shown above](#examples), Query B with the thresholds for the various countries could be defined as a static dataset and Query A could actually be the result of a query on your Big Data that you are validating. 

Some mock tests and examples can be found in [src/test/resources/csv-tests/sample.yaml](https://github.com/yahoo/validatar/blob/master/src/test/resources/csv-tests/sample.yaml).

## How to install

### Direct Download

Validatar is available on JCenter/Bintray. You can download the artifacts (you will need the jar-with-dependencies artifact to run Validatar) directly from [JCenter](http://jcenter.bintray.com/com/yahoo/validatar/validatar/)

The JARs should be sufficient for usage but if you need to depend on Validatar source directly. You will need to point your Maven or other build tools to JCenter.

### Maven

```
<repositories>
    <repository>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
        <id>central</id>
        <name>bintray</name>
        <url>http://jcenter.bintray.com</url>
    </repository>
</repositories>
```

```
<dependency>
  <groupId>com.yahoo.validatar</groupId>
  <artifactId>validatar</artifactId>
  <version>${validatar.version}</version>
</dependency>
```

### Gradle

```
repositories {
    maven {
        url  "http://jcenter.bintray.com"
    }
}
```

```
compile 'com.yahoo.validatar:validatar:${validatar.version}'
```

## How to Run

For Hadoop based engines like Hive or Pig, it is recommended you run Validatar with ```hadoop jar``` since that sets up most of the classpath for you. Otherwise, you can launch validatar with ```java -cp /PATH/TO/JARS com.yahoo.validatar.App ...```, where ```com.yahoo.validatar.App``` is the main class.

Use ```hadoop jar validatar-jar-with-dependencies.jar com.yahoo.validatar.App --help``` (or -h) for Help

### Running Hive Tests

    export HADOOP_CLASSPATH="$HADOOP_CLASSPATH:/path/to/hive/jdbc/lib/jars/*"
    hadoop jar validatar-jar-with-dependencies.jar com.yahoo.validatar.App -s tests/ --report report.xml --hive-jdbc ...

Hive needs the JDBC uri of HiveServer2. Note that the DB is in the URI.

```
--hive-jdbc "jdbc:hive2://<URI>/<DB>;<Optional params: E.g. sasl.qop=auth;principal=hive/<PRINCIPAL_URL> etc>
```

Do not add it if your queries use the

```
... FROM DB.TABLE WHERE ...
```

format. Instead, you should leave it out and have **ALL** your queries specify the database.

### Running Pig Tests

    export HADOOP_CLASSPATH="$HADOOP_CLASSPATH:/path/to/pig/lib/*" (Add other jars here depending on your pig exec type or if hive/hcat is used in Pig)
    hadoop jar validatar-jar-with-dependencies.jar com.yahoo.validatar.App -s tests/ --report report.xml --pig-exec-type mr --pig-setting 'mapreduce.job.acl-view-job=*' ...

Pig parameters are not supported in the pig query. Instead, use our parameter substitution (see below).

Running REST tests require no other dependencies and can be launched with Java instead of hadoop jar.

## Pluggability

Engines, report generators and test suite parsers are all pluggable. You can implement your own extending the appropriate
interfaces and pass them in to validatar to load at run time by placing it in the classpath. If you wished to have a report generated and posted to a
web service, you could do that! Or vice versa to read test suites off of a webservice or a queue somewhere. Refer to
the options below to see how to pass in the custom implementations.

## Help

Feel free to reach out to us if you run into issues. You are welcome to open any issues. Pull requests welcome!

We list the complete help output from Validatar for reference here:

```
Application options:
Option (* = required)             Description
---------------------             -----------
-h, --help                        Shows help message.
--parameter <Parameter>           Parameter to replace all '${VAR}' in
                                    the query string. Ex: --parameter
                                    DATE=2014-07-24
* --test-suite <File: Test suite  File or folder that contains the test
  file/folder>                      suite file(s).


Advanced Parsing Options:
Option                                 Description
------                                 -----------
--custom-parser <Additional custom     Additional custom parser to load.
  fully qualified classes to plug in>


Hive engine options:
Option (* = required)                   Description
---------------------                   -----------
--hive-driver <Hive driver>             Fully qualified package name to the
                                          hive driver. (default: org.apache.
                                          hive.jdbc.HiveDriver)
* --hive-jdbc <Hive JDBC connector>     JDBC string to the HiveServer2 with an
                                          optional database. If the database
                                          is provided, the queries must NOT
                                          have one. Ex: 'jdbc:hive2:
                                          //HIVE_SERVER:PORT/
                                          [DATABASE_FOR_ALL_QUERIES]'
--hive-password <Hive server password>  Hive server password. (default: anon)
--hive-setting <Hive generic settings   Settings and their values. Ex: 'hive.
  to use.>                                execution.engine=mr'
--hive-username <Hive server username>  Hive server username. (default: anon)


REST Engine options:
Option                               Description
------                               -----------
--rest-function <REST Javascript     The name of the Javascript function
  method name>                         used in all queries (default:
                                       process)
--rest-retry <Integer: REST Query    The default number of times to retry
  retry limit>                         each HTTP request (default: 3)
--rest-timeout <Integer: REST Query  The default time to wait for each HTTP
  timeout>                             request (default: 60000)

This REST Engine works by making a HTTP GET or POST, parsing the response (JSON is best)
using your provided native JavaScript into a common format.
The query part of the engine is a JavaScript function that takes your response from your
request and transforms it to a columnar JSON object with the columns as keys and values
as arrays of values. You may need to iterate over your output and pull out your columns
and return it as a JSON string using JSON stringify. Example: Suppose you extracted
columns called 'a' and 'b', you would create and return the following JSON string :
{"a": [a1, a2, ... an], "b": [b1, b2, ... bn]}
This engine will inspect these elements and convert them to the proper typed objects.
The metadata part of the query contains the required key/value pairs for making the REST
call. The url to make the request to can be set using the url. You can use a
custom timeout in ms for the call using rest-timeout. The HTTP method can be set
using the method - currently support GET and POST
The string body for the POST can be set using the body. The number of
times to retry can be set using rest-retry. If you wish to change the name of the
Javascript function you are using, use the rest-function. Default name is
process. Any other key/value pair is added as headers to the REST call,
with the key being the header name and the value, its value.

CSV Engine options:
Option                                 Description
------                                 -----------
--csv-delimiter <The field delimiter>  The delimiter to use while parsing
                                         fields within a record. Defaults to
                                         ',' or CSV (default: ,)

This Engine lets you load delimited text data from files or to specify it directly as a query.
It follows the RFC 4180 CSV specification: https://tools.ietf.org/html/rfc4180

Your data MUST contain a header row naming your columns.
The types of all fields will be inferred as STRINGS. However, you can provide mappings
for each column name by adding entries to the metadata section of the query, where
the key is the name of your column and the value is the type of the column.
The values can be BOOLEAN, STRING, LONG, DECIMAL, DOUBLE, and TIMESTAMP.
DECIMAL is used for really large numbers that cannot fit inside a long (2^63). TIMESTAMP is
used to interpret a whole number as a timestamp field - millis from epoch. Use to load dates.
This engine primarily exists to let you easily load expected data in as a dataset. You can
then use the data by joining it with some other data and performing asserts on the joined
dataset.

Pig engine options:
Option                                  Description
------                                  -----------
--pig-exec-type <Pig execution type>    The exec-type for Pig to use.  This is
                                          the -x argument used when running
                                          Pig. Ex: local, mr, tez etc.
                                          (default: mr)
--pig-output-alias <Pig default output  The default name of the alias where
  alias>                                  the result is.This should contain
                                          the data that will be collected
                                          (default: validatar_results)
--pig-setting <Pig generic settings to  Settings and their values. The -D
  use.>                                   params that would have been sent to
                                          Pig. Ex: 'mapreduce.job.acl-view-
                                          job=*'


Advanced Engine Options:
Option                                 Description
------                                 -----------
--custom-engine <Additional custom     Additional custom engine to load.
  fully qualified classes to plug in>


Reporting options:
Option                           Description
------                           -----------
--report-format <Report format>  Which report format to use. (default:
                                   junit)


Junit report options:
Option                       Description
------                       -----------
--report-file <Report file>  File to store the test reports.
                               (default: report.xml)


Advanced Reporting Options:
Option                                 Description
------                                 -----------
--custom-formatter <Additional custom  Additional custom formatter to load.
  fully qualified classes to plug in>
```

## Contributing

All contributions, ideas and feedback are welcome! To run and build Validatar, you need Maven 3 and JDK (1.8.60+ for Nashorn). You can
use the make commands in the Makefile to run tests and see coverage (need a clover license) etc.

## Changelog

Version | Notes
------- | -----
0.1.4   | Initial release with Hive
0.1.5   | Typesystem, metadata support
0.1.6   | No feature release. Source and Javadoc bundled in artifact
0.1.7   | Multiple Hive databases across Queries
0.1.8   | Null types in Hive results fix
0.1.9   | Empty results handling bug fix
0.2.0   | Internal switch to Java 8. hive-queue is no longer a setting. Use hive-setting.
0.3.0   | Pig support added.
0.4.0   | Rest API datasource added.
0.4.1   | Classloader and reflections library removal [#19](https://github.com/yahoo/validatar/issues/19)
0.4.2   | Parameter Expansion in metadata [#21](https://github.com/yahoo/validatar/issues/21)
0.4.3   | Parameter Expansion in asserts [#24](https://github.com/yahoo/validatar/issues/24). Hive NULL type bug fix.
0.5.1   | Vector support, join and filter clauses using where [#26](https://github.com/yahoo/validatar/issues/26). CSV static datasource from file or String [#27](https://github.com/yahoo/validatar/issues/27).

## Members

Akshai Sarma, akshaisarma@gmail.com
Josh Walters, josh@joshwalters.com

## Contributors

