package org.nyu.edu.dlts.utils;

import org.apache.commons.httpclient.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import java.io.File;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Nathan Stevens
 *
 * Date: 9/5/12
 * Time: 1:48 PM
 * Utility class for copying data from the Archon to Archivesspace
 */
public class ASpaceCopyUtil implements  PrintConsole {
    public static final String SUPPORTED_ASPACE_VERSION = "v1.3";

    // String to indicate when no ids where return from aspace backend
    private final String NO_ID = "no id assigned";

    // used to create the Archive Space JSON data
    private ASpaceMapper mapper;

    // The utility class used to map to ASpace Enums
    private ASpaceEnumUtil enumUtil;

    // used to make REST calls to archive space backend service
    private ASpaceClient aspaceClient = null;

    // used make http connection to archon
    private ArchonClient archonClient = null;

    // used to store information about the archives space backend
    private String aspaceInformation = "Simulator";

    private String aspaceVersion = "";

    // hashmap that maps repository from old database with copy in new database
    private HashMap<String, String> repositoryURIMap = new HashMap<String, String>();

    // hasmap to store the repo agents for use in creating event objects
    private HashMap<String, String> repositoryAgentURIMap = new HashMap<String, String>();

    // hashmap that stores the repository groups from the archive space database
    private HashMap<String, JSONObject> repositoryGroupURIMap = new HashMap<String, JSONObject>();

    // store the repository names and Archon ID for debugging purposes
    private ArrayList<String> repositoryNamesList = new ArrayList<String>();

    // hashmap that maps location from the old database with copy in new database
    private HashMap<String, String> locationURIMap = new HashMap<String, String>();

    // hashmap that maps subjects from old database with copy in new database
    private HashMap<String, String> subjectURIMap = new HashMap<String, String>();

    // hashmap that maps names from old database with copy in new database
    private HashMap<String, String> nameURIMap = new HashMap<String, String>();

    // hashmap that maps classification from old database with copy in new database
    private HashMap<String, String> classificationURIMap = new HashMap<String, String>();

    // hashmap that maps classification term from old database with copy in new database
    private HashMap<String, String> classificationTermURIMap = new HashMap<String, String>();

    // hashset to keep track of classifications ids which have been linked to a record
    // so un-liked classifications can be clean deleted
    private HashSet<String> linkedClassificationSet = new HashSet<String>();

    // hashmap that maps accessions from old database with copy in new database
    private HashMap<String, String> accessionURIMap = new HashMap<String, String>();

    // hashmap that maps digital objects from old database with copy in new database
    private HashMap<String, String> digitalObjectURIMap = new HashMap<String, String>();

    // hashmap that stores the converted digital objects so that they can be save to the correct repo
    // when saving the collection content
    private HashMap<String, ArrayList<JSONArray>> digitalObjectMap = new HashMap<String, ArrayList<JSONArray>>();

    // hashmap that maps resource from old database with copy in new database
    private HashMap<String, String> resourceURIMap = new HashMap<String, String>();

    // stores subject that need to be converted to agent records
    private ArrayList<JSONObject> subjectToAgentList = new ArrayList<JSONObject>();

    // stop watch object for keeping tract of time
    private StopWatch stopWatch = null;

    // specify debug the boolean
    private boolean debug = true;

    // random string generator to use when simulating rest calls
    private RandomString randomString = new RandomString(3);

    // specify the current record type and ID in case we have fetal error during migration
    private String currentRecordType = "";
    private String currentRecordIdentifier = "";
    private String currentRecordDBID = "";

    // integer to keep track of # of resource records copied
    private int copyCount = 1;

    // integer to keep track of total number of aspace client threads
    private int totalASpaceClients = 0;

    // integers to keeps track of the number of digital objects copied
    private int digitalObjectTotal = 0;
    private int digitalObjectSuccess = 0;

    // boolean to specify weather to save digital objects by themselves or with the resource records
    boolean saveDigitalObjectsWithResources = true;

    // These fields are used to track of the number of messages posted to the output console
    // in order to prevent memory usage errors
    private int messageCount = 0;
    private final int MAX_MESSAGES = 100;

    // keep tract of the number of errors when converting and saving records
    private int saveErrorCount = 0;
    private int aspaceErrorCount = 0;

    private String resetPassword = "password";

    // this is used to output text to user when doing the data transfer
    private JTextArea outputConsole;

    // this are used to give user better feedback on progress
    private JProgressBar progressBar;
    private JLabel errorCountLabel;

    // used to specify the stop the copying process. Only get checked when copying resources
    private boolean stopCopy = false;

    // used to specified the the copying process is running
    private boolean copying = false;

    /* Variables used to save URI mapping to file to make data transfer more efficient */

    // file where the uri maps is saved
    private static File uriMapFile = null;

    // keys use to store objects in hash map
    private final String REPOSITORY_KEY = "repositoryURIMap";
    private final String LOCATION_KEY = "locationURIMap";
    private final String USER_KEY = "userURIMap";
    private final String SUBJECT_KEY = "subjectURIMap";
    private final String NAME_KEY = "nameURIMap";
    private final String ACCESSION_KEY = "accessionURIMap";
    private final String DIGITAL_OBJECT_KEY = "digitalObjectURIMap";
    private final String RESOURCE_KEY = "resourceURIMap";
    private final String REPOSITORY_MISMATCH_KEY = "repositoryMismatchMap";
    private final String RECORD_TOTAL_KEY = "copyProgress";

    // An Array List for storing the total number of main records transferred
    ArrayList<String> recordTotals = new ArrayList<String>();

    // Specifies whether or not to simulate the REST calls
    private boolean simulateRESTCalls = false;

    // Specifies whether to delete the previously saved resource records. Useful for testing purposes
    private boolean deleteSavedResources = false;

    // this list is used to copy a specific resource
    private ArrayList<String> resourcesIDsList;

    // A string builder object to track errors
    private StringBuilder errorBuffer = new StringBuilder();

    /* variables used for debugging operations */
    private int maxDepth = 0;

    private int batchLengthMin = 0;
    private int batchLengthMax = 100000;

    private File recordDumpDirectory = null;

    private JSONObject cachedResourcesJS;

    // booleans used to convert some bbcode to html or blanks
    private boolean bbcodeToHTML = false;
    private boolean bbcodeToBlank = true;

    // the default repository id
    private String defaultRepositoryId;

    /**
     * The main constructor, used when running as a stand alone application
     *
     * @param archonClient
     */
    public ASpaceCopyUtil(ArchonClient archonClient, String host, String admin, String adminPassword) {
        this.archonClient = archonClient;
        this.aspaceClient = new ASpaceClient(host, admin, adminPassword);
        init();
    }

    /**
     * Method to initiate certain variables that are needed to work
     */
    private void init() {
        print("Starting database copy ... ");

        // set the error buffer for the mapper
        mapper = new ASpaceMapper(this);

        // set the enum util to that of the mapper
        enumUtil = mapper.getEnumUtil();

        // set the file that contains the record map
        uriMapFile = new File(System.getProperty("user.home") + File.separator + "uriMaps.bin");

        // first add the admin repo to the repository URI map
        repositoryURIMap.put("adminRepo", ASpaceClient.ADMIN_REPOSITORY_ENDPOINT);

        // set the print console object so we have something to print out while the
        // records are being read in batches of 100
        archonClient.setPrintConsole(this);

        /*
        // get the language codes and set it for the enum and mapper utils
        languageCodes = sourceRCD.getLanguageCodes();
        nameLinkCreatorCodes = sourceRCD.getNameLinkCreatorCodes();

        mapper.setLanguageCodes(languageCodes);
        mapper.setNameLinkCreatorCodes(nameLinkCreatorCodes);
        mapper.setConnectionUrl(sourceRCD.getConnectionUrl());

        enumUtil.setLanguageCodes(languageCodes);
        enumUtil.setNameLinkCreatorCodes(nameLinkCreatorCodes);

        // map used when copying lookup list items to the archive space backend
        lookupListMap.put("subject term source", ASpaceClient.VOCABULARY_ENDPOINT);
        */

        // start the stop watch object so we can see how long this data transfer takes
        startWatch();
    }

    /**
     * Method to set the reset password when copying user records
     *
     * @param resetPassword
     */
    public void setResetPassword(String resetPassword) {
        this.resetPassword = resetPassword;
    }

    /**
     * Method to set the output console
     *
     * @param outputConsole
     */
    public void setOutputConsole(JTextArea outputConsole) {
        this.outputConsole = outputConsole;
    }

    /**
     * Method to set the progress bar
     *
     * @param progressBar
     * @param errorCountLabel
     */
    public void setProgressIndicators(JProgressBar progressBar, JLabel errorCountLabel) {
        this.progressBar = progressBar;
        this.errorCountLabel = errorCountLabel;
    }

    /**
     * Set the option for either converting some bbcode to html
     *
     * @param option
     */
    public void setBBCodeOption(String option) {
        if(option.equals("-bbcode_html")) {
            bbcodeToHTML = true;
            bbcodeToBlank = false;
        } else {
            bbcodeToHTML = false;
            bbcodeToBlank = true;
        }
    }

    /**
     * Method to set the default repository id
     * @param id
     */
    public void setDefaultRepositoryId(String id) {
        defaultRepositoryId = id;
    }

    /**
     * Method to copy the archon enum records
     */
    public void copyEnumRecords() throws Exception {
        HashMap<String, JSONObject> dynamicEnums = aspaceClient.loadDynamicEnums();

        if(dynamicEnums == null) {
            print("Can't load ASpace dynamic enums ...");
            return;
        }

        updateProgress("Enum List", 0, 0);

        mapper.setASpaceDynamicEnums(dynamicEnums);

        JSONObject enumsJS = archonClient.getAllEnums();
        Iterator<String> keys = enumsJS.keys();

        int total = enumsJS.length();
        int count = 0;

        while(keys.hasNext()) {
            String enumEndpoint = keys.next();
            JSONObject enumList = enumsJS.getJSONObject(enumEndpoint);

            JSONObject updatedEnumJS = mapper.mapEnumList(enumList, enumEndpoint);

            if(updatedEnumJS != null && !enumEndpoint.contains("enum_type=countries")) {
                String endpoint = updatedEnumJS.getString("uri");
                String jsonText = updatedEnumJS.toString();
                String id = saveRecord(endpoint, jsonText, "EnumList->" + enumEndpoint);

                if (!id.equalsIgnoreCase(NO_ID)) {
                    print("Copied Enum List Values: " + enumEndpoint + " :: " + id);
                }
            }

            count++;
            updateProgress("Enum List", total, count);
        }

        // set this to be 100 percent since not all lookup will need to be migrated
        updateRecordTotals("Enum List", total, total);
    }

    /**
     * Method to copy the repository records
     *
     * @throws Exception
     */
    public void copyRepositoryRecords() throws Exception {
        print("Copying repository records ...");

        // update the progress bar to indicate loading of records
        updateProgress("Repositories", 0, 0);

        JSONObject records = archonClient.getRepositoryRecords();

        // these are used to update the progress bar
        int total = records.length();
        int count = 0;
        int success = 0;

        Iterator<String> keys = records.keys();
        while(keys.hasNext()) {
            if(stopCopy) return;

            JSONObject repository = records.getJSONObject(keys.next());

            String shortName = repository.getString("Name");
            String repoID = repository.getString("ID");

            repositoryNamesList.add(shortName + ", ID: " + repoID);

            if (!repositoryURIMap.containsKey(repoID)) {
                String jsonText;
                String id;

                // get and save the agent object for the repository
                String agentURI = null;
                jsonText = mapper.getCorporateAgent(repository);
                id = saveRecord(ASpaceClient.AGENT_CORPORATE_ENTITY_ENDPOINT, jsonText, "Repository_Name_Corporate->" + shortName);

                if (!id.equalsIgnoreCase(NO_ID)) {
                    agentURI = ASpaceClient.AGENT_CORPORATE_ENTITY_ENDPOINT + "/" + id;
                }

                jsonText = mapper.convertRepository(repository, agentURI);
                id = saveRecord(ASpaceClient.REPOSITORY_ENDPOINT, jsonText, "Repository->" + shortName);

                if (!id.equalsIgnoreCase(NO_ID)) {
                    String uri = ASpaceClient.REPOSITORY_ENDPOINT + "/" + id;
                    repositoryURIMap.put(repoID, uri);
                    repositoryAgentURIMap.put(uri, agentURI);

                    print("Copied Repository: " + shortName + " :: " + id);
                    success++;
                } else {
                    print("Fail -- Repository: " + shortName);
                }
            } else {
                print("Repository already in database " + shortName);
                success++;
            }

            count++;
            updateProgress("Repositories", total, count);
        }

        updateRecordTotals("Repositories", total, success);
    }

    /**
     * Method to load the repository groups, map them to access classes
     *
     * then place them in a hashmap
     */
    public void mapRepositoryGroups() {
        print("Mapping repository user group records ...");

        // update the progress bar to indicate loading of records
        updateProgress("Repository Groups", 0, 0);

        // these are used to update the progress bar
        int total = repositoryURIMap.size();
        int count = 0;

        // now load the groups for all repositories
        for(String repoURI: repositoryURIMap.values()) {
            JSONArray groupsJA = aspaceClient.loadRepositoryGroups(repoURI);

            // process each group and map it to an AT user class
            for(int i = 0; i < groupsJA.length(); i++) {
                try {
                    JSONObject groupJS = groupsJA.getJSONObject(i);

                    // if no members array exist add one
                    if(groupJS.isNull("member_usernames")) {
                        JSONArray membersJA = new JSONArray();
                        groupJS.put("member_usernames", membersJA);
                    }

                    // map this group to an AR access class
                    mapper.mapAccessClass(repositoryGroupURIMap, groupJS, repoURI);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                count++;
                updateProgress("Repository Groups", total, count);
            }
        }

        if(debug) {
            print("Number of groups mapped: " + repositoryGroupURIMap.size());
        }
    }

    /**
     * Copy the user records, remapping the repository
     *
     * @throws Exception
     */
    public void copyUserRecords() throws Exception {
        print("Copying User records ...");

        // update the progress bar here to indicate loading of records
        updateProgress("Users", 0, 0);

        JSONObject records = archonClient.getUserRecords();

        // these are used to update the progress bar
        int total = records.length();
        int count = 0;
        int success = 0;

        Iterator<String> keys = records.keys();
        while(keys.hasNext()) {
            if(stopCopy) return;

            JSONObject user = records.getJSONObject(keys.next());
            String userName = user.getString("DisplayName");

            // check to see if the is admin is set to true, otherwise continue
            if(user.getInt("IsAdminUser") == 0 || user.getString("Login").equals("admin")) {
                print("None admin or duplicate user: " + userName + ", not migrating ...");
                continue;
            }

            // first get the group the user belongs too
            ArrayList<String> groupURIs = getUserGroupURIs(user);

            if(groupURIs == null) {
                print("Denied user: " + userName + ", not migrating ...");
                continue;
            }

            NameValuePair[] params = new NameValuePair[1 + groupURIs.size()];
            params[0] = new NameValuePair("password", resetPassword);

            for (int i = 0; i < groupURIs.size(); i++) {
                params[i + 1] = new NameValuePair("groups[]", groupURIs.get(i));
            }

            String jsonText = mapper.convertUser(user);
            String id = saveRecord(ASpaceClient.USER_ENDPOINT, jsonText, params, "User->" + userName);

            if (!id.equalsIgnoreCase(NO_ID)) {
                print("Copied User: " + userName + " :: " + id);
                success++;
            } else {
                print("Fail -- User: " + userName);
            }

            count++;
            updateProgress("Users", total, count);
        }

        updateRecordTotals("Users", total, success);

        // refresh the database connection to prevent heap space error
        freeMemory();
    }

    /**
     * Method to return the URI's of ASpace group(s) a user belongs to based on repository and access class
     *
     * It currently assigns users to a single group, but is designed to multiple groups if needed
     */
    public ArrayList<String> getUserGroupURIs(JSONObject user) throws Exception {
        ArrayList<String> groupURIs = new ArrayList<String>();

        // get the repository URIs the user belong to
        int repositoryLimit = user.getInt("RepositoryLimit");
        ArrayList<String> repoURIs = new ArrayList<String>();

        if(repositoryLimit == 1) {
            JSONArray repoIDs = user.getJSONArray("Repositories");
            for (int i = 0; i < repoIDs.length(); i++) {
                String repoID = repoIDs.getString(i);
                repoURIs.add(getRepositoryURI(repoID));
            }
        } else {
            for(String repoURI: repositoryURIMap.values()) {
                if (!repoURI.endsWith("/1")) {
                    repoURIs.add(repoURI);
                }
            }
        }

        // construct the key base on access class
        String key = "";
        JSONObject groupJS;

        // Get the correct access class by getting the lowest value user group number
        int accessClass = 10;

        JSONArray userGroupsJA = user.getJSONArray("Usergroups");
        for(int i = 0; i < userGroupsJA.length(); i++) {
            int userGroup = userGroupsJA.getInt(i);
            if(userGroup < accessClass) {
                accessClass = userGroup;
            }
        }

        // first check that this user access class prevents them from being
        if(accessClass == 5) {
            return null;
        } else if (accessClass == 1) {
            key = ASpaceClient.ADMIN_REPOSITORY_ENDPOINT + ASpaceMapper.ACCESS_CLASS_PREFIX + 1;
            groupJS = repositoryGroupURIMap.get(key);
            if (groupJS != null) {
                groupURIs.add(groupJS.getString("uri"));
            }
        } else {
            for (String repoURI : repoURIs) {
                key = repoURI + ASpaceMapper.ACCESS_CLASS_PREFIX + accessClass;
                groupJS = repositoryGroupURIMap.get(key);

                if (groupJS != null) {
                    groupURIs.add(groupJS.getString("uri"));
                }
            }
        }

        return groupURIs;
    }

    /**
     * Method to copy subject records
     * @throws Exception
     */
    public void copySubjectRecords() throws Exception {
        print("Copying Subject records ...");

        // update the progress so that the title changes
        updateProgress("Subjects", 0, 0);

        JSONObject records = archonClient.getSubjectRecords();

        int total = records.length();
        int success = 0;
        int count = 0;

        Iterator<String> keys = records.keys();
        while (keys.hasNext()) {
            if (stopCopy) return;

            String key = keys.next();

            JSONObject subject = records.getJSONObject(key);

            String arId = subject.getString("ID");
            int subjectTypeID = subject.getInt("SubjectTypeID");

            // check the subject type id since some of these subject need to be converted
            // to an agent record instead
            if(subjectTypeID == 3 || subjectTypeID == 8 || subjectTypeID == 10 && subject.get("Parent") == null) {
                subjectToAgentList.add(subject);
                success++;
            } else {
                JSONObject json = mapper.convertSubject(subject);

                if(json != null) {
                    String id = saveRecord(ASpaceClient.SUBJECT_ENDPOINT, json.toString(), "Subject->" + subject.getString("Subject"));

                    if (!id.equalsIgnoreCase(NO_ID)) {
                        String uri = ASpaceClient.SUBJECT_ENDPOINT + "/" + id;
                        subjectURIMap.put(arId, uri);
                        print("Copied Subject: " + subject + " :: " + id);
                        success++;
                    } else {
                        print("Fail -- Subject: " + subject);
                    }
                } else {
                    print("Error Convert Subject Record to JSON: " + subject);
                }
            }

            count++;
            updateProgress("Subjects", total, count);

            String subjectTerm = subject.getString("Subject");
            System.out.println(count + " :: Subject Term: " + subjectTerm);
        }

        updateRecordTotals("Subjects", total, success);

        // try freeing some memory
        freeMemory();
    }

    /**
     * Copy the name records
     *
     * @throws Exception
     */
    public void copyCreatorRecords() throws Exception {
        print("Copying Creator records ...");

        // update the progress so that the title changes
        updateProgress("Creator", 0, 0);

        JSONObject records = archonClient.getCreatorRecords();

        // now add the subject which should be saved as name records
        addSubjectAgentsToCreatorRecords(records);

        // these are used to update the progress bar
        int total = records.length();
        int count = 0;
        int success = 0;

        // get the inverted keys
        String[] invertedKeys = getKeysInInvertedOrder(records);

        for(String key: invertedKeys) {
            if(stopCopy) return;

            JSONObject creator = records.getJSONObject(key);

            String arId = creator.getString("ID");

            int creatorTypeId = creator.getInt("CreatorTypeID");

            JSONObject agentJS = mapper.convertCreator(creator, creatorTypeId);

            if(agentJS == null) {
                print("Unknown Creator Type for: " + creator.getString("Name") + " :: " + arId);
                continue;
            }

            // based on the type of name copy to the correct location
            String id = "";
            String uri = "";

            switch (creatorTypeId) {
                case 19:
                case 21:
                case 23:
                    // add any relationships if needed
                    if(creator.has("CreatorRelationships") && !creator.get("CreatorRelationships").equals(null)) {
                        try {
                            addCreatorRelationShips(records, creator, agentJS);
                        } catch(Exception e) {
                            print("Invalid Relationship Record For: " + creator.getString("Name"));
                        }
                    }

                    id = saveRecord(ASpaceClient.AGENT_PEOPLE_ENDPOINT, agentJS.toString(), "Creator_Person->" + creator.getString("Name"));
                    uri = ASpaceClient.AGENT_PEOPLE_ENDPOINT + "/" + id;
                    break;
                case 20:
                    id = saveRecord(ASpaceClient.AGENT_FAMILY_ENDPOINT, agentJS.toString(), "Creator_Family->" + creator.getString("Name"));
                    uri = ASpaceClient.AGENT_FAMILY_ENDPOINT + "/" + id;
                    break;
                case 22:
                    id = saveRecord(ASpaceClient.AGENT_CORPORATE_ENTITY_ENDPOINT, agentJS.toString(), "Creator_Corporate->" + creator.getString("Name"));
                    uri = ASpaceClient.AGENT_CORPORATE_ENTITY_ENDPOINT + "/" + id;
                    break;
                default:
                    break;
            }

            if (!id.equalsIgnoreCase(NO_ID)) {
                nameURIMap.put(arId, uri);
                print("Copied Creator: " + creator.get("Name") + " :: " + id);
                success++;
            } else {
                print("Failed -- Creator: " + creator.get("Name"));
            }

            count++;
            updateProgress("Names", total, count);
        }

        updateRecordTotals("Names", total, success);

        // refresh the database connection to prevent heap space error
        freeMemory();
    }

    /**
     * Method to add a creator relationship
     *
     * @param records
     * @param creator
     * @param agentJS
     */
    private void addCreatorRelationShips(JSONObject records, JSONObject creator, JSONObject agentJS) throws Exception {
        JSONArray relationships = creator.getJSONArray("CreatorRelationships");

        JSONArray relatedAgentsJA = new JSONArray();

        for(int i = 0; i < relationships.length(); i++) {
            JSONObject relationship = relationships.getJSONObject(i);

            int creatorRelationshipTypeID = relationship.getInt("CreatorRelationshipTypeID");

            JSONObject relatedCreator = records.getJSONObject(relationship.getString("RelatedCreatorID"));

            String relatedCreatorID = relatedCreator.getString("ID");

            if(creatorRelationshipTypeID == 2) {
                int creatorTypeID = relatedCreator.getInt("CreatorTypeID");
                switch (creatorTypeID) {
                    case 19: case 21: case 23:
                        String uri = nameURIMap.get(relatedCreatorID);
                        if(uri != null) {
                            JSONObject agentRelationJS = new JSONObject();

                            agentRelationJS.put("jsonmodel_type", "agent_relationship_parentchild");
                            agentRelationJS.put("relator", "is_parent_of");
                            agentRelationJS.put("ref", uri);
                            relatedAgentsJA.put(agentRelationJS);

                            System.out.println("Adding relationship " + relationship.toString());
                        } else {
                            System.out.println("Related creator not saved to ArchivesSpace ...");
                        }
                    default:
                        System.out.println("Unable to create a parent-child relationship for Creator " + relatedCreatorID + " - this creator is not a person.");
                        break;
                }
            }
        }

        if(relatedAgentsJA.length() > 1) {
            agentJS.put("related_agents", relatedAgentsJA);
        }
    }

    /**
     * Method to add subject agent record to the name record list
     * @param records
     */
    private void addSubjectAgentsToCreatorRecords(JSONObject records) throws Exception {
        for(JSONObject subject: subjectToAgentList) {
            int subjectTypeID = subject.getInt("SubjectTypeID");
            String sortName = subject.getString("Subject");
            String key = "subject_"  + subject.get("ID");

            subject.put("Name", sortName);
            subject.put("CreatorSourceID", 99); // this will get set to local

            if(subjectTypeID == 8) {
                // this is a person agent
                subject.put("CreatorTypeID", 19);
            } else if(subjectTypeID == 10) {
                // this is a family agent
                subject.put("CreatorTypeID", 20);
            } else {
                // must be a coorperate agent
                subject.put("CreatorTypeID", 22);
            }

            records.put(key, subject);
        }
    }

    /**
     * Method to copy classification records
     *
     * @throws Exception
     */
    public void copyClassificationRecords() throws Exception {
        print("Copying Classification records ...");

        // update the progress so that the title changes
        updateProgress("Classifications", 0, 0);

        JSONObject records = archonClient.getClassificationRecords();

        int total = records.length();
        int success = 0;
        int count = 0;

        // the total repositories
        int repositoryCount = repositoryURIMap.size() - 1;

        Iterator<String> keys = records.keys();
        while (keys.hasNext()) {
            if (stopCopy) return;

            String key = keys.next();

            JSONObject classification = records.getJSONObject(key);

            String arId = classification.getString("ID");

            JSONObject classificationJS = mapper.convertClassification(classification);

            // add any creator record
            addCreator(classificationJS, classification);

            // copy this classification to each repository. The empty ones will be deleted
            // once all the records are copied over
            int copyCount = 0;
            for(String repoURI: repositoryURIMap.values()) {
                if (!repoURI.endsWith("/1")) {
                    copyCount += copyClassificationToRepository(repoURI, arId, classification, classificationJS);
                }
            }

            // increment the success variable only if we copied the classification records to each of the
            // repositories
            if(copyCount == repositoryCount) {
                success++;
            }

            count++;
            updateProgress("classification", total, count);

            String title = classification.getString("Title");
            System.out.println(count + " :: Classification: " + title + "/ ID: " + arId);
        }

        updateRecordTotals("Classification", total, success);

        // try freeing some memory
        freeMemory();
    }

    /**
     * Method to copy a classification record to a particular repository. This is only needed
     * because classifications from Archon are not tied to a particular repository, but in
     * ASpace they are. In principle they really shouldn't be.
     *
     * @param repoURI
     * @param arId
     * @param classification
     * @param classificationJS
     * @throws Exception
     */
    private int copyClassificationToRepository(String repoURI, String arId, JSONObject classification, JSONObject classificationJS) throws Exception {
        // create the batch import JSON array and dummy URI now
        JSONArray batchJA = new JSONArray();

        String batchEndpoint = repoURI + ASpaceClient.BATCH_IMPORT_ENDPOINT;
        String classificationURI = repoURI + ASpaceClient.CLASSIFICATION_ENDPOINT + "/" + arId;
        String classificationTitle = classification.getString("Title");

        if (classificationJS != null) {
            classificationJS.put("uri", classificationURI);
            batchJA.put(classificationJS);

            // add the creator
            addCreator(classificationJS, classification);

            // add any classification terms here
            ArrayList<String> classificationTermURIs = new ArrayList<String>();
            JSONArray classificationChildren = classification.getJSONArray("children");

            for (int i = 0; i < classificationChildren.length(); i++) {
                if (stopCopy) return 0;

                JSONObject classificationTerm = classificationChildren.getJSONObject(i);

                JSONObject classificationTermJS = mapper.convertClassification(classificationTerm);

                String classificationTermTitle = classificationTerm.getString("Title");

                String cId = classificationTerm.getString("ID");

                String classificationTermURI = repoURI + ASpaceClient.CLASSIFICATION_TERM_ENDPOINT + "/" + cId;
                classificationTermURIs.add(classificationTermURI);

                if (classificationTermJS != null) {
                    classificationTermJS.put("uri", classificationTermURI);

                    classificationTermJS.put("classification", mapper.getReferenceObject(classificationURI));

                    String pId = classificationTerm.getString("ParentID");
                    if (!pId.equals(arId)) {
                        String parentURI = repoURI + ASpaceClient.CLASSIFICATION_TERM_ENDPOINT + "/" + pId;
                        classificationTermJS.put("parent", mapper.getReferenceObject(parentURI));
                    }

                    // add the linked agents aka Names records
                    addCreator(classificationTermJS, classificationTerm);

                    batchJA.put(classificationTermJS);

                    print("Added Classification Term: " + classificationTermTitle + " :: " + cId);
                } else {
                    print("Fail -- Classification Term to JSON: " + classificationTermTitle);
                }
            }

            // now save the classification and its classification terms using the batch endpoint
            String bids = saveRecord(batchEndpoint, batchJA.toString(2), arId);

            if (!bids.equals(NO_ID)) {
                String parentKey = repoURI + "_" + arId;

                if (!simulateRESTCalls) {
                    JSONObject bidsJS = new JSONObject(bids);
                    classificationURI = (new JSONArray(bidsJS.getString(classificationURI))).getString(0);

                    // now map all the classification term uris
                    for(String uri: classificationTermURIs) {
                        String key = repoURI + "_" + getIdFromURI(uri);
                        String classificationTermURI = (new JSONArray(bidsJS.getString(uri))).getString(0);

                        // need to store the parent classification key along with the
                        // term uri so we can keep track of which classifications are being used
                        String value = classificationTermURI + "," + parentKey;
                        classificationTermURIMap.put(key, value);
                    }
                }

                classificationURIMap.put(parentKey, classificationURI);

                print("Batch Copied Classification: " + classificationTitle + " :: " + parentKey);
            } else {
                print("Batch Copy Fail -- : " + classificationTitle);
                return 0;
            }
        }

        return 1;
    }

    /**
     * Method to copy accession records
     *
     * @throws Exception
     */
    public void copyAccessionRecords() throws Exception {
        print("Copying Accession records ...");

        // update the progress so that the title changes
        updateProgress("Accessions", 0, 0);

        JSONObject records = archonClient.getAccessionRecords();

        // these are used to update the progress bar
        int total = records.length();
        int count = 0;
        int success = 0;

        Iterator<String> keys = records.keys();
        while (keys.hasNext()) {
            if (stopCopy) return;

            String key = keys.next();

            JSONObject accession = records.getJSONObject(key);

            System.out.println("\n\nAccession Record:\n" + accession.toString(2));

            String arId = accession.getString("ID");
            String accessionTitle = accession.getString("Title");

            JSONObject accessionJS = mapper.convertAccession(accession);

            if (accessionJS != null) {
                String repoURI = getRepositoryURI(defaultRepositoryId);

                // add the subjects
                addSubjects(accessionJS, accession.getJSONArray("Subjects"), accessionTitle);

                // add the linked agents aka Names records
                JSONArray linkedAgentsJA = addCreators(accessionJS, accession.getJSONArray("Creators"), accessionTitle);

                // add the donors
                if(accession.has("Donor") && !accession.getString("Donor").isEmpty()) {
                    addDonor(linkedAgentsJA, accession, accessionTitle);
                }

                // add the classification
                if(accession.has("Classifications")) {
                    addClassifications(repoURI, accessionJS, accession.getJSONArray("Classifications"), accessionTitle);
                }

                // add an instance that holds the location information
                if(accession.has("Locations")) {
                    addAccessionInstances(accessionJS, accession.getJSONArray("Locations"), accessionTitle);
                }

                String uri = repoURI + ASpaceClient.ACCESSION_ENDPOINT;
                String id = saveRecord(uri, accessionJS.toString(), "Accession->" + accessionTitle);
                if (!id.equalsIgnoreCase(NO_ID)) {
                    uri = uri + "/" + id;

                    // now add the event objects
                    //addEvents(accession, repoURI, uri);

                    accessionURIMap.put(arId, uri);
                    print("Copied Accession: " + accessionTitle + " :: " + id);
                    success++;
                } else {
                    print("Fail -- Accession: " + accessionTitle);
                }
            } else {
                print("Fail -- Accession to JSON: " + accessionTitle);
            }

            count++;
            updateProgress("Accessions", total, count);
        }

        updateRecordTotals("Accessions", total, success);

        // refresh the database connection to prevent heap space error
        freeMemory();
    }

    /**
     * Method to copy the digital object records
     *
     * @throws Exception
     */
    public void copyDigitalObjectRecords() throws Exception {
        print("Copying Digital Object records ...");

        // update the progress so that the title changes
        updateProgress("Digital Objects", 0, 0);

        JSONObject records = archonClient.getDigitalObjectRecords();

        // these are used to update the progress bar
        digitalObjectTotal = 0;
        digitalObjectSuccess = 0;

        int total = records.length();
        int totalFree = 0;  // total number of free digital objects
        int count = 0;

        Iterator<String> keys = records.keys();
        while (keys.hasNext()) {
            if (stopCopy) return;

            String key = keys.next();

            // check to make sure we don't process the array of children objects
            if (key.equals("components")) continue;

            JSONObject digitalObject = records.getJSONObject(key);

            String arId = digitalObject.getString("ID");
            String digitalObjectTitle = digitalObject.getString("Title");

            // create the batch import JSON array and dummy URI now
            JSONArray batchJA = new JSONArray();

            String repoURI = getRepositoryURI(defaultRepositoryId);
            String batchEndpoint = repoURI + ASpaceClient.BATCH_IMPORT_ENDPOINT;
            String digitalObjectURI = repoURI + ASpaceClient.DIGITAL_OBJECT_ENDPOINT + "/" + arId;

            JSONObject digitalObjectJS = mapper.convertDigitalObject(digitalObject);

            if (digitalObjectJS != null) {
                digitalObjectJS.put("uri", digitalObjectURI);
                digitalObjectJS.put("jsonmodel_type", "digital_object");
                batchJA.put(digitalObjectJS);

                // add the subjects
                addSubjects(digitalObjectJS, digitalObject.getJSONArray("Subjects"), digitalObjectTitle);

                // add the linked agents aka Names records
                addCreators(digitalObjectJS, digitalObject.getJSONArray("Creators"), digitalObjectTitle);

                // add any archival objects here
                JSONArray digitalObjectChildren = digitalObject.getJSONArray("components");
                for (int i = 0; i < digitalObjectChildren.length(); i++) {
                    if (stopCopy) return;

                    JSONObject digitalObjectChild = digitalObjectChildren.getJSONObject(i);

                    JSONObject digitalObjectChildJS = mapper.convertToDigitalObjectComponent(digitalObjectChild);

                    String digitalObjectChildTitle = digitalObjectChild.getString("Title");

                    String cId = digitalObjectChild.getString("ID");

                    String digitalObjectChildURI = repoURI + ASpaceClient.DIGITAL_OBJECT_COMPONENT_ENDPOINT + "/" + cId;

                    if (digitalObjectChildJS != null) {
                        digitalObjectChildJS.put("uri", digitalObjectChildURI);
                        digitalObjectChildJS.put("jsonmodel_type", "digital_object_component");
                        digitalObjectChildJS.put("digital_object", mapper.getReferenceObject(digitalObjectURI));

                        batchJA.put(digitalObjectChildJS);

                        print("Added Digital Object Component: " + digitalObjectChildTitle + " :: " + cId);
                    } else {
                        print("Fail -- Digital Object Child to JSON: " + digitalObjectChildTitle);
                    }
                }

                // check to see we just not saving the digital objects or copying them now
                String digitalObjectListKey = null;

                if(saveDigitalObjectsWithResources) {
                    if(digitalObject.getInt("CollectionID") != 0) {
                        digitalObjectListKey = "collection_" + digitalObject.get("CollectionID");
                    } else if(digitalObject.getInt("CollectionContentID") != 0) {
                        digitalObjectListKey = "content_" + digitalObject.get("CollectionContentID");
                    } else {
                        print("No record to attach digital object " + digitalObjectTitle + "...");
                        print("Saving to default repository ...");
                    }
                }

                // call methods to actually save this digital object to the back end
                // or store it for saving when saving the resource records
                if(digitalObjectListKey != null) {
                    digitalObjectTotal++;
                    storeDigitalObject(batchJA, digitalObjectListKey);
                } else {
                    totalFree++;
                    saveDigitalObject(batchEndpoint, batchJA);
                }
            } else {
                print("Fail -- Digital Object to JSON: " + digitalObjectTitle);
            }

            count++;
            updateProgress("Digital Objects", total, count);
        }

        updateRecordTotals("Standalone Digital Objects", totalFree, digitalObjectSuccess);

        // refresh the database connection to prevent heap space error
        freeMemory();
    }

    /**
     * Method to save a digital object to the backend using the batch API
     *
     * @param batchEndpoint
     * @param batchJA
     * @return
     * @throws Exception
     */
    private String saveDigitalObject(String batchEndpoint, JSONArray batchJA) throws Exception {

        JSONObject digitalObjectJS = batchJA.getJSONObject(0);
        String digitalObjectURI = digitalObjectJS.getString("uri");
        String digitalObjectTitle = digitalObjectJS.getString("title");
        String arId  = digitalObjectURI.split("/")[4];

        String bids = saveRecord(batchEndpoint, batchJA.toString(2), arId);

        if (!bids.equals(NO_ID)) {
            if (!simulateRESTCalls) {
                JSONObject bidsJS = new JSONObject(bids);
                digitalObjectURI = (new JSONArray(bidsJS.getString(digitalObjectURI))).getString(0);
            }

            digitalObjectURIMap.put(arId, digitalObjectURI);

            digitalObjectSuccess++;

            print("Batch Copied Digital Object: " + digitalObjectTitle + " :: " + arId);
            return digitalObjectURI;
        } else {
            print("Batch Copy Fail -- Digital Object: " + digitalObjectTitle);
            return null;
        }
    }

    /**
     * Method to store the digital object in a hash map so it can be saved later
     *
     * @param batchJA
     *
     * @param digitalObjectListKey
     */
    private void storeDigitalObject(JSONArray batchJA, String digitalObjectListKey) {
        ArrayList<JSONArray> digitalObjectList = new ArrayList<JSONArray>();

        if(digitalObjectMap.containsKey(digitalObjectListKey)) {
            digitalObjectList = digitalObjectMap.get(digitalObjectListKey);
        } else {
            digitalObjectMap.put(digitalObjectListKey, digitalObjectList);
        }

        digitalObjectList.add(batchJA);
    }

    /**
     * Method to copy resource records from one database to the next
     *
     * @throws Exception
     * @param max Used to specify the maximum amount of records that should be copied
     */
    public void copyResourceRecords(int max) throws Exception {
        currentRecordType = "Resource Record";

        // first delete previously saved resource records if that option was selected by user
        if(deleteSavedResources) {
            deleteSavedResources();
        }

        // update the progress bar now to indicate the records are being loaded
        updateProgress("Resource Records", 0, 0);

        JSONObject records;
        if(cachedResourcesJS == null) {
            records = archonClient.getCollectionRecords();
            cachedResourcesJS = records;
            saveURIMaps();
        } else {
            records = cachedResourcesJS;
        }

        // reset the number of digital object copied successfully
        digitalObjectSuccess = 0;

        print("\nCopying " + records.length() + " Resource records ...\n");

        copyCount = 0; // keep track of the number of resource records copied

        // these are used to update the progress bar
        int total = records.length();
        int count = 0;
        int maxBatchLength = 0;

        // if we in debug mode, then set total to max
        if(debug && max < total) total = max;

        Iterator<String> keys = records.keys();
        while (keys.hasNext()) {
            count++;

            // check if to stop copy process
            if(stopCopy) {
                updateRecordTotals("Resource Records", total, copyCount);
                return;
            }

            // get the resource record
            JSONObject resource = records.getJSONObject(keys.next());

            // get the resource title
            String resourceTitle = resource.getString("Title");

            // get the record id
            String dbId = resource.getString("ID");

            // get the parent repository
            String repositoryID = resource.getString("RepositoryID");

            // get the at resource identifier to see if to only copy a specified resource
            // and to use for trouble shooting purposes
            String arId = resource.getString("CollectionIdentifier");

            currentRecordIdentifier = "DB ID: " + dbId + "\nAR ID: " + arId;
            currentRecordDBID = dbId;

            // set the atId in the mapper object
            mapper.setCurrentResourceRecordIdentifier(arId);

            // check to see if we are not just copy a single resource
            if(resourcesIDsList != null && !resourcesIDsList.contains(dbId)) {
                print("Not Copied: Resource not in list: " + resourceTitle);
                continue;
            }

            if (resourceURIMap.containsKey(dbId)) {
                print("Not Copied: Resource already in database " + resource);
                updateProgress("Resource Records", total, count);
                continue;
            }

            // create the batch import JSON array in case we need it
            JSONArray batchJA = new JSONArray();

            // hashmap used to store components so tree can be generated and sort orders be corrected
            HashMap<String, JSONObject> parentMap = new HashMap<String, JSONObject>();

            // we need to update the progress bar here
            updateProgress("Resource Records", total, count);

            // indicate we are copying the resource record
            print("Copying Resource: " + resourceTitle + " ,DB_ID: " + dbId);

            // get the main json object
            JSONObject resourceJS = mapper.convertResource(resource);

            if (resourceJS != null) {
                String repoURI = getRepositoryURI(repositoryID);
                String endpoint = repoURI + ASpaceClient.RESOURCE_ENDPOINT;
                String batchEndpoint = repoURI + ASpaceClient.BATCH_IMPORT_ENDPOINT;

                // add the subjects
                addSubjects(resourceJS, resource.getJSONArray("Subjects"), resourceTitle);

                // add the linked agents aka Names records
                addCreators(resourceJS, resource.getJSONArray("Creators"), resourceTitle);

                // add any linked classifications
                String classificationId = resource.getString("ClassificationID");
                if(!classificationId.isEmpty() && !classificationId.equals("0")) {
                    JSONArray classificationIds = new JSONArray();
                    classificationIds.put(classificationId);
                    addClassifications(repoURI, resourceJS, classificationIds, resourceTitle);
                }

                // add the instances
                //addInstances(resourceJS, resource, repository, repoURI + "/");
                addDigitalInstances(resourceJS, "collection_" + dbId, resourceTitle, batchEndpoint);

                // add the linked accessions
                //addRelatedAccessions(resourceJS, resource, repoURI + "/");

                // if we using batch import then we not not going to
                resourceJS.put("uri", endpoint + "/" + dbId);
                resourceJS.put("jsonmodel_type", "resource");
                batchJA.put(resourceJS);

                // set the resource URI and archival object endpoint
                String resourceURI = endpoint + "/" + dbId;
                String aoEndpoint = repoURI + ASpaceClient.ARCHIVAL_OBJECT_ENDPOINT;

                // add any archival objects here
                JSONObject resourceComponents = archonClient.getCollectionContentRecords(dbId);

                Iterator<String> ckeys = resourceComponents.sortedKeys();
                while (ckeys.hasNext()) {
                    JSONObject component = resourceComponents.getJSONObject(ckeys.next());
                    String title = component.getString("Title");

                    int contentType = component.getInt("ContentType");
                    String cid = component.getString("ID");

                    if(contentType != 2) {
                        JSONObject componentJS = mapper.convertResourceComponent(component);

                        if (componentJS != null) {
                            componentJS.put("resource", mapper.getReferenceObject(resourceURI));

                            // set the parent component if needed
                            int rootContentId = component.getInt("RootContentID");
                            int parentId = component.getInt("ParentID");
                            //int parentId = 0; // debug code

                            if (rootContentId != 0 && parentId != 0) {
                                parentId = getParentId(resourceComponents, componentJS, parentId);

                                // parent id might still actually be zero, which would cause a save error
                                if(parentId != 0) {
                                    String parentURI = aoEndpoint + "/" + parentId;
                                    componentJS.put("parent", mapper.getReferenceObject(parentURI));
                                }

                                addComponentToParentMap(parentMap, parentId, componentJS);
                            } else {
                                addComponentToParentMap(parentMap, 0, componentJS);
                            }

                            // add the subjects now
                            addSubjects(componentJS, component.getJSONArray("Subjects"), title);

                            // add the linked agents aka Names records
                            addCreators(componentJS, component.getJSONArray("Creators"), title);

                            // add the instances
                            //addInstances(componentJS, component, repositoryID, repoURI + "/");

                            addDigitalInstances(componentJS, "content_" + cid, resourceTitle, batchEndpoint);

                            // save this json record now to get the URI
                            componentJS.put("uri", aoEndpoint + "/" + cid);
                            componentJS.put("jsonmodel_type", "archival_object");
                            batchJA.put(componentJS);

                            print("Copied Resource Component: " + title + " :: " + cid + "\n");
                        } else {
                            print("Fail -- Resource Component to JSON: " + title);
                        }
                    } else {
                        print("Creating Instance for " + component.get("ID"));
                    }
                }

                freeMemory();

                print("Batch Copying Resource # " + count + " || Title: " + resourceTitle + "\nMax Depth: " + maxDepth);

                /* DEBUG CODE */
                int batchLength = batchJA.length();

                if(batchLength < batchLengthMin) {
                    print("\nRecord too small to copy ..." + resourceTitle + "\n");
                    continue;
                } else if(batchLength > batchLengthMax) {
                    print("\nRecord too big to copy ..." + resourceTitle + "\n");
                    continue;
                } else {
                    print("\nNumber of records in batch: " + batchLength + "\n");
                    if(batchLength > maxBatchLength) {
                        maxBatchLength = batchLength;
                    }
                }

                /* get a new session to save memory on aspace side
                if(batchLength > 1) {
                    aspaceClient.getSession();
                }*/
                /* END DEBUG */

                // redo the sort order so we don't have any collisions and things are sorted correctly
                redoComponentSortOrder(parentMap);

                String bids = saveRecord(batchEndpoint, batchJA.toString(2), arId);
                if (!bids.equals(NO_ID)) {
                    if (!simulateRESTCalls) {
                        JSONObject bidsJS = new JSONObject(bids);
                        resourceURI = (new JSONArray(bidsJS.getString(resourceURI))).getString(0);
                    }

                    updateResourceURIMap(dbId, resourceURI);
                    incrementCopyCount();

                    print("Batch Copied Resource: " + resourceTitle + " :: " + resourceURI);
                } else {
                    print("Batch Copy Fail -- Resource: " + resourceTitle);
                }
            } else {
                print("Fail -- Resource to JSON: " + resourceTitle);
            }

            if (debug && copyCount >= max) break;
        }

        // update the number of digital records that were copied during processing of collection records
        updateRecordTotals("Instance Digital Objects", digitalObjectTotal, digitalObjectSuccess);

        // wait for any threads to finish before returning if we running more than one
        // thread to copy
        while(getTotalASpaceClients() != 0 && !stopCopy) {
            print("Waiting for last set of records to be copied ...");
            Thread.sleep(60000); //wait 60 seconds before checking again
        }

        // update the number of resource actually copied
        updateRecordTotals("Resource Records", total, copyCount);

        System.out.println("\n\nMaximum Batch Length: " + maxBatchLength);
    }

    /**
     * Method to add an instance to resource, or resource component record
     *
     * @param json
     * @param recordTitle the title of the record
     * @throws Exception
     */
    private synchronized void addDigitalInstances(JSONObject json, String recordKey, String recordTitle, String batchEndpoint) throws Exception {
        JSONArray instancesJA = new JSONArray();

        if(digitalObjectMap.containsKey(recordKey)) {
            ArrayList<JSONArray> digitalObjectList = digitalObjectMap.get(recordKey);

            for(JSONArray batchJA: digitalObjectList) {
                String digitalObjectURI = saveDigitalObject(batchEndpoint, batchJA);

                if(digitalObjectURI != null) {
                    JSONObject instanceJS = new JSONObject();
                    instanceJS.put("instance_type", "digital_object");
                    instanceJS.put("digital_object", mapper.getReferenceObject(digitalObjectURI));
                    instancesJA.put(instanceJS);

                    if (debug) print("Added Digital Object Instance to " + recordTitle);
                }
            }

            if (instancesJA.length() != 0) {
                json.put("instances", instancesJA);
            }
        }
    }

    /**
     * Method to return the parent id. This is needed in case we have a parent that a type 2 so we actually
     * need to get the grandparent, or even older is this case
     *
     * @param resourceComponents
     * @param componentJS
     * @param parentId
     * @return
     */
    private int getParentId(JSONObject resourceComponents, JSONObject componentJS, Integer parentId) throws Exception {
        while (parentId != 0) {
            JSONObject parent = resourceComponents.getJSONObject(parentId.toString());

            if(parent.getInt("ContentType") == 2) {
                parentId = parent.getInt("ParentID");

                // save the sort order number of the parent for proper sorting
                String paddedSortOrder = String.format("%05d", parent.getInt("SortOrder"));
                componentJS.put("sort_key2", paddedSortOrder);

                // add an instance that will represent this parent

            } else {
                return parentId;
            }
        }

        return 0;
    }

    /**
     * Method to add json object to the parent map for generating the object tree more efficiently
     *
     * @param parentMap
     * @param parentId
     * @param componentJS
     */
    private void addComponentToParentMap(HashMap<String, JSONObject> parentMap, int parentId, JSONObject componentJS) throws Exception {
        String key  = "parent_" + parentId;
        String sort_key = componentJS.getString("sort_key2") + "_" + componentJS.getString("sort_key1");
        JSONObject childrenJS = new JSONObject();

        if(parentMap.containsKey(key)) {
            childrenJS = parentMap.get(key);
        } else {
            parentMap.put(key, childrenJS);
        }

        childrenJS.put(sort_key, componentJS);
    }

    /**
     * Method to redo the sort order for the components
     */
    private void redoComponentSortOrder(HashMap<String, JSONObject> parentMap) throws Exception {
        for(String key: parentMap.keySet()) {
            JSONObject childrenJS = parentMap.get(key);

            System.out.println("Redo sort order for: " + key + " number of children: " + childrenJS.length());

            int i = 1;
            Iterator<String> keys = childrenJS.sortedKeys();
            while(keys.hasNext()) {
                JSONObject componentJS = childrenJS.getJSONObject(keys.next());
                componentJS.put("position", i);
                i++;
            }
        }
    }

    /**
     * Add the subjects to the json resource, or resource component record
     *
     * @param json   The json representation of the AR record
     * @param subjectIds The subjects ids
     * @param recordTitle The record parent record title
     * @throws Exception
     */
    private synchronized void addSubjects(JSONObject json, JSONArray subjectIds, String recordTitle) throws Exception {
        JSONArray subjectsJA = new JSONArray();

        for (int i = 0; i < subjectIds.length(); i++) {
            String id = subjectIds.getString(i);
            String subjectURI = subjectURIMap.get(id);

            if (subjectURI != null) {
                subjectsJA.put(mapper.getReferenceObject(subjectURI));

                if (debug) print("Added subject to " + recordTitle);
            } else {
                print("No mapped subject found ...");
            }
        }

        // if we had any subjects add them parent json record
        if (subjectsJA.length() != 0) {
            json.put("subjects", subjectsJA);
        }
    }

    /**
     * Add the name creator to the resource or resource component record
     *
     * @param json   object record
     * @param record
     * @throws Exception
     */
    private void addCreator(JSONObject json, JSONObject record) throws Exception {
        String creatorID = record.getString("CreatorID");
        String nameURI = nameURIMap.get(creatorID);

        if (nameURI != null) {
            json.put("creator", mapper.getReferenceObject(nameURI));
        } else if (!creatorID.isEmpty() && !creatorID.equals("0")) {
            print("No mapped creator found ...");
        }
    }

    /**
     * Add the names to the resource or resource component record
     *
     * @param json       object record
     * @param creatorIds The array of subject ids
     * @throws Exception
     */
    private synchronized JSONArray addCreators(JSONObject json, JSONArray creatorIds, String recordTitle) throws Exception {
        JSONArray linkedAgentsJA = new JSONArray();

        for (int i = 0; i < creatorIds.length(); i++) {
            String id = creatorIds.getString(i);
            String nameURI = nameURIMap.get(id);

            if (nameURI != null) {
                JSONObject linkedAgentJS = new JSONObject();

                linkedAgentJS.put("role", "creator");
                linkedAgentJS.put("ref", nameURI);
                linkedAgentsJA.put(linkedAgentJS);

                if (debug) print("Added creator to " + recordTitle);
            } else {
                print("No mapped name found ...");
            }
        }

        // if we had any subjects add them parent json record
        json.put("linked_agents", linkedAgentsJA);

        return linkedAgentsJA;
    }

    /**
     * Method to create an agent record and add it to the Accession record as a donor
     *
     * @param linkedAgentsJA
     * @param accession
     * @param accessionTitle
     */
    private void addDonor(JSONArray linkedAgentsJA, JSONObject accession, String accessionTitle) throws Exception {
        String donorName = accession.getString("Donor");

        String nameURI = getDonorURI(donorName, accession, accessionTitle);

        if (nameURI != null) {
            JSONObject linkedAgentJS = new JSONObject();

            linkedAgentJS.put("role", "source");
            linkedAgentJS.put("relator", "dnr");
            linkedAgentJS.put("ref", nameURI);
            linkedAgentsJA.put(linkedAgentJS);

            if (debug) print("Added donor to " + accessionTitle);
        } else {
            print("No mapped name found ...");
        }
    }

    /**
     * Method to see if we need to save an agent record for the donor or just use an existing one
     *
     * @param donorName
     * @param accession
     * @param accessionTitle
     * @return
     */
    private String getDonorURI(String donorName, JSONObject accession, String accessionTitle) throws Exception {
        String uri = null;

        if(nameURIMap.containsKey(donorName)) {
            uri = nameURIMap.get(donorName);
        } else {
            JSONObject agentJS = mapper.convertAccessionDonor(accession);

            if (agentJS != null) {
                String id = saveRecord(ASpaceClient.AGENT_PEOPLE_ENDPOINT, agentJS.toString(), "Donor_Person->" + donorName);

                if (!id.equalsIgnoreCase(NO_ID)) {
                    uri = ASpaceClient.AGENT_PEOPLE_ENDPOINT + "/" + id;
                    nameURIMap.put(donorName, uri);
                    print("Copied Donor: " + donorName + " :: " + id);
                } else {
                    print("Failed -- Donor: " + donorName);
                }
            }
        }

        return uri;
    }

    /**
     * Add the classifications to the json resource, or accession record
     *
     * @param repoURI This is needed since classification are linked to specific repository
     * @param json The json representation of the AR record
     * @param classificationIds  The classification ids
     * @param recordTitle The record parent record title
     * @throws Exception
     */
    private synchronized void addClassifications(String repoURI, JSONObject json, JSONArray classificationIds, String recordTitle) throws Exception {
        JSONArray classificationsJA = new JSONArray();

        for (int i = 0; i < classificationIds.length(); i++) {
            String key = repoURI + "_" + classificationIds.getString(i);
            String classificationURI = getClassificationURI(key);

            if (classificationURI != null) {
                classificationsJA.put(mapper.getReferenceObject(classificationURI));

                if (debug) print("Added classification to " + recordTitle);
            } else {
                print("No mapped classification found ...");
            }
        }

        // if we had any subjects add them parent json record
        if (classificationsJA.length() != 0) {
            json.put("classifications", classificationsJA);
        }
    }

    /**
     * Method to add an instance to an Accession record
     *
     * @param accessionJS
     * @param locations
     * @param accessionTitle
     */
    private synchronized void addAccessionInstances(JSONObject accessionJS, JSONArray locations, String accessionTitle) throws Exception {
        JSONArray instancesJA = new JSONArray();

        for (int i = 0; i < locations.length(); i++) {
            JSONObject instanceJS = new JSONObject();
            JSONObject location = locations.getJSONObject(i);

            // set the type
            instanceJS.put("instance_type", "accession");

            // add the container now
            JSONObject containerJS = new JSONObject();

            containerJS.put("type_1", "object");
            containerJS.put("indicator_1", location.get("Content"));

            // add the extent information
            if(location.has("Extent")) {
                containerJS.put("container_extent", location.getString("Extent"));
                containerJS.put("container_extent_type", enumUtil.getASpaceExtentType(location.getInt("ExtentUnitID")));
            }

            // add a location record record now
            String building = location.getString("Location");
            String coordinate1 = location.getString("RangeValue");
            String coordinate2 = location.getString("Section");
            String coordinate3 = location.getString("Shelf");

            String locationURI = getLocationURI(building, coordinate1, coordinate2, coordinate3);

            if(locationURI != null) {
                Date date = new Date(); // this is need to have valid container_location json record

                JSONArray locationsJA = new JSONArray();

                JSONObject locationJS = new JSONObject();
                locationJS.put("status", "current");
                locationJS.put("start_date", date);
                locationJS.put("ref", locationURI);

                locationsJA.put(locationJS);

                // put all the records together now
                containerJS.put("container_locations", locationsJA);
            }

            instanceJS.put("container", containerJS);

            instancesJA.put(instanceJS);
        }

        // if we had any instances add them parent json record
        if (instancesJA.length() != 0) {
            accessionJS.put("instances", instancesJA);
        }
    }

    /**
     * Method to either create or return a location record based on the coordinate and so on
     * @param building
     * @param coordinate1
     * @param coordinate2
     * @param coordinate3
     * @return
     */
    private String getLocationURI(String building, String coordinate1, String coordinate2, String coordinate3) throws Exception {
        String key = building;

        // lets create a JSON object for the location in case we need to save it
        JSONObject locationJS = new JSONObject();
        locationJS.put("building", building);

        if (!coordinate1.equals("null") && !coordinate1.isEmpty()) {
            locationJS.put("coordinate_1_label", "Range");
            locationJS.put("coordinate_1_indicator", coordinate1);
            key += "-" + coordinate1;
        } else {
            // put in dummy range so record saves
            locationJS.put("coordinate_1_label", "Range");
            locationJS.put("coordinate_1_indicator", "0");
            key += "-0";
        }

        if (!coordinate2.equals("null") && !coordinate2.isEmpty()) {
            locationJS.put("coordinate_2_label", "Section");
            locationJS.put("coordinate_2_indicator", coordinate2);
            key += "-" + coordinate2;
        }

        if (!coordinate3.equals("null") && !coordinate3.isEmpty()) {
            locationJS.put("coordinate_3_label", "Shelf");
            locationJS.put("coordinate_3_indicator", coordinate3);
            key += "-" + coordinate3;
        }

        if(locationURIMap.containsKey(key)) {
            return locationURIMap.get(key);
        } else {
            String id = saveRecord(ASpaceClient.LOCATION_ENDPOINT, locationJS.toString(), "Location->" + key);

            if (!id.equalsIgnoreCase(NO_ID)) {
                String uri = ASpaceClient.LOCATION_ENDPOINT + "/" + id;
                locationURIMap.put(key, uri);
                print("Copied Location: " + key + " :: " + id);
                return uri;
            } else {
                print("Fail -- Location: " + key);
                return null;
            }
        }
    }

    /**
     * Method to start the start the time watch
     */
    private void startWatch() {
        stopWatch = new StopWatch();
        stopWatch.start();
    }

    private String stopWatch() {
        stopWatch.stop();
        return stopWatch.getPrettyTime();
    }

    /**
     * Method to return the status of getting the session needed to create certain records
     *
     * @return
     */
    public boolean getSession() {
        if (simulateRESTCalls) return true;

        boolean connected = aspaceClient.getSession();

        if (connected) {
            aspaceInformation = aspaceClient.getArchivesSpaceInformation();
            setASpaceVersion();
        }

        return connected;


    }

    /**
     * Method to extract the aspace version from the information return from the backend
     */
    private void setASpaceVersion() {
        try {
            JSONObject infoJS = new JSONObject(aspaceInformation);
            aspaceVersion = infoJS.getString("archivesSpaceVersion");
        } catch (Exception e) {
        }
    }

    /**
     * Method to return the aspace version
     */
    public String getASpaceVersion() {
        return aspaceVersion;
    }

    /** Method to add to resource map in a thread safe manner
     *
     * @param oldIdentifier
     * @param uri
     */
    private synchronized void updateResourceURIMap(String oldIdentifier, String uri) {
        resourceURIMap.put(oldIdentifier, uri);
        saveURIMaps();
    }

    /**
     * Method to increment the number of resource records copied
     */
    private synchronized void incrementCopyCount() {
        copyCount++;
    }

    /**
     * Method to delete any previously saved resource records on the ASpace backend.
     * Useful for testing purposes, but not much else
     */
    private void deleteSavedResources() {
        print("\nNumber of Resources to Delete: " + resourceURIMap.size());
        ArrayList<String> deletedKeys = new ArrayList<String>();

        // initialize the progress bar
        updateProgress("Resource", resourceURIMap.size(), -1);

        int count = 1;
        for(String key: resourceURIMap.keySet()) {
            try {
                String uri = resourceURIMap.get(key);
                print("Deleting Resource: " + uri);

                String message = aspaceClient.deleteRecord(uri);
                print(message + "\n");
                deletedKeys.add(key);

                // update the progress bar
                updateProgress("Resource", 0, count);
                count++;
            } catch (Exception e) {
                e.printStackTrace();
                print("Error deleting ... \n" + e.getMessage());
            }
        }

        // remove the deleted keys now, since we can't modify a
        // hashmap while we still iterating over it
        for(String key: deletedKeys) {
            resourceURIMap.remove(key);
        }
    }

    /**
     * Method to return the new repository for a given domain object.
     *
     * @param repoID
     * @return The URI of the new repository
     */
    private synchronized String getRepositoryURI(String repoID) {
        // check to see if old repo is not null. If it is then we need to just return anyone
        // since this should never occur in a properly formatted AT database
        if(repoID != null && !repoID.isEmpty() && repositoryURIMap.containsKey(repoID)) {
            return repositoryURIMap.get(repoID);
        } else {
            return "/repositories/2";
        }
    }

    /**
     * Method to return the new repository for a given domain object.
     *
     * @param key The hash map key
     * @return The URI of the new repository
     */
    private synchronized String getClassificationURI(String key) {
        if (classificationURIMap.containsKey(key)) {
            linkedClassificationSet.add(key);
            return classificationURIMap.get(key);
        } else if (classificationTermURIMap.containsKey(key)) {
            String sa[] = classificationTermURIMap.get(key).split(",");
            linkedClassificationSet.add(sa[1]);
            return sa[0];
        }else {
            return null;
        }
    }

    /**
     * Method to save the record that takes into account running in stand alone
     * or within the AT
     *
     * @param endpoint to make post to
     * @param jsonText record
     */
    public synchronized String saveRecord(String endpoint, String jsonText, String atId) {
        return saveRecord(endpoint, jsonText, null, atId);
    }

    /**
     * Method to save the record that takes into account running in stand alone
     * or within the AT
     *
     * @param endpoint to make post to
     * @param jsonText record
     * @param params   parameters to pass to service
     */
    public synchronized String saveRecord(String endpoint, String jsonText, NameValuePair[] params, String atId) {
        String id = NO_ID;

        try {
            // Make sure we don't try to print out a batch import record since they can
            // be thousands of lines long
            if(endpoint.contains(ASpaceClient.BATCH_IMPORT_ENDPOINT)) {
                print("Route: " + endpoint + "\nBatch Record Length: " + jsonText.length() + " bytes");
            } else {
                print("Route: " + endpoint + "\n" + jsonText);
            }

            // we may want to save this record to the file system for debuging purposes
            if(recordDumpDirectory != null) {
                saveRecordToFile(jsonText);
            }

            if(simulateRESTCalls) {
                id = "1001" + randomString.nextString();
                Thread.sleep(2);
            } else {
                id = aspaceClient.post(endpoint, jsonText, params, atId);
            }
        } catch (Exception e) {
            if(endpoint.contains(ASpaceClient.BATCH_IMPORT_ENDPOINT)) {
                print("Error saving batch import record ...");
            } else {
                print("Error saving record " + jsonText);
            }

            incrementErrorCount();
            incrementASpaceErrorCount();
        }

        return id;
    }

    /**
     * Method to save the json record to the file system. Mainly used for debug purposes
     *
     * @param jsonText
     */
    private void saveRecordToFile(String jsonText) {
        String filename = currentRecordType.replace(" ", "_") + currentRecordDBID + ".json";
        File file = new File(recordDumpDirectory, filename);
        FileManager.saveTextData(file, jsonText);
    }

    /**
     * Method to increment the error count
     */
    private synchronized void incrementErrorCount() {
        saveErrorCount++;

        if(errorCountLabel != null) {
            errorCountLabel.setText(saveErrorCount + " and counting ...");
        }
    }

    /**
     * Method to increment the aspace error count that occur when saving to the
     * backend
     */
    private synchronized void incrementASpaceErrorCount() {
        aspaceErrorCount++;
    }

    /**
     * Convenient print method for printing string in the text console in the future
     *
     * @param string
     */
    public synchronized void print(String string) {
        if(outputConsole != null) {
            messageCount++;

            if(messageCount < MAX_MESSAGES) {
                outputConsole.append(string + "\n");
            } else {
                messageCount = 0;
                outputConsole.setText(string + "\n");
            }
        } else {
            System.out.println(string);
        }
    }

    /**
     * Method to update the progress bar if not running in command line mode
     *
     * @param recordType
     * @param total
     * @param count
     */
    private synchronized void updateProgress(String recordType, int total, int count) {
        if(progressBar == null) return;

        if(count == -1) {
            progressBar.setMinimum(0);
            progressBar.setMaximum(total);
            progressBar.setString("Deleting " + total + " " + recordType);
        } else if(count == 0) {
            progressBar.setMinimum(0);
            progressBar.setMaximum(1);
            progressBar.setString("Loading " + recordType);
        } else if(count == 1) {
            progressBar.setMinimum(0);
            progressBar.setMaximum(total);
            progressBar.setString("Copying " + total + " " + recordType);
        }

        progressBar.setValue(count);
    }

    /**
     * Method to update the record totals
     *
     * @param recordType
     * @param total
     * @param success
     */
    private void updateRecordTotals(String recordType, int total, int success) {
        float percent = (new Float(success)/new Float(total))*100.0f;
        recordTotals.add(recordType + " : " + success + " / " + total + " (" + String.format("%.2f", percent) + "%)");
    }

    /**
     * Method to return the number of errors when saving records
     *
     * @return
     */
    public int getSaveErrorCount() {
        return saveErrorCount;
    }

    /**
     * Method to add an error message to the buffer
     *
     * @param message
     */
    public synchronized void addErrorMessage(String message) {
        errorBuffer.append(message).append("\n");
        incrementErrorCount();
    }

    /**
     * Method to return the error messages that occurred during the transfer process
     * @return
     */
    public String getSaveErrorMessages() {
        int errorsAndWarnings = saveErrorCount - aspaceErrorCount;

        String errorMessage = "RECORD CONVERSION ERRORS/WARNINGS ( " + errorsAndWarnings + " ) ::\n\n" + errorBuffer.toString() +
                "\n\n\nRECORD SAVE ERRORS ( " + aspaceErrorCount + " ) ::\n\n" + aspaceClient.getErrorMessages() +
                "\n\nTOTAL COPY TIME: " + stopWatch.getPrettyTime() +
                "\n\nNUMBER OF RECORDS COPIED: \n" + getTotalRecordsCopiedMessage();

        return errorMessage;
    }

    /**
     * Method to do certain task after the copy has completed
     */
    public void cleanUp() {
        copying = false;

        // remove any un-liked classifications
        deleteUnlinkedClassifications();

        String totalRecordsCopied = getTotalRecordsCopiedMessage();

        print("\n\nFinish coping data ... Total time: " + stopWatch.getPrettyTime());
        print("\nNumber of Records copied: \n" + totalRecordsCopied);

        print("\nNumber of errors/warnings: " + saveErrorCount);
    }

    /**
     * Method to delete all the unlinked classifications
     */
    public void deleteUnlinkedClassifications() {
        for(String key: classificationURIMap.keySet()) {
            if(!linkedClassificationSet.contains(key)) {
                print("Deleting unlinked classification: " + key);
                String uri = classificationURIMap.get(key);

                if(!simulateRESTCalls) {
                    try {
                        aspaceClient.deleteRecord(uri);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                print("Linked classification: " + key);
            }
        }
    }

    /**
     * A method to display the short names and id of repositories. Useful when debugging
     */
    public void displayRepositories() {
        print("\n\nRepository Names: ");

        for(String repo: repositoryNamesList) {
            print(repo);
        }
    }

    /**
     * Method to return the current status of the migration
     *
     * @return
     */
    public String getCurrentProgressMessage() {
        int errorsAndWarnings = saveErrorCount - aspaceErrorCount;

        String totalRecordsCopied = getTotalRecordsCopiedMessage();

        String errorMessages = "RECORD CONVERSION ERRORS/WARNINGS ( " + errorsAndWarnings + " ) ::\n\n" + errorBuffer.toString() +
                "\n\n\nRECORD SAVE ERRORS ( " + aspaceErrorCount + " ) ::\n\n" + aspaceClient.getErrorMessages();

        String message = errorMessages +
                "\n\nRunning for: " + stopWatch.getPrettyTime() +
                "\n\nCurrent # of Records Copied: \n" + totalRecordsCopied;

        return message;
    }

    /**
     * Method to return string with total records copied
     * @return
     */
    private String getTotalRecordsCopiedMessage() {
        String totalRecordsCopied = "";

        for(String entry: recordTotals) {
            totalRecordsCopied += entry + "\n";
        }

        return totalRecordsCopied;
    }

    /**
     * Method to set the boolean which specifies whether to stop copying the resources
     */
    public void stopCopy() {
        stopCopy = true;
    }

    /**
     * Method to check if the copying process is running
     *
     * @return
     */
    public boolean isCopying() {
        return copying;
    }

    /**
     * Method to set the whether the copying process is running
     *
     * @param copying
     */
    public void setCopying(boolean copying) {
        this.copying = copying;
    }

    /**
     * Method to try and free some memory by refreshing the hibernate session and running GC
     */
    private void freeMemory() {
        if(outputConsole != null) {
            outputConsole.setText("");
        }

        Runtime runtime = Runtime.getRuntime();

        long freeMem = runtime.freeMemory();
        System.out.println("\nFree memory before GC: " + freeMem/1048576L + "MB");

        runtime.gc();

        freeMem = runtime.freeMemory();
        System.out.println("Free memory after GC:  " + freeMem/1048576L + "MB");

        System.out.println("Number of client threads: "  + getTotalASpaceClients() + "\n");
    }

    /**
     * increment the number of aspace clients
     */
    private synchronized void incrementTotalASpaceClients() {
        totalASpaceClients++;
    }

    /**
     * Method to decrement the total number of asapce client
     */
    private synchronized void decrementTotalASpaceClients() {
        totalASpaceClients--;
    }

    /**
     * Method to return the keys in inverted order. Used to correctly assign creator
     * relationships, otherwise a relationship is created to an agent that's not yet created
     *
     * @param records
     * @return
     */
    private String[] getKeysInInvertedOrder(JSONObject records) {
        String[] invertedKeys = new String[records.length()];
        int start = records.length() - 1;

        Iterator<String> keys = records.sortedKeys();
        while (keys.hasNext()) {
            invertedKeys[start] = keys.next();
            start--;
        }

        return invertedKeys;
    }

    /**
     * Method to get the current aspace client
     *
     * @return current number of aspace client
     */
    private synchronized int getTotalASpaceClients() {
        return totalASpaceClients;
    }

    /**
     * Method to save the URI maps to a binary file
     */
    public void saveURIMaps() {
        HashMap uriMap = new HashMap();

        // only save maps we are going to need,
        // or we not generating from ASpace backend data
        uriMap.put(LOCATION_KEY, locationURIMap);
        uriMap.put(SUBJECT_KEY, subjectURIMap);
        uriMap.put(NAME_KEY, nameURIMap);
        uriMap.put(ACCESSION_KEY, accessionURIMap);
        uriMap.put(DIGITAL_OBJECT_KEY, digitalObjectURIMap);
        uriMap.put(RESOURCE_KEY, resourceURIMap);

        // store the record totals array list here also
        uriMap.put(RECORD_TOTAL_KEY, recordTotals);

        /* DEBUG CODE. Store the return resource records
        if(cachedResourcesJS != null) {
            String key = archonClient.getHost() + ArchonClient.COLLECTION_ENDPOINT;
            uriMap.put(key, cachedResourcesJS.toString());
        }*/

        // save to file system now
        print("\nSaving URI Maps ...");

        try {
            FileManager.saveObjectToFile(uriMapFile, uriMap);
        } catch (Exception e) {
            print("Unable to save URI map file " + uriMapFile.getName());
        }
    }

    /**
     * Method to load the save URI maps
     */
    public void loadURIMaps() {
        try {
            HashMap uriMap  = (HashMap) FileManager.getObjectFromFile(uriMapFile);

            locationURIMap = (HashMap<String,String>)uriMap.get(LOCATION_KEY);
            subjectURIMap = (HashMap<String,String>)uriMap.get(SUBJECT_KEY);
            nameURIMap = (HashMap<String,String>)uriMap.get(NAME_KEY);
            accessionURIMap = (HashMap<String,String>)uriMap.get(ACCESSION_KEY);
            digitalObjectURIMap = (HashMap<String,String>)uriMap.get(DIGITAL_OBJECT_KEY);
            resourceURIMap = (HashMap<String,String>)uriMap.get(RESOURCE_KEY);

            // load the record totals so far
            if(uriMap.containsKey(RECORD_TOTAL_KEY)) {
                recordTotals = (ArrayList<String>)uriMap.get(RECORD_TOTAL_KEY);
            }

            /* load the caches resource records
            String key = archonClient.getHost() + ArchonClient.COLLECTION_ENDPOINT;
            if(uriMap.containsKey(key)) {
                String jsonText = (String)uriMap.get(key);
                cachedResourcesJS = new JSONObject(jsonText);
            }*/

            print("Loaded URI Maps");
        } catch (Exception e) {
            print("Unable to load URI map file: " + uriMapFile.getName());
        }
    }

    /**
     * Method to see if the URI map file exist
     *
     * @return
     */
    public boolean uriMapFileExist() {
        return uriMapFile.exists();
    }

    /**
     * Method used to simulate the REST calls. Useful for testing memory usage and for setting baseline
     * data transfer time
     *
     * @param simulateRESTCalls
     */
    public void setSimulateRESTCalls(boolean simulateRESTCalls) {
        this.simulateRESTCalls = simulateRESTCalls;
    }

    /**
     * Method to specify whether to delete the save resources.
     *
     * @param deleteSavedResources
     */
    public void setDeleteSavedResources(boolean deleteSavedResources) {
        this.deleteSavedResources = deleteSavedResources;
    }

    /**
     * Method to set the batch size limits
     *
     * @param min
     * @param max
     */
    public void setBatchSizeLimits(int min, int max) {
        batchLengthMin = min;
        batchLengthMax = max;
    }

    /**
     * Method to set the resources to copy
     *
     * @param resourcesIDsList
     */
    public void setResourcesToCopyList(ArrayList<String> resourcesIDsList) {
        if(resourcesIDsList.size() != 0) {
            this.resourcesIDsList = resourcesIDsList;
        } else {
            this.resourcesIDsList = null;
        }
    }

    /**
     * Method to get the current
     * @return
     */
    public String getCurrentRecordInfo()  {
        String info = "Current Record Type: " + currentRecordType +
                "\nRecord Identifier : " + currentRecordIdentifier;

        return info;
    }

    /**
     * Method to set the record dump directory. Used for debugging purposes
     *
     * @param directory
     */
    public void setRecordDumpDirectory(File directory) {
        this.recordDumpDirectory = directory;
    }

    /**
     * A Method to remove bbcode from the title of Archival Objects
     *
     * @param title
     */
    private String cleanTitle(String title) {
        if(bbcodeToHTML) {
            title = title.replace("[i]", "<i>").replace("[/i]", "</i>");
        } else if(bbcodeToBlank) {
            title = title.replace("[i]", "").replace("[/i]", "");
        }

        return title;
    }

    /**
     * Method to extract the id from a given URI
     * @param uri
     * @return
     */
    private String getIdFromURI(String uri) {
        int beginIndex = uri.lastIndexOf("/") + 1;
        return uri.substring(beginIndex);
    }

    /**
     * Method to test the conversion without having to startup the gui
     *
     * @param args
     */
    public static void main(String[] args) throws JSONException {
        String host = "http://archives-dev.library.illinois.edu/archondev/uiuctest";
        ArchonClient archonClient = new ArchonClient(host, "aspace", "We$tbr0ok");

        //String host = "http://archives-dev.library.illinois.edu/archondev/tracer";
        //String host = "http://archives-dev.library.illinois.edu/archondev/houston";
        //String host = "http://localhost/~nathan/archon";
        //ArchonClient archonClient = new ArchonClient(host, "admin", "admin");

        archonClient.getSession();

        ASpaceCopyUtil aspaceCopyUtil  = new ASpaceCopyUtil(archonClient, "http://54.227.35.51:8089", "admin", "admin");
        aspaceCopyUtil.getSession();
        aspaceCopyUtil.setSimulateRESTCalls(false);
        aspaceCopyUtil.setDefaultRepositoryId("1");

        try {
            /*ArrayList<String> recordList = new ArrayList<String>();
            recordList.add("4719");
            aspaceCopyUtil.setResourcesToCopyList(recordList);

            //File recordDirectory = new File("/Users/nathan/temp/JSON_Records");
            //aspaceCopyUtil.setRecordDumpDirectory(recordDirectory);*/

            aspaceCopyUtil.copyEnumRecords();
            aspaceCopyUtil.copyRepositoryRecords();
            aspaceCopyUtil.mapRepositoryGroups();

            // display the repository
            aspaceCopyUtil.displayRepositories();

            aspaceCopyUtil.copyUserRecords();
            aspaceCopyUtil.copySubjectRecords();
            aspaceCopyUtil.copyCreatorRecords();
            aspaceCopyUtil.copyClassificationRecords();
            aspaceCopyUtil.copyAccessionRecords();
            aspaceCopyUtil.copyDigitalObjectRecords();
            aspaceCopyUtil.copyResourceRecords(100000);

            // removed all unused classifications
            aspaceCopyUtil.deleteUnlinkedClassifications();

            // print out the error messages
            System.out.println("\n\nSave Errors:\n" + aspaceCopyUtil.getSaveErrorMessages());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
