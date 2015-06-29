all: full

clean:
	mvn clean

test:
	mvn clean checkstyle:check test

cc:
	mvn clean cobertura:cobertura

see-cc: cc
	cd target/site/cobertura; python -m SimpleHTTPServer

jar:
	mvn clean package

release:
	mvn -B release:prepare release:clean

full:
	mvn clean checkstyle:check cobertura:cobertura javadoc:jar package
