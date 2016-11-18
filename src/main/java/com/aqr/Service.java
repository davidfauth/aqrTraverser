package com.aqr;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.cypher.internal.frontend.v3_0.ast.functions.Rels;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;










import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractScheduledService;




@Path("/service")
public class Service {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-M-d-H-m");

    @Context
    private final GraphDatabaseService database;

    public Service( @Context GraphDatabaseService database )
    {
        this.database = database;
    }

    public enum RelTypes implements RelationshipType
    {
    	ASSERTS_ID_MAP
}

    public enum Labels implements Label {
        Issue,
        IDValue,
		PerspectiveEntity, ID_ISIN

    }

    @GET
    @Path("/helloworld")
    public String helloWorld() {
        return "Hello World!";
    }


    @GET
    @Path("/warmup")
    public String warmUp(@Context GraphDatabaseService db) {
        try ( Transaction tx = db.beginTx()) {
            for ( Node n : db.getAllNodes()) {
                n.getPropertyKeys();
                for ( Relationship relationship : n.getRelationships()) {
                    relationship.getPropertyKeys();
                    relationship.getStartNode();
                }
            }
        }
        return "Warmed up and ready to go!";
    }

    //MATCH (source:LABEL1) <-[r1]-(ins:INS)-[r2]->(destination:DEST) 
    //WHERE source.value IN [*This is a list with 35,000 elements*] 
    //AND r1.ValidTo = 2534022320000000 AND r2.ValidTo = 2534022320000000 
    //RETURN {id:source.value, val: collect({label: labels(destination), prop: destination.value})};
    
    @POST
    @Path("/findNodes")
    public Response findNodes(String body, @Context final GraphDatabaseService db) throws IOException {
      
    	final HashMap input = Validators.getValidQueryInput(body);
	  final HashMap<Object, Object> targetNodes = new HashMap<>();
	  final HashMap<Object, Object> fofNodes = new HashMap<>();
  	  final HashMap<Object, Object> fofRels = new HashMap<>();
  	  final HashMap<Object, String> testMap = new HashMap<>();
	  final HashMap<Integer, Object> removedNodes = new HashMap<>();
	  final HashMap<Integer, Integer> countOfCliques = new HashMap<>();
	  final Multimap<Long, Long> listOfRelationships = ArrayListMultimap.create();
	  
	  final ArrayList<Long> intermediateNodes = new ArrayList<Long>();
	  final Cache<Object, String> testCache = CacheBuilder.newBuilder().maximumSize(10_000_000).build();
	  final Cache<Object, String> labelCache = CacheBuilder.newBuilder().maximumSize(10_000_000).build();

      StreamingOutput stream = new StreamingOutput() {
          @Override
          public void write(OutputStream os) throws IOException, WebApplicationException
          {
        	  int uniqueNodes = 0;
        	  int clique_id = 0;
        	  int counter = 0;
        	  Node startFeed = null;
        	  Node startNode = null;
        	  Node endNode = null;
        	  Relationship startRel = null;
        	  
        	  try (Transaction tx = db.beginTx()) {
        		  List<String> myList = new ArrayList<String>(Arrays.asList(input.get("value").toString().split(",")));
         		  String validTo = input.get("validTo").toString();
         		  Node targetNode = null;
        		  for (String nodeList : myList) {
        			  targetNode = findNodeByKey(nodeList);
        		  
        			  for (Relationship r : targetNode.getRelationships(Direction.INCOMING)){
        				  		if (!intermediateNodes.contains(r.getStartNode().getId())){   
        				  			Long lValidTo = Double.valueOf(r.getProperty("ValidTo").toString()).longValue();
									if(lValidTo == Long.parseLong(validTo)){
										intermediateNodes.add(r.getStartNode().getId());
										targetNodes.put(r.getEndNode().getId(), r.getStartNode().getId());
									}
									
								}
							}
						
        		  }
        		  
        		  // Get Intermediate Nodes
        		  String nodeList = "";
        		  String labelList = "";
        		  for (Long lNodeId : intermediateNodes) {
        			  startNode = findNodeById(lNodeId);
        			  for (Relationship r : startNode.getRelationships(Direction.OUTGOING)){
  				  			Long lValidTo = Double.valueOf(r.getProperty("ValidTo").toString()).longValue();
								if(lValidTo == Long.parseLong(validTo)){
									nodeList=nodeList + r.getEndNode().getProperty("value").toString() + ",";
									labelList=labelList + r.getEndNode().getLabels().toString() + ",";
									testCache.put(lNodeId, nodeList);
									labelCache.put(lNodeId,labelList);
									counter++;
								}
								
						}
        			  testMap.put(lNodeId, nodeList);
        			  nodeList = "";
        			  labelList = "";
					

        		 }
                tx.success();
              }
        	  System.out.println("intermediate node count is:" + intermediateNodes.size());
        	  System.out.println("testCache count is:" + testCache.size());
        	  // Write out JSON
              JsonGenerator jg = objectMapper.getJsonFactory().createJsonGenerator( os, JsonEncoding.UTF8 );
              jg.writeStartObject();
              jg.writeArrayFieldStart("nodes");
              Iterator it = targetNodes.entrySet().iterator();
              while (it.hasNext()) {
                  Map.Entry pair = (Map.Entry)it.next();

                    	jg.writeStartObject();
                    	jg.writeObjectField("id", pair.getKey());
                       	jg.writeObjectField("val", "prop:{" + testCache.getIfPresent(pair.getValue())+ "}");
                       	jg.writeObjectField("labels", "labels:{" + labelCache.getIfPresent(pair.getValue())+ "}");
                    	jg.writeEndObject();
              }
        	  jg.writeEndArray();

              jg.writeArrayFieldStart("links");

               jg.writeEndArray();


              jg.writeEndObject();
              jg.flush();
              jg.close();
          
          }
          
          
      
      };
        
    return Response.ok().entity(stream).type(MediaType.APPLICATION_JSON).build();
 }


    
    private Node findNodeByKey(String id) {
        final Node node = database.findNode(Labels.ID_ISIN, "value", id);
        if (node == null) {
            return null;
        } else {
            return node;
        }
    }
 
        private Node findServiceNode(int id) {
            final Node node = database.findNode(Labels.Issue, "key", id);
            if (node == null) {
                return null;
            } else {
                return node;
            }
        }

        private Node findNodeById(Long lNodeId) {
//          final Node node = database.findNode(Labels.PDE, "id", id);
          final Node node = database.getNodeById(lNodeId);

          if (node == null) {
              return null;
          } else {
              return node;
          }
      }

    @XmlRootElement
    public class MyJaxBean {
        @XmlElement public String param1;
    }

    class ValueComparator implements Comparator<String> {

        Map<String, Double> base;
        public ValueComparator(Map<String, Double> base) {
            this.base = base;
        }

        // Note: this comparator imposes orderings that are inconsistent with equals.
        public int compare(String a, String b) {
            if (base.get(a) >= base.get(b)) {
                return -1;
            } else {
                return 1;
            } // returning 0 would merge keys
        }
    }



}