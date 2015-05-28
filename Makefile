clean:
	mvn clean

test:
	mvn clean cobertura:cobertura checkstyle:check

jar:
	mvn clean package

full:
	mvn clean checkstyle:check cobertura:cobertura javadoc:javadoc package
