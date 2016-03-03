# Validatar

[![Build Status](https://travis-ci.org/yahoo/validatar.svg?branch=master)](https://travis-ci.org/yahoo/validatar) [![Coverage Status](https://coveralls.io/repos/yahoo/validatar/badge.svg?branch=master)](https://coveralls.io/r/yahoo/validatar?branch=master) [![Download](https://api.bintray.com/packages/yahoo/maven/validatar/images/download.svg)](https://bintray.com/yahoo/maven/validatar/_latestVersion)

... is a Functional Testing Framework for Big Data pipelines. We currently support querying data through Hive (HiveServer2) and Pig (PigServer). Since a lot of other datasources (e.g. Storm DRPC) expose a REST interface, a REST datasource is also supported. You can GET or POST to your endpoint and parse the result into a standard format using some custom Javascript. Validatar is currently compiled against *Pig-0.14*. Running against an older or newer version may result in issues if interfaces have changed. These are relatively minor from experience and can be fixed with relatively minor fixes to engine code if absolutely needed.

## How to build Validatar

You need maven/JDK (1.8.60+ for Nashorn) to build Validatar.

Run:

    make jar

## Writing Tests

### Test file format

Test files are written in the YAML format. See examples of all different datasources in src/test/resources/. The schema is as follows:

```
name: Test family name : String
description: Test family description : String
queries:
   - name: Query name : String : Ex "Analytics"
     engine: Execution engine : String Ex "hive" or "pig" or "rest"
     value: Query : String : Ex "SELECT COUNT(*) AS pv_count FROM page_data"
   ...
tests:
   - name: Test name : String
     description: Test description : String
     asserts:
        - Assertion on some query. Query name is prefixed to the value. : Ex Analytics.pv_count > 10000
   ...
```

Queries are named, this name is used as a namespace for all the values returned from the query. In the above example, we created a query named "Analytics". It stores the return value "pv_count". We are then able to use this in our later asserts.

Validatar can run a single test file or a folder of test files. Use the --help option to see more details or refer to the Help section below.

### Assertions

Assertions are quite flexible, allowing for the following operations:

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

Since these are the only operations we currently support, assertions are scalar, i.e. they operate on a single row. As a result, *Validatar currently limits all results from running queries to a single row.* Any need for aggregate or multi-row consideration can be pushed into the query itself. Please do open issues if you find that you are unable to do so and we will look into relaxing this requirement.

### Parameter Substitution

You may want queries that use a specific date column, or similar changing parameter. For this, we have a parameter substation feature.

Simply pass `--parameter KEY=VALUE` in the CLI and the `KEY` will be replaced with `VALUE` in all queries. For example, to query June 23rd 2015, you could use `--parameter DATE=2015-06-23`. If the query uses `${DATE}` in the query it will be replaced before execution with `2015-06-23`.

### Execution Engines

#### Hive

The query part of a Hive test is just a HiveSQL statement. We recommend that you push all the heavy lifting to the query - joins, aggregate results etc. We use Hive JDBC underneath to execute against HiveServer2 and fetch the results. We support hive settings at the execution level by passing in --hive-setting arguments to validatar.

Some mock tests can be found in src/test/resources/sample-tests/tests.yaml

#### Pig

The query part of a Pig test is a PigLatin script. You can register your UDFs etc as long as you register them with the full path to them at runtime. We use PigServer underneath to run the query. You can provide the alias in the script to fetch your results from (or leave it to the default). Setting the exec mode and other pig settings are supported.

Some mock tests can be found in src/test/resources/pig-tests/sample.yaml

#### REST

The query part of a REST test is a Javascript function that processes the response from your HTTP endpoint into a standard table-like format - a Javascript object (dictionary) where the keys are the column names and the value is an array of the column values.
We execute the native Javascript via Nashorn. The function that takes a single argument - the string response from your endpoint. The name of this function is customizable if desired.

The metadata for the query is used to define the REST call. We currently support setting the method (defaults to GET), the body (if POST), timeout, retry and custom headers.

This execution engine exists essentially a catch-all for any other type of Big-Data datasource that has a REST interface but is not natively in Validatar.

Some mock tests and examples can be found in src/test/resources/rest-tests/sample.yaml

## How to run

Use hadoop jar validatar-jar-with-dependencies.jar com.yahoo.validatar.App --help (or -h) for Help

### To run Hive tests in Validatar:

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

format. Instead, you should leave it out and have ALL your queries specify the database.

### To run Pig tests in Validatar:

    export HADOOP_CLASSPATH="$HADOOP_CLASSPATH:/path/to/pig/lib/*" (Add other jars here depending on your pig exec type or if hive/hcat is used in Pig)
    hadoop jar validatar-jar-with-dependencies.jar com.yahoo.validatar.App -s tests/ --report report.xml --pig-exec-type mr --pig-setting 'mapreduce.job.acl-view-job=*' ...

Pig parameters are not supported in the pig query. Instead, use our parameter substitution (see below).

Running REST tests require no other dependencies and can be launched with Java instead of hadoop jar.

## Pluggability
Engines, report generators and test suite parsers are all pluggable. You can implement your own extending the appropriate
interfaces and pass them in to validatar to load at run time. If you wished to have a report generated and posted to a
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

## Changelog

Version | Notes
------- | -----
0.1.4 | Initial release with Hive
0.1.5 | Typesystem, metadata support
0.1.6 | No feature release. Source and Javadoc bundled in artifact
0.1.7 | Multiple Hive databases across Queries
0.1.8 | Null types in Hive results fix
0.1.9 | Empty results handling bug fix
0.2.0 | Internal switch to Java 8. hive-queue is no longer a setting. Use hive-setting.
0.3.0 | Pig support added.
0.4.0 | Rest API datasource added.
0.4.1 | Classloader and reflections library removal [#19](issues/19)

## Members

Akshai Sarma, akshaisarma@gmail.com
Josh Walters, josh@joshwalters.com

## Contributors

