# aqrTraverser

Runs this query as an extension:

MATCH (source:LABEL1) <-[r1]-(ins:INS)-[r2]->(destination:DEST) 
WHERE source.value IN [*This is a list with 35,000 elements*] 
AND r1.ValidTo = 2534022320000000 AND r2.ValidTo = 2534022320000000 
RETURN {id:source.value, val: collect({label: labels(destination), prop: destination.value})};


# Instructions

1. Build it:

        mvn clean package

2. Copy target/AQRDemo-1.0.jar to the plugins/ directory of your Neo4j server.

3. Download and copy additional jars to the plugins/ directory of your Neo4j server.
        
        wget http://central.maven.org/maven2/com/google/guava/guava/19.0/guava-19.0.jar

4. Configure Neo4j by adding a line to conf/neo4j.conf:

        dbms.unmanaged_extension_classes=com.aqr=/v1

5. Start Neo4j server.

6. Verify the extension is working.
		:GET /v1/service/helloworld

7. Query the database:

        :POST /v1/service/findNodes {"value" : "US26143X1000,US78468K1060","validTo":"253402232000000"}
        # for example:
        :POST /v1/import/locations {"file":"/Users/maxdemarzi/Projects/import_maxmind/src/main/resources/data/GeoLite2-City-Locations-en.csv"}
        
        
