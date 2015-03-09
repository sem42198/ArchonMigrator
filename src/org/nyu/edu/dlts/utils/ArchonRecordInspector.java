package org.nyu.edu.dlts.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A simple class which allow easy inspecting of archon records for doing analysis
 *
 * Created by nathan on 3/4/2015.
 */
public class ArchonRecordInspector {
    // used to connect to the archon client
    private static ArchonClient archonClient;

    // variables which store a particular archon record
    private static JSONObject contentRecordsJS;
    private static  HashMap<String, String> archonRecordsMap;

    // file used to save the records locally
    private static File jsonFile;
    private static File mapFile;

    /**
     * Method to load the collection content for a particular collection record
     * @param cidKey
     */
    public static void loadCollectionContent(String cidKey) {
        System.out.println("Reading Collection Content for Collection: " + cidKey);

        // get the aggregated json record
        contentRecordsJS = archonClient.getCollectionContentRecords(cidKey);

        // get the raw records for each call in a hasp map
        archonRecordsMap = archonClient.getArchonRecordsMap();

        // now save these records to files
        jsonFile = new File("C:\\Users\\nathan\\temp\\content" + cidKey + ".json");
        mapFile = new File("C:\\Users\\nathan\\temp\\content" + cidKey + ".bin");

        try {
            FileManager.saveTextData(jsonFile, contentRecordsJS.toString(2));
            FileManager.saveObjectToFile(mapFile, archonRecordsMap);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to load collection content record from files instead of the archon backend
     *
     * @param cidKey
     */
    public static void loadCollectionContentFromFile(String cidKey) {
        System.out.println("Loading content record from file ...\n");

        jsonFile = new File("C:\\Users\\nathan\\temp\\content" + cidKey + ".json");
        mapFile = new File("C:\\Users\\nathan\\temp\\content" + cidKey + ".bin");

        try {
            contentRecordsJS = FileManager.getJSONObject(jsonFile);
            archonRecordsMap = (HashMap<String, String>) FileManager.getObjectFromFile(mapFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to run test on the collection content
     */
    private static void processCollectionContent() {
        /*Iterator<String> keys = contentRecordsJS.keys();
        while(keys.hasNext()) {
            String key = keys.next();
        }*/

        // now get the number of records in the hash map
        int count = 0;
        for(String key: archonRecordsMap.keySet()) {
            System.out.println("content endpoint: " + key);

            try {
                JSONObject recordPart = new JSONObject(archonRecordsMap.get(key));
                System.out.println("Record length: " + recordPart.length());
                count += recordPart.length();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        System.out.println("\nNumber of Content Records From Merge JSON: " + contentRecordsJS.length());
        System.out.println("Number of Content Records From Raw JSON: " + count);
    }

    /**
     * Main method
     *
     * @param args
     */
    public static void main(String[] args) throws JSONException {
        //String host = "http://archives-dev.library.illinois.edu/archondev/tracer";
        String host = "http://quanta2.bobst.nyu.edu/~nathan/archon";
        archonClient = new ArchonClient(host, "admin", "admin");
        archonClient.getSession();

        System.out.println("Connected to " + host + "\n\n");

        // load the content record
        loadCollectionContentFromFile("811");

        // process the content records
        processCollectionContent();
    }
}
