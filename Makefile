all: full

full:
	mvn clean javadoc:jar package

clean:
	mvn clean

test:
	mvn clean verify javadoc:javadoc

jar:
	mvn clean package

release:
	mvn -B release:prepare release:clean

coverage:
	mvn clean clover2:setup test clover2:aggregate clover2:clover

doc:
	mvn clean compile javadoc:javadoc

see-coverage: coverage
	cd target/site/clover; python -m SimpleHTTPServer

see-doc: doc
	cd target/site/apidocs; python -m SimpleHTTPServer

fix-javadocs:
	mvn javadoc:fix -DfixClassComment=false -DfixFieldComment=false

