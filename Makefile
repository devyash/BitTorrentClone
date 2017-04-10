all: build-rel

JAR=target/BitTorrentClone-1.0.jar

build:
	mvn package

build-rel:
	ant -f build/build.xml

tar-rel: clean
	ant -f build/build.xml tar

clean:
	rm -rf *.log *.log* *.zip
	# mvn clean dependency:copy-dependencies
