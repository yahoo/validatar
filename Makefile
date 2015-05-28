clean:
	mvn clean

test:
	mvn clean cobertura:cobertura checkstyle:check

jar:
	mvn clean package

release:
	mvn -B release:prepare release:clean

full:
	mvn clean checkstyle:check cobertura:cobertura javadoc:jar package
