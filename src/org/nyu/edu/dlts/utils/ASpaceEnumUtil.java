package org.nyu.edu.dlts.utils;

import org.json.JSONObject;
import java.util.HashMap;

/**
 * This is util class used to mapped ASpace enum list to AR enum list items
 *
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: 12/7/12
 * Time: 9:52 AM
 */
public class ASpaceEnumUtil {
    private HashMap<String, JSONObject> dynamicEnums;

    private HashMap<String, String> enumListIDsToValues = new HashMap<String, String>();

    private String[] ASpaceTermTypes = null;
    private String[] ASpaceNameRules = null;
    private String[] ASpaceNameDescriptionTypes = null;
    private String[] ASpaceLinkedAgentRoles = null;
    private String[] ASpaceExtentTypes = null;
    private String[] ASpaceDateEnums = null;
    private String[] ASpaceDigitalObjectTypes = null;
    private String[] ASpaceNoteTypes = null;
    private String[] ASpaceResourceLevels = null;
    private String[] ASpaceFindingAidDescriptionRules = null;
    private String[] ASpaceFindingAidStatus = null;
    private String[] ASpaceInstanceTypes = null;
    private String[] ASpaceInstanceContainerTypes = null;
    private String[] ASpaceAcquisitionTypes = null;
    private String[] ASpaceFileVersionUseStatements = null;
    private String[] ASpaceAccessionResourceTypes = null;

    // A trying that is used to bypass
    public final static String UNMAPPED = "other_unmapped";

    public boolean returnARValue = true; // set this to return the AT value instead of UNMAPPED

    /**
     * Main constructor
     */
    public ASpaceEnumUtil() {
        initASpaceTermTypes();
        initASpaceNameRules();
        initASpaceNameDescriptionType();
        initASpaceExtentTypes();
        initASpaceFindingAidStatus();
        initASpaceInstanceTypes();
        initASpaceAcquisitionTypes();
    }

    /**
     * Initialize the array that hold term types
     */
    private void initASpaceTermTypes() {
        ASpaceTermTypes = new String[]{
                "cultural_context",     // 0
                "function",             // 1
                "geographic",           // 2
                "genre_form",           // 3
                "occupation",           // 4
                "style_period",         // 5
                "technique",            // 6
                "temporal",             // 7
                "topical",              // 8
                "uniform_title"         // 9
        };
    }

    /**
     * Method to map the AR subject term type to the ones in ASPace
     * @param arID
     * @return
     */
    public String getASpaceTermType(int arID) {
        if(arID == 4) {
            return ASpaceTermTypes[1];
        } else if(arID == 5) {
            return ASpaceTermTypes[3];
        } else if(arID == 6) {
            return ASpaceTermTypes[2];
        } else if(arID == 7) {
            return ASpaceTermTypes[4];
        } else if(arID == 2) {
            return ASpaceTermTypes[7];
        } else if(arID == 1) {
            return ASpaceTermTypes[8];
        } else if(arID == 9) {
            return ASpaceTermTypes[9];
        } else { // return topical as the default
            return ASpaceTermTypes[8];
        }
    }

    /**
     * Method to map the subject source
     *
     * @param arID
     * @return
     */
    public String getASpaceSubjectSource(int arID) {
        String key = "subject_source_" + arID;
        return getEnumValueForID(key, "local");
    }

    /**
     * Map the name source
     *
     * @param arID
     * @return
     */
    public String getASpaceNameSource(int arID) {
        String key = "name_source_" + arID;
        return getEnumValueForID(key, "local");
    }

    /**
     * Method to initASpaceialize the name rules array
     */
    private void initASpaceNameRules() {
        ASpaceNameRules = new String[] {
                "local",    // 0
                "aacr",     // 1
                "dacs"      // 2
        };
    }

    /**
     * Map the name source
     *
     * @param arID
     * @return
     */
    public String getASpaceExtentType(int arID) {
        String key = "extent_type_" + arID;
        return getEnumValueForID(key, "linear_feet");
    }

    /**
     * Map the aspace resource type
     *
     * @param arID
     * @return
     */
    public String getASpaceResourceType(boolean arID) {
        String key = "resource_type_" + arID;
        return getEnumValueForID(key, "records");
    }

    /**
     * Get the Name description type
     */
    private void initASpaceNameDescriptionType() {
        ASpaceNameDescriptionTypes = new String[]{
                "administrative history",   // 0
                "biographical statement",   // 1
        };
    }

    /**
     * Method to init ASpace extent type array
     */
    private void initASpaceExtentTypes() {
        ASpaceExtentTypes = new String[] {
                "cassettes",            // 0
                "cubic_feet",           // 1
                "files",                // 2
                "gigabytes",            // 3
                "leaves",               // 4
                "linear_feet",          // 5
                "megabytes",            // 6
                "photographic_prints",  // 7
                "photographic_slides",  // 8
                "reels",                // 9
                "sheets",               // 10
                "terabytes",            // 11
                "volumes"               // 12
        };
    }

    /**
     * Map an AR value to a collection management record enum
     *
     * @param arID
     * @return
     */
    public String getASpaceCollectionManagementRecordProcessingPriority(int arID) {
        String key = "processing_priority_" + arID;
        return getEnumValueForID(key, "low");
    }

    /**
     * Method to return the map find description
     *
     * @param value
     * @return
     */
    public String getASpaceFindingAidDescriptionRule(Integer value) {
        if (value == 1) {
            return ASpaceFindingAidDescriptionRules[2];
        } else if(value == 2) {
            return ASpaceFindingAidDescriptionRules[0];
        } else if(value == 3) {
            return ASpaceFindingAidDescriptionRules[3];
        } else if(value == 4) {
            return ASpaceFindingAidDescriptionRules[4];
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to initASpace the finding aid status
     */
    private void initASpaceFindingAidStatus() {
        ASpaceFindingAidStatus = new String[] {
                "completed",        // 0
                "in_progress",      // 1
                "under_revision",   // 2
                "unprocessed"       // 3
        };
    }

    /**
     * Method to return the finding aid status
     *
     * @param atValue
     * @return
     */
    public String getASpaceFindingAidStatus(String atValue) {
        atValue = atValue.toLowerCase();

        if (atValue.contains("completed")) {
            return ASpaceFindingAidStatus[0];
        } else if(atValue.contains("in_process")) {
            return ASpaceFindingAidStatus[1];
        } else if(atValue.contains("under_revision")) {
            return ASpaceFindingAidStatus[2];
        } else if(atValue.contains("unprocessed")) {
            return ASpaceFindingAidStatus[3];
        } else if (returnARValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to initialize the ASpace instance types
     */
    private void initASpaceInstanceTypes() {
        ASpaceInstanceTypes = new String[] {
                "audio",                // 0
                "books",                // 1
                "computer_disks",       // 2
                "graphic_materials",    // 3
                "maps",                 // 4
                "microform",            // 5
                "mixed_materials",      // 6
                "moving_images",        // 7
                "realia",               // 8
                "text",                 // 9
                "digital_object"        // 10
        };
    }

    /**
     * Method to return the equivalent ASpace instance type
     *
     * @param atValue
     * @return
     */
    public String getASpaceInstanceType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if(atValue.contains("audio")) {
            return ASpaceInstanceTypes[0];
        } else if(atValue.contains("books")) {
            return ASpaceInstanceTypes[1];
        } else if(atValue.contains("computer disks")) {
            return ASpaceInstanceTypes[2];
        } else if(atValue.contains("digital object")) {
            return ASpaceInstanceTypes[10];
        } else if(atValue.contains("graphic materials")) {
            return ASpaceInstanceTypes[3];
        } else if(atValue.contains("maps")) {
            return ASpaceInstanceTypes[4];
        } else if(atValue.contains("microform")) {
            return ASpaceInstanceTypes[5];
        } else if(atValue.contains("mixed materials")) {
            return ASpaceInstanceTypes[6];
        } else if(atValue.contains("moving Images")) {
            return ASpaceInstanceTypes[7];
        } else if(atValue.contains("realia")) {
            return ASpaceInstanceTypes[8];
        } else if(atValue.contains("text")) {
            return ASpaceInstanceTypes[9];
        } else if(returnARValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to init the aspace term types
     */
    private void initASpaceAcquisitionTypes() {
        ASpaceAcquisitionTypes = new String[] {
                "deposit",  // 0
                "gift",     // 1
                "purchase", // 2
                "transfer"  // 3
        };
    }

    /**
     * Method to set the hash map that holds the dynamic enums
     *
     * @param dynamicEnums
     */
    public void setASpaceDynamicEnums(HashMap<String, JSONObject> dynamicEnums) {
        this.dynamicEnums = dynamicEnums;

        // now extract the language codes, and salutations so we can compare with the AT values
        JSONObject languageCodeEnumJS = dynamicEnums.get("language_iso639_2");
        JSONObject salutationEnumJS = dynamicEnums.get("agent_contact_salutation");
    }

    /**
     * Method to return the correct enum list based on the AT list name. Only editable list in the
     * Archon are returned
     *
     * @param listName
     * @return
     */
    public JSONObject getDynamicEnum(String listName) throws Exception {
        JSONObject dynamicEnum = null;

        if (listName.contains("countries")) {
            dynamicEnum = dynamicEnums.get("country_iso_3166");
            dynamicEnum.put("valueKey", "ISOAlpha2");
            dynamicEnum.put("idPrefix", "country");
            dynamicEnum.put("keepValueCase", true);
        } else if (listName.contains("creatorsources")) {
            dynamicEnum = dynamicEnums.get("name_source");
            dynamicEnum.put("valueKey", "SourceAbbreviation");
            dynamicEnum.put("idPrefix", "name_source");
        } else if (listName.contains("extentunits")) {
            dynamicEnum = dynamicEnums.get("extent_extent_type");
            dynamicEnum.put("valueKey", "ExtentUnit");
            dynamicEnum.put("idPrefix", "extent_type");
        } else if (listName.contains("containertypes")) {
            dynamicEnum = dynamicEnums.get("container_type");
            dynamicEnum.put("valueKey", "ContainerType");
            dynamicEnum.put("idPrefix", "container_types");
        } else if (listName.contains("materialtypes")) {
            dynamicEnum = dynamicEnums.get("resource_resource_type");
            dynamicEnum.put("valueKey", "MaterialType");
            dynamicEnum.put("idPrefix", "resource_type");
        } else if (listName.contains("filetypes")) {
            dynamicEnum = dynamicEnums.get("file_version_file_format_name");
            dynamicEnum.put("valueKey", "FileType");
            dynamicEnum.put("idPrefix", "file_type");
        } else if (listName.contains("processingpriorities")) {
            dynamicEnum = dynamicEnums.get("collection_management_processing_priority");
            dynamicEnum.put("valueKey", "ProcessingPriority");
            dynamicEnum.put("idPrefix", "processing_priority");
        } else if (listName.contains("subjectsources")) {
            dynamicEnum = dynamicEnums.get("subject_source");
            dynamicEnum.put("valueKey", "EADSource");
            dynamicEnum.put("idPrefix", "subject_source");
        }

        return dynamicEnum;
    }

    /**
     * Method to add a code and a value the code to value data map
     *
     * @param id This is the unique id of the enum value
     * @param value
     */
    public void addIdAndValueToEnumList(String id, String value) {
        enumListIDsToValues.put(id, value);
    }

    /**
     * Method to return an archon enum value given the ID
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public String getEnumValueForID(String key, String defaultValue) {
        if(enumListIDsToValues.containsKey(key)) {
            return enumListIDsToValues.get(key);
        } else {
            return defaultValue;
        }
    }
}
