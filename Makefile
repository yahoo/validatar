clean:
	mvn clean

test:
	mvn clean cobertura:cobertura checkstyle:check

jar:
	mvn package
