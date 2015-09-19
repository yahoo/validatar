# Validatar

[![Build Status](https://travis-ci.org/yahoo/validatar.svg?branch=master)](https://travis-ci.org/yahoo/validatar) [![Coverage Status](https://coveralls.io/repos/yahoo/validatar/badge.svg?branch=master)](https://coveralls.io/r/yahoo/validatar?branch=master) [![Download](https://api.bintray.com/packages/yahoo/maven/validatar/images/download.svg)](https://bintray.com/yahoo/maven/validatar/_latestVersion)

Functional testing framework for Big Data pipelines. Currently support querying pipeline results through Hive (HiveServer2) and Pig (PigServer).

## How to build Validatar

You need maven/JDK to build Validatar.

Run:

    make jar

## How to run

To run Hive tests in Validatar:

    export HADOOP_CLASSPATH="$HADOOP_CLASSPATH:/path/to/hive/jdbc/lib/jars/*"
    hadoop jar validatar-jar-with-dependencies.jar com.yahoo.validatar.App -s tests/ --report report.xml

To run Pig tests in Validatar:

    Coming soon!

You will also need the settings specified for the engine you are planning to run.

Hive needs the JDBC uri of HiveServer2. Note that the DB is in the URI. Do not add it if your queries use ... FROM DB.TABLE WHERE ...
   ```
   --hive-jdbc "jdbc:hive2://<URI>/<DB>;<Optional params: E.g. sasl.qop=auth;principal=hive/<PRINCIPAL_URL> etc>
   ```


## Writing Tests

### Test file format

Test files are written in the YAML format. See example in src/test/resources/. The schema is as follows:

```
name: Test family name : String
description: Test family description : String
queries:
   - name: Query name : String : Ex "Analytics"
     engine: Execution engine : String Ex "hive" or "pig"
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

### Parameter Substitution

You may want queries that use a specific date column, or similar changing parameter. For this, we have a parameter substation feature.

Simply pass `--parameter KEY=VALUE` in the CLI and the `KEY` will be replaced with `VALUE` in all queries. For example, to query June 23rd 2015, you could use `--parameter DATE=2015-06-23`. If the query uses `${DATE}` in the query it will be replaced before execution with `2015-06-23`.

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

## Members

Akshai Sarma, akshaisarma@gmail.com
Josh Walters, josh@joshwalters.com

## Contributors


