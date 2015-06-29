all: full

full:
	mvn clean checkstyle:check cobertura:cobertura javadoc:jar package

clean:
	mvn clean

test:
	mvn clean checkstyle:check test

jar:
	mvn clean package

release:
	mvn -B release:prepare release:clean

coverage:
	mvn clean cobertura:cobertura

doc:
	mvn clean javadoc:javadoc

see-coverage: coverage
	cd target/site/cobertura; python -m SimpleHTTPServer

see-doc: doc
	cd target/site/apidocs; python -m SimpleHTTPServer

fix-javadocs:
	mvn javadoc:fix -DfixClassComment=false -DfixFieldComment=false

