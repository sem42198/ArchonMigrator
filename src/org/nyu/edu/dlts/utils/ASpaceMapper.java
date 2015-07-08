package org.nyu.edu.dlts.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: 9/5/12
 * Time: 1:41 PM
 *
 * Class to map AT data model to ASPace JSON data model
 */
public class ASpaceMapper {
    // String used when mapping AT access class to groups
    public static final String ACCESS_CLASS_PREFIX = "_AccessClass_";

    // The utility class used to map to ASpace Enums
    private ASpaceEnumUtil enumUtil = new ASpaceEnumUtil();

    // used to map AT vocabularies to ASpace vocabularies
    public String vocabularyURI = "/vocabularies/1";

    // the script mapper script
    private String mapperScript = null;

    // these store the ids of all accessions, resources, and digital objects loaded so we can
    // check for uniqueness before copying them to the ASpace backend
    private ArrayList<String> digitalObjectIDs = new ArrayList<String>();
    private ArrayList<String> accessionIDs = new ArrayList<String>();
    private ArrayList<String> resourceIDs = new ArrayList<String>();
    private ArrayList<String> eadIDs = new ArrayList<String>();

    // some code used for testing
    private boolean makeUnique = true;

    // initialize the random string generators for use when unique ids are needed
    private RandomString randomString = new RandomString(3);
    private RandomString randomStringLong = new RandomString(6);

    // used to store errors
    private ASpaceCopyUtil aspaceCopyUtil;

    // used when generating errors
    private String currentResourceRecordIdentifier;

    /**
     *  Main constructor
     */
    public ASpaceMapper() { }

    /**
     * Constructor that takes an aspace copy util object
     * @param aspaceCopyUtil
     */
    public ASpaceMapper(ASpaceCopyUtil aspaceCopyUtil) {
        this.aspaceCopyUtil = aspaceCopyUtil;
    }

    /**
     * Method to set the hash map that holds the dynamic enums
     *
     * @param dynamicEnums
     */
    public void setASpaceDynamicEnums(HashMap<String, JSONObject> dynamicEnums) {
        enumUtil.setASpaceDynamicEnums(dynamicEnums);
    }

    /**
     * This method is used to map AT lookup list values into a dynamic enum.
     *
     * @param enumList
     * @return
     */
    public JSONObject mapEnumList(JSONObject enumList, String endpoint) throws Exception {
        // first we get the correct dynamic enum based on list. If it null then we just return null
        JSONObject dynamicEnumJS = enumUtil.getDynamicEnum(endpoint);

        if(dynamicEnumJS == null) return null;

        // add the values return from aspace enum an arraylist to make lookup easier
        JSONArray valuesJA = dynamicEnumJS.getJSONArray("values");
        ArrayList<String> valuesList = new ArrayList<String>();
        for(int i = 0; i < valuesJA.length(); i++) {
            valuesList.add(valuesJA.getString(i));
        }

        // now see if all the archon values are in the ASpace list already
        // if they are not then add them
        String valueKey = dynamicEnumJS.getString("valueKey");
        String idPrefix = dynamicEnumJS.getString("idPrefix");

        boolean toLowerCase = true;
        if(dynamicEnumJS.has("keepValueCase")) {
            toLowerCase = false;
        }

        int count = 0;
        Iterator<String> keys = enumList.keys();
        while(keys.hasNext()) {
            JSONObject enumJS = enumList.getJSONObject(keys.next());
            String value = enumJS.getString(valueKey);

            // most values in ASpace are lower case so normalize
            if(toLowerCase) {
                value = value.toLowerCase();
            }

            // map the id to value
            String id = idPrefix + "_" + enumJS.get("ID");
            enumUtil.addIdAndValueToEnumList(id, value);

            // see if to add this to aspace
            if(!valuesList.contains(value)) {
                valuesJA.put(value);
                count++;
                System.out.println("Adding value " + value);
            }
        }

        if(count != 0) {
            return dynamicEnumJS;
        } else {
            return null;
        }
    }

    /**
     * Method to get the corporate agent object from a repository
     *
     * @param repository
     * @return
     */
    public String getCorporateAgent(JSONObject repository) throws JSONException {
        // Main json object, agent_person.rb schema
        JSONObject agentJS = new JSONObject();
        agentJS.put("agent_type", "agent_corporate_entity");

        // hold name information
        JSONArray namesJA = new JSONArray();
        JSONObject namesJS = new JSONObject();

        //add the contact information
        JSONArray contactsJA = new JSONArray();
        JSONObject contactsJS = new JSONObject();

        contactsJS.put("name", repository.get("Name"));
        contactsJS.put("address_1", repository.get("Address"));
        contactsJS.put("address_2", repository.get("Address2"));
        contactsJS.put("city", repository.get("City"));

        // add the country and country code together
        String country = "Country Code "  + repository.get("CountryID");
        contactsJS.put("country", country);

        String postCode = repository.get("ZIPCode") + "-" + repository.get("ZIPPlusFour");
        contactsJS.put("post_code", postCode);

        String phone = repository.get("Phone") + " ext " + repository.get("PhoneExtension");
        contactsJS.put("telephone", phone);
        contactsJS.put("fax", repository.get("Fax"));
        contactsJS.put("email", repository.get("Email"));

        contactsJA.put(contactsJS);
        agentJS.put("agent_contacts", contactsJA);

        // add the names object
        String primaryName = repository.getString("Name");
        namesJS.put("source", "local");
        namesJS.put("primary_name", primaryName);
        namesJS.put("sort_name", primaryName);

        namesJA.put(namesJS);
        agentJS.put("names", namesJA);

        return agentJS.toString();
    }

    /**
     * Method to convert an AT subject record to
     *
     * @param record
     * @return
     * @throws Exception
     */
    public String convertRepository(JSONObject record, String agentURI) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the Archon database Id as an external ID
        addExternalId(record, json, "repository");

        // get the repo code
        json.put("repo_code", record.get("Name"));
        json.put("name", fixEmptyString(record.getString("Name")));
        json.put("org_code", record.get("Code"));
        json.put("url", fixUrl(record.getString("URL")));

        if(agentURI != null) {
            json.put("agent_representation", getReferenceObject(agentURI));
        }

        return json.toString();
    }

    /**
     * Method to convert an AT subject record to
     *
     * @param record
     * @return
     * @throws Exception
     */
    public String convertUser(JSONObject record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "user");

        // get the username replacing spaces with underscores
        String username = record.getString("Login");
        json.put("username", username);

        // get the full name
        String name = fixEmptyString(record.getString("DisplayName"), "full name not entered");
        json.put("name", name);
        json.put("email", record.get("Email"));

        return json.toString();
    }

    /**
     * Method to convert a subject record
     *
     * @param record
     * @return
     * @throws Exception
     */
    public String convertSubject(JSONObject record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "subject");

        json.put("vocabulary", vocabularyURI);

        json.put("source", enumUtil.getASpaceSubjectSource(record.getInt("SubjectSourceID")));

        // add the terms
        JSONArray termsJA = new JSONArray();
        addSubjectTerms(record, termsJA);
        json.put("terms", termsJA);

        return json.toString();
    }

    /**
     * Method to add terms to a subject
     *
     * @param record
     * @param termsJA
     * @throws Exception
     */
    private void addSubjectTerms(JSONObject record, JSONArray termsJA) throws Exception {
        if(record.get("Parent") != null && record.getInt("ParentID") != 0) {
            addSubjectTerms(record.getJSONObject("Parent"), termsJA);
        }

        JSONObject termJS = new JSONObject();
        termJS.put("term", record.get("Subject"));
        termJS.put("term_type", enumUtil.getASpaceTermType(record.getInt("SubjectTypeID")));
        termJS.put("vocabulary", vocabularyURI);
        termsJA.put(termJS);
    }

    /**
     * Method to convert a name aka creator record to an ASpace agent record
     *
     * @param record
     * @param creatorTypeId
     * @return
     * @throws Exception
     */
    public JSONObject convertCreator(JSONObject record, int creatorTypeId) throws Exception {
         // Main json object
        JSONObject agentJS = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, agentJS, "creator");

        agentJS.put("vocabulary", vocabularyURI);

        // hold name information
        JSONArray namesJA = new JSONArray();
        JSONObject namesJS = new JSONObject();

        // add the biog-history note to the agent object
        if(record.has("BiogHist") && !record.getString("BiogHist").isEmpty()) {
            addBiologicalHistoryNote(agentJS, record, creatorTypeId);
        }

        // add the agent date
        if(record.has("Dates") && !record.getString("Dates").isEmpty()) {
            JSONArray datesJA = new JSONArray();
            JSONObject dateJS = new JSONObject();
            dateJS.put("date_type", "single");
            dateJS.put("label", "existence");
            dateJS.put("expression", record.get("Dates"));
            datesJA.put(dateJS);
            agentJS.put("dates_of_existence", datesJA);
        }

        // add the source for the name
        namesJS.put("source", enumUtil.getASpaceNameSource(record.getInt("CreatorSourceID")));

        // add basic information to the names record
        String sortName = record.getString("Name");
        namesJS.put("sort_name", sortName);
        namesJS.put("name_order", "direct");

        switch (creatorTypeId) {
            case 19:
            case 21:
            case 23:
                agentJS.put("agent_type", "agent_person");
                namesJS.put("primary_name", sortName);

                if(record.has("NameFullerForm")) {
                    namesJS.put("fuller_form", record.get("NameFullerForm"));
                }
                break;
            case 20:
                agentJS.put("agent_type", "agent_family");
                namesJS.put("family_name", sortName);
                break;
            case 22:
                agentJS.put("agent_type", "agent_corporate_entity");
                namesJS.put("primary_name", sortName);
                break;
            default:
                String message = sortName + ":: Unknown name type: " + creatorTypeId + "\n";
                aspaceCopyUtil.addErrorMessage(message);
                return null;
        }

        // add the names array and names json objects to main record
        namesJA.put(namesJS);

        agentJS.put("names", namesJA);

        return agentJS;
    }

    /**
     * Method to convert the classification record
     *
     * @param record
     * @return
     * @throws Exception
     */
    public JSONObject convertClassification(JSONObject record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // set the model type
        if(record.getString("ParentID").equals("0")) {
            json.put("jsonmodel_type", "classification");
        } else {
            json.put("jsonmodel_type", "classification_term");
        }

        json.put("identifier", record.get("ClassificationIdentifier"));
        json.put("title", record.get("Title"));
        json.put("description", record.get("Description"));

        return json;
    }

    /**
     * Method to add a bioghist note agent object
     *
     *
     * @param agentJS
     * @param record
     * @param creatorTypeId
     * @throws Exception
     */
    public void addBiologicalHistoryNote(JSONObject agentJS, JSONObject record, int creatorTypeId) throws Exception {
        JSONArray notesJA = new JSONArray();
        JSONObject noteJS = new JSONObject();

        noteJS.put("jsonmodel_type", "note_bioghist");
        noteJS.put("label", "biographical statement");

        JSONArray subnotesJA = new JSONArray();

        JSONObject textNoteJS = new JSONObject();
        addTextNote(textNoteJS, record.getString("BiogHist"));
        subnotesJA.put(textNoteJS);

        // add a subnote which holds the citation information
        if(!record.getString("BiogHistAuthor").isEmpty()) {
            JSONObject citationJS = new JSONObject();
            citationJS.put("jsonmodel_type", "note_citation");
            JSONArray contentJA = new JSONArray();
            contentJA.put("Author: " + record.get("BiogHistAuthor"));
            citationJS.put("content", contentJA);
            subnotesJA.put(citationJS);
        }

        // add the subnote which hold the source information
        if(!record.getString("Sources").isEmpty()) {
            String noteType = "note_citation";
            if(creatorTypeId == 22) {
                noteType = "note_abstract";
            }

            JSONObject subnoteJS = new JSONObject();
            subnoteJS.put("jsonmodel_type", noteType);
            JSONArray contentJA = new JSONArray();
            contentJA.put(record.get("Sources"));
            subnoteJS.put("content", contentJA);
            subnotesJA.put(subnoteJS);
        }

        noteJS.put("subnotes", subnotesJA);
        notesJA.put(noteJS);
        agentJS.put("notes", notesJA);
    }

    /**
     * Method to convert a digital object record
     *
     * @param record
     * @return
     */
    public JSONObject convertDigitalObject(JSONObject record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "digital_object");

        /* add the fields required for abstract_archival_object.rb */

        String title = record.getString("Title");
        json.put("title", title);

        boolean dateAdded = addDate(record, json, "digitized");

        // need to add title if no date or title
        if(title.isEmpty() && !dateAdded) {
            json.put("title", "Digital Object " + record.get("ID"));
        }

        /* add the fields required digital_object.rb */

        JSONArray fileVersionsJA = new JSONArray();
        addFileVersion(fileVersionsJA, record, "Digital Object");
        json.put("file_versions", fileVersionsJA);

        json.put("digital_object_id", getUniqueID(ASpaceClient.DIGITAL_OBJECT_ENDPOINT, record.getString("Identifier"), null));

        // set the digital object type
        json.put("digital_object_type", "mixed_materials");

        // set the restrictions apply
        json.put("publish", convertToBoolean(record.getInt("Browsable")));

        // add the notes
        addDigitalObjectNotes(record, json);

        return json;
    }

    /**
     * Method to convert a digital object record into a aspace digital object component
     *
     * @param record
     * @return
     */
    public JSONObject convertToDigitalObjectComponent(JSONObject record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        /* add the fields required for abstract_archival_object.rb */
        String title = record.getString("Title");
        json.put("title", fixEmptyString(title));

        /* add fields required for digital object component*/
        JSONArray fileVersionsJA = new JSONArray();
        addFileVersion(fileVersionsJA, record, "Digital Object Component");
        json.put("file_versions", fileVersionsJA);

        String label = title;
        json.put("label", label);

        json.put("position", record.getInt("DisplayOrder"));

        json.put("component_id", fixEmptyString(record.getString("ID"), "ID_" + randomString.nextString()));

        return json;
    }

    /**
     * Method to convert external documents to the aspace external document object
     *
     * @param fileVersionsJA
     */
    public void addFileVersion(JSONArray fileVersionsJA, JSONObject record, String type) throws JSONException {
        if(record.has("ContentURL") && !record.getString("ContentURL").isEmpty()) {
            JSONObject fileVersionJS = new JSONObject();

            fileVersionJS.put("file_uri", record.getString("ContentURL"));
            fileVersionJS.put("use_statement", "image-master");
            fileVersionJS.put("xlink_actuate_attribute", "none");
            fileVersionJS.put("xlink_show_attribute", "none");

            fileVersionsJA.put(fileVersionJS);
        } else if(record.has("Filename") && !record.getString("Filename").isEmpty()) {
            JSONObject fileVersionJS = new JSONObject();

            fileVersionJS.put("file_uri", record.getString("Filename"));
            fileVersionJS.put("use_statement", "image-master");
            fileVersionJS.put("xlink_actuate_attribute", "none");
            fileVersionJS.put("xlink_show_attribute", "none");
            fileVersionJS.put("file_format_name", record.get("FileTypeID"));
            fileVersionJS.put("file_size_bytes", record.get("Bytes"));
        } else {
            //System.out.println("No file version found for " + type + ": " + record.get("Title"));
        }
    }

    /**
     * Method to convert an resource record to json ASpace JSON
     *
     * @param record
     * @return
     * @throws Exception
     */
    public JSONObject convertResource(JSONObject record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "resource");

        /* Add fields needed for abstract_archival_object.rb */

        // check to make sure we have a title
        String title = fixEmptyString(record.getString("Title"));
        json.put("title", title);

        // add the language code
        json.put("language", getLanguageCode(null, "eng"));

        // add the extent array containing one object or many depending if we using multiple extents
        addResourceExtent(record, json);

        // add the date array containing the dates json objects
        addResourceDates(record, json, "Resource: " + currentResourceRecordIdentifier);

        // add external documents
        String otherURL = record.getString("OtherURL");
        if(!otherURL.isEmpty()) {
            addExternalDocument(json, "Other URL", otherURL);
        }

        /* Add fields needed for resource.rb */

        // get the ids and make them unique if we in DEBUG mode
        String id_0 = record.getString("CollectionIdentifier");
        if(id_0.isEmpty()) {
            id_0 = record.getString("ClassificationID");
        }

        String id_1 = "";
        String id_2 = "";
        String id_3 = "";

        if(makeUnique) {
            id_1 = randomString.nextString();
            id_2 = randomString.nextString();
            id_3 = randomString.nextString();
        }

        json.put("id_0", id_0);
        json.put("id_1", id_1);
        json.put("id_2", id_2);
        json.put("id_3", id_3);

        // get the level
        json.put("level", "collection");

        json.put("resource_type", "papers"); // should be mapped correctly

        // set the publish, restrictions, processing note, container summary
        json.put("publish", convertToBoolean(record.getInt("Enabled")));

        json.put("container_summary", "Archon Container Summary");

        // add fields for EAD
        json.put("ead_id", getUniqueID("ead", "Archon EAD", null));
        json.put("ead_location", "Archon Finding Aid location");
        json.put("finding_aid_title", "Archon Finding Aid Title");
        json.put("finding_aid_date", record.get("PublicationDate"));
        json.put("finding_aid_author", record.get("FindingAidAuthor"));

        Integer descriptiveRulesID = record.getInt("DescriptiveRulesID");
        if(descriptiveRulesID != null) {
            json.put("finding_aid_description_rules", enumUtil.getASpaceFindingAidDescriptionRule(descriptiveRulesID));
        }

        json.put("finding_aid_language", record.get("FindingLanguageID"));
        //json.put("finding_aid_sponsor", record.getSponsorNote());
        //json.put("finding_aid_edition_statement", record.getEditionStatement());
        //json.put("finding_aid_series_statement", record.getSeries());
        //json.put("finding_aid_revision_date", record.getRevisionDate());
        //json.put("finding_aid_revision_description", record.getRevisionDescription());
        json.put("finding_aid_note", record.get("PublicationNote"));

        // add the notes
        addResourceNotes(record, json);

        return json;
    }

    /**
     * Method to add extent information
     *
     * @param record
     * @param json
     * @throws Exception
     */
    private void addResourceExtent(JSONObject record, JSONObject json) throws Exception {
        JSONArray extentJA = new JSONArray();
        JSONObject extentJS = new JSONObject();

        extentJS.put("portion", "whole");
        extentJS.put("extent_type", "linear_feet");

        if (!record.getString("Extent").isEmpty()) {
            extentJS.put("number", record.getString("Extent"));
        } else {
            extentJS.put("number", "0");
        }

        extentJA.put(extentJS);

        // add the alternative extent statement
        String altExtent = record.getString("AltExtentStatement");
        if(!altExtent.isEmpty()) {
            extentJS = new JSONObject();

            extentJS.put("portion", "whole");
            extentJS.put("extent_type", "cubic_feet");
            extentJS.put("number", altExtent);
        }

        json.put("extents", extentJA);
    }

    /**
     * Method to add a date json object
     *
     * @param json
     * @param record
     */
    private void addResourceDates(JSONObject record, JSONObject json, String recordIdentifier) throws Exception {
        JSONArray dateJA = new JSONArray();

        JSONObject dateJS = new JSONObject();

        dateJS.put("date_type", "single");
        dateJS.put("label", "created");

        String dateExpression = record.getString("InclusiveDates");
        dateJS.put("expression", dateExpression);

        Integer dateBegin = convertToInteger(record.getString("NormalDateBegin"));
        Integer dateEnd = convertToInteger(record.getString("NormalDateEnd"));

        if (dateBegin != null) {
            dateJS.put("date_type", "inclusive");

            dateJS.put("begin", dateBegin.toString());

            if (dateEnd != null) {
                if(dateEnd >= dateBegin) {
                    dateJS.put("end", dateEnd.toString());
                } else {
                    dateJS.put("end", dateBegin.toString());

                    String message = "End date: " + dateEnd + " before begin date: " + dateBegin + ", ignoring end date\n" + recordIdentifier;
                    aspaceCopyUtil.addErrorMessage(message);
                }
            } else {
                dateJS.put("end", dateBegin.toString());
            }
        }

        // see if to add this date now
        if((dateExpression != null && !dateExpression.isEmpty()) || dateBegin != null) {
            dateJA.put(dateJS);
        }

        // add the bulk dates
        String bulkDates = record.getString("PredominantDates");
        if(!bulkDates.isEmpty()) {
            dateJS = new JSONObject();

            dateJS.put("date_type", "bulk");

            dateJS.put("label", "other");

            dateExpression = bulkDates;
            dateJS.put("expression", dateExpression);

            dateJA.put(dateJS);
        }

        // add the acquisition date
        String acquisitionDate = record.getString("AcquisitionDate");
        if(!acquisitionDate.isEmpty()) {
            dateJS = new JSONObject();

            dateJS.put("date_type", "single");

            dateJS.put("label", "other");

            dateExpression = "Date acquired: " + acquisitionDate;
            dateJS.put("expression", dateExpression);

            dateJA.put(dateJS);
        }

        json.put("dates", dateJA);
    }

    /**
     * Method to convert an resource component record to json ASpace JSON
     *
     * @param record
     * @return
     * @throws Exception
     */
    public JSONObject convertResourceComponent(JSONObject record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        addExternalId(record, json, "resource_component");

        /* Add fields needed for abstract_archival_object.rb */

        // check to make sure we have a title
        String title = record.getString("Title");
        json.put("title", title);

        boolean dateAdded = addDate(record, json, "created");

        // need to add title if no date or title
        if(title.isEmpty() && !dateAdded) {
            json.put("title", "Resource Component " + record.get("ID"));
        }

        /* add field required for archival_object.rb */

        // make the ref id unique otherwise ASpace complains
        String refId = record.getString("ID") + "_SortOrder-" + record.getInt("SortOrder");
        json.put("ref_id", refId);

        //String level = "series";
        String level = record.getString("EADLevel");
        json.put("level", level);

        if(level.equals("otherlevel")) {
            json.put("other_level", fixEmptyString(record.getString("OtherLevel")));
        }

        if(!record.getString("UniqueID").isEmpty()) {
            json.put("component_id", record.getString("UniqueID"));
        }

        //json.put("position", record.getInt("SortOrder"));

        // place a dummy key to help with the sorting if we redoing the sorting
        String paddedSortOrder = String.format("%05d", record.getInt("SortOrder"));
        json.put("sort_key1", paddedSortOrder + "_" + record.get("ID"));
        json.put("sort_key2", ""); // sort order of the parent physical only content record

        // add the notes
        addResourceComponentNotes(record, json);

        return json;
    }

    /**
     * Method to add a date json object
     *
     * @param record
     * @param json
     * @param label
     */
    private boolean addDate(JSONObject record, JSONObject json, String label) throws Exception {
        String dateExpression = record.getString("Date");
        if(dateExpression.isEmpty()) return false;

        JSONArray dateJA = new JSONArray();
        JSONObject dateJS = new JSONObject();

        dateJS.put("date_type", "single");
        dateJS.put("label", label);
        dateJS.put("expression", dateExpression);

        dateJA.put(dateJS);
        json.put("dates", dateJA);

        return true;
    }

    /**
     * Method to return the language code
     *
     * @param languageCodes
     * @return
     */
    private String getLanguageCode(JSONArray languageCodes, String defaultLanguageCode) throws Exception {
        if(languageCodes != null && languageCodes.length() != 0) {
            return languageCodes.getString(0);
        } else {
            return defaultLanguageCode;
        }
    }

    /**
     * Add an external document to the JSON object
     * @param json
     * @param title
     * @param location
     */
    private void addExternalDocument(JSONObject json, String title, String location) throws Exception {
        JSONArray externalDocumentsJA = new JSONArray();

        JSONObject documentJS = new JSONObject();
        documentJS.put("title", title);
        documentJS.put("location", fixUrl(location));
        externalDocumentsJA.put(documentJS);

        json.put("external_documents", externalDocumentsJA);
    }

    /**
     * Method to add notes to the digital object
     *
     * @param json
     * @param record
     */
    private void addDigitalObjectNotes(JSONObject record, JSONObject json) throws Exception {
        JSONArray notesJA = new JSONArray();

        String noteContent = "";

        addDigitalObjectNote(notesJA, "summary", "Summary Note", record.getString("Scope"));

        addDigitalObjectNote(notesJA, "physdesc", "Physical Description Note", record.getString("PhysicalDescription"));

        if(!record.getString("Publisher").isEmpty()) {
            noteContent = "Publisher: " + record.getString("Publisher");
            addDigitalObjectNote(notesJA, "note", "Publisher Note", noteContent);
        }

        if(!record.getString("Contributor").isEmpty()) {
            noteContent = "Contributor: " + record.getString("Contributor");
            addDigitalObjectNote(notesJA, "note", "Contributor Note", noteContent);
        }

        addDigitalObjectNote(notesJA, "userestrict", "Rights Statement Note", record.getString("RightsStatement"));

        json.put("notes", notesJA);
    }

    /**
     * Method to add notes to this finding aid
     * @param json
     * @param record
     */
    private void addResourceNotes(JSONObject record, JSONObject json) throws Exception {
        JSONArray notesJA = new JSONArray();

        String noteContent = "";

        addMultipartNote(notesJA, "scopecontent", "Scope and Contents", record.getString("Scope"));

        addSinglePartNote(notesJA, "abstract", "Abstract", record.getString("Abstract"));

        addMultipartNote(notesJA, "arrangement", "Arrangement Note", record.getString("Arrangement"));

        addMultipartNote(notesJA, "accessrestrict", "Conditions Governing Access", record.getString("AccessRestrictions"));

        addMultipartNote(notesJA, "userestrict", "Conditions Governing Use", record.getString("UseRestrictions"));

        addMultipartNote(notesJA, "phystech", "Physical Access Requirements", record.getString("PhysicalAccess"));

        addMultipartNote(notesJA, "phystech", "Technical Access Requirements", record.getString("TechnicalAccess"));

        addMultipartNote(notesJA, "acqinfo", "Source of Acquisition", record.getString("AcquisitionSource"));

        addMultipartNote(notesJA, "acqinfo", "Method of Acquisition", record.getString("AcquisitionMethod"));

        addMultipartNote(notesJA, "appraisal", "Appraisal Information", record.getString("AppraisalInfo"));

        addMultipartNote(notesJA, "accruals", "Accruals and Additions", record.getString("AccrualInfo"));

        addMultipartNote(notesJA, "custodhist", "Custodial History", record.getString("CustodialHistory"));

        noteContent = record.getString("OrigCopiesNote") + "\n\n" + record.get("OrigCopiesURL");
        addMultipartNote(notesJA, "originalsloc", "Existence and Location of Originals", noteContent);

        noteContent = record.getString("RelatedMaterials") + "\n\n" + record.get("RelatedMaterialsURL");
        addMultipartNote(notesJA, "relatedmaterial", "Related Materials", noteContent);

        addMultipartNote(notesJA, "relatedmaterial", "Related Publications", record.getString("RelatedPublications"));

        addMultipartNote(notesJA, "separatedmaterial", "Separated Materials", record.getString("SeparatedMaterials"));

        addMultipartNote(notesJA, "prefercite", "Preferred Citation", record.getString("PreferredCitation"));

        addMultipartNote(notesJA, "odd", "Other Descriptive Information", record.getString("OtherNote"));

        addMultipartNote(notesJA, "processinfo", "Processing Information", record.getString("ProcessingInfo"));

        noteContent = record.getString("BiogHist") + "\n\nNote written by " + record.get("BiogHistAuthor");
        addMultipartNote(notesJA, "bioghist", "Biographical or Historical Information", noteContent);

        json.put("notes", notesJA);
    }

    /**
     * Method to add notes for this resource component
     *
     * @param json
     * @param record
     */
    private void addResourceComponentNotes(JSONObject record, JSONObject json) throws Exception {
        JSONArray notesJA = new JSONArray();

        addSinglePartNote(notesJA, "materialspec", "Private Title", record.getString("PrivateTitle"));

        addMultipartNote(notesJA, "scopecontent", "Scope and Contents", record.getString("Description"));

        Object object = record.get("Notes");
        if(object instanceof JSONObject) {
            JSONObject recordNotes = (JSONObject)object;

            Iterator<String> keys = recordNotes.keys();
            while(keys.hasNext()) {
                JSONObject note = recordNotes.getJSONObject(keys.next());
                if(note.has("Title")) {
                    addMultipartNote(notesJA, "odd", note.getString("Title"), note.getString("Content"));
                }
            }
        }

        json.put("notes", notesJA);
    }

    /**
     * Method to add single part note
     * @param notesJA
     * @param noteType
     * @param noteLabel
     * @param noteContent
     * @throws Exception
     */
    private void addSinglePartNote(JSONArray notesJA, String noteType, String noteLabel, String noteContent) throws Exception {
        if(noteContent.isEmpty()) return;

        JSONObject noteJS = new JSONObject();

        noteJS.put("jsonmodel_type", "note_singlepart");
        noteJS.put("type", noteType);
        noteJS.put("label", noteLabel);

        JSONArray contentJA = new JSONArray();
        contentJA.put(noteContent);
        noteJS.put("content", contentJA);

        notesJA.put(noteJS);
    }

    /**
     * Add a multipart note
     *
     * @param notesJA
     * @param noteType
     * @param noteLabel
     * @param noteContent
     * @throws Exception
     */
    private void addMultipartNote(JSONArray notesJA, String noteType, String noteLabel, String noteContent) throws Exception {
        if(noteContent.isEmpty()) return;

        JSONObject noteJS = new JSONObject();

        noteJS.put("jsonmodel_type", "note_multipart");
        noteJS.put("type", noteType);
        noteJS.put("label", noteLabel);

        JSONArray subnotesJA = new JSONArray();

        // add the default text note
        JSONObject textNoteJS = new JSONObject();
        addTextNote(textNoteJS, fixEmptyString(noteContent, "multi-part note content"));
        subnotesJA.put(textNoteJS);

        noteJS.put("subnotes", subnotesJA);

        notesJA.put(noteJS);
    }

    /**
     * Method to add a text note
     *
     * @param noteJS
     * @param content
     * @throws Exception
     */
    private void addTextNote(JSONObject noteJS, String content) throws Exception {
        noteJS.put("jsonmodel_type", "note_text");
        noteJS.put("content", content);
    }

    /**
     * Method to add digital object note
     *
     * @param notesJA
     * @param noteType
     * @param noteLabel
     * @param noteContent
     * @throws Exception
     */
    private void addDigitalObjectNote(JSONArray notesJA, String noteType, String noteLabel, String noteContent) throws Exception {
        if(noteContent.isEmpty()) return;

        JSONObject noteJS = new JSONObject();

        noteJS.put("jsonmodel_type", "note_digital_object");
        noteJS.put("type", noteType);
        noteJS.put("label", noteLabel);

        JSONArray contentJA = new JSONArray();
        contentJA.put(noteContent);
        noteJS.put("content", contentJA);

        notesJA.put(noteJS);
    }

    /**
     * Method to add the AT internal database ID as an external ID for the ASpace object
     *
     * @param record
     * @param source
     */
    public void addExternalId(JSONObject record, JSONObject recordJS, String source) throws Exception {
        source = "Archon Instance::" + source.toUpperCase();

        JSONArray externalIdsJA = new JSONArray();
        JSONObject externalIdJS = new JSONObject();

        externalIdJS.put("external_id", record.get("ID"));
        externalIdJS.put("source", source);

        externalIdsJA.put(externalIdJS);

        recordJS.put("external_ids", externalIdsJA);
    }

    /**
     * Method to get a reference object which points to another URI
     *
     * @param recordURI
     * @return
     * @throws Exception
     */
    public JSONObject getReferenceObject(String recordURI) throws Exception {
        JSONObject referenceJS = new JSONObject();
        referenceJS.put("ref", recordURI);
        return referenceJS;
    }

    /**
     * Method to set a string that's empty to "unspecified"
     * @param text
     * @return
     */
    public String fixEmptyString(String text) {
        return fixEmptyString(text, null);
    }

    /**
     * Method to set a string that empty to "not set"
     * @param text
     * @return
     */
    private String fixEmptyString(String text, String useInstead) {
        if(text == null || text.trim().isEmpty()) {
            if(useInstead == null) {
                return "unspecified";
            } else {
                return useInstead;
            }
        } else {
            return text;
        }
    }

    /**
     * Method to prepend http:// to a url to prevent ASpace from complaining
     *
     * @param url
     * @return
     */
    private String fixUrl(String url) {
        if(url.isEmpty()) return "http://url.unspecified";

        String lowercaseUrl = url.toLowerCase();

        // check to see if its a proper uri format
        if(lowercaseUrl.contains("://")) {
            return url;
        } else if(lowercaseUrl.startsWith("/") || lowercaseUrl.contains(":\\")) {
            url = "file://" + url;
            return url;
        } else {
            url = "http://" + url;
            return  url;
        }
    }

    /**
     * Method to map the ASpace group to one or more AT access classes
     *
     * @param groupJS
     * @return
     */
    public void mapAccessClass(HashMap<String, JSONObject> repositoryGroupURIMap,
                               JSONObject groupJS, String repoURI) {
        try {
            String groupCode = groupJS.getString("group_code");
            String key = "";

            if (groupCode.equals("administrators")) { // map to access class 5
                key = repoURI + ACCESS_CLASS_PREFIX + "1";
                repositoryGroupURIMap.put(key, groupJS);
            } else if(groupCode.equals("repository-managers")) { // map to access class 4
                key = repoURI + ACCESS_CLASS_PREFIX + "1";
                repositoryGroupURIMap.put(key, groupJS);
            } else if(groupCode.equals("repository-archivists")) { // map to access class 3
                key = repoURI + ACCESS_CLASS_PREFIX + "1";
                repositoryGroupURIMap.put(key, groupJS);
            } else if(groupCode.equals("repository-advanced-data-entry")) { // map to access class 2
                key = repoURI + ACCESS_CLASS_PREFIX + "2";
                repositoryGroupURIMap.put(key, groupJS);
            } else if(groupCode.equals("repository-basic-data-entry")) { // map to access class 1
                key = repoURI + ACCESS_CLASS_PREFIX + "3";
                repositoryGroupURIMap.put(key, groupJS);
            } else if (groupCode.equals("repository-viewers")) { // map access class to access class 0 for now
                key = repoURI + ACCESS_CLASS_PREFIX + "4";
                repositoryGroupURIMap.put(key, groupJS);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to return a unique id, in cases where ASpace needs a unique id but AT doesn't
     *
     * @param endpoint
     * @param id
     * @return
     */
    private String getUniqueID(String endpoint, String id, String[] idParts) {
        id = id.trim();

        if(endpoint.equals(ASpaceClient.DIGITAL_OBJECT_ENDPOINT)) {
            // if id is empty add text
            if(id.isEmpty()) {
                id = "Digital Object ID ##"+ randomStringLong.nextString();
            }

            if(!digitalObjectIDs.contains(id)) {
                digitalObjectIDs.add(id);
            } else {
                id += " ##" + randomStringLong.nextString();
                digitalObjectIDs.add(id);
            }

            return id;
        } else if(endpoint.equals(ASpaceClient.ACCESSION_ENDPOINT)) {
            String message = null;

            if(!accessionIDs.contains(id)) {
                accessionIDs.add(id);
            } else {
                String fullId = "";

                do {
                    idParts[0] += " ##" + randomString.nextString();
                    fullId = concatIdParts(idParts);
                } while(accessionIDs.contains(fullId));

                accessionIDs.add(fullId);

                message = "Duplicate Accession Id: "  + id  + " Changed to: " + fullId + "\n";
                aspaceCopyUtil.addErrorMessage(message);
            }

            // we don't need to return the new id here, since the idParts array
            // is being used to to store the new id
            return "not used";
        } else if(endpoint.equals(ASpaceClient.RESOURCE_ENDPOINT)) {
            String message = null;

            if(!resourceIDs.contains(id)) {
                resourceIDs.add(id);
            } else {
                String fullId = "";

                do {
                    idParts[0] += " ##" + randomString.nextString();
                    fullId = concatIdParts(idParts);
                } while(resourceIDs.contains(fullId));

                resourceIDs.add(fullId);

                message = "Duplicate Resource Id: "  + id  + " Changed to: " + fullId + "\n";
                aspaceCopyUtil.addErrorMessage(message);
            }

            // we don't need to return the new id here, since the idParts array
            // is being used to to store the new id
            return "not used";
        } else if(endpoint.equals("ead")) {
            if(id.isEmpty()) {
                return "";
            }

            if(!eadIDs.contains(id)) {
                eadIDs.add(id);
            } else {
                String nid = "";

                do {
                    nid = id + " ##" + randomString.nextString();
                } while(eadIDs.contains(nid));

                eadIDs.add(nid);

                //String message = "Duplicate EAD Id: "  + id  + " Changed to: " + nid + "\n";
                //aspaceCopyUtil.addErrorMessage(message);

                // assign id to new id
                id = nid;
            }

            return id;
        } else {
            return id;
        }
    }

    /**
     * Method to concat the id parts in a string array into a full id delimited by "."
     *
     * @param ids
     * @return
     */
    private String concatIdParts(String[] ids) {
        String fullId = "";
        for(int i = 0; i < ids.length; i++) {
            if(!ids[i].isEmpty() && i == 0) {
                fullId += ids[0];
            } else if(!ids[i].isEmpty()) {
                fullId += "."  + ids[i];
            }
        }

        return fullId;
    }


    /**
     * Method to set the current resource record identifier. Usefull for error
     * message generation
     *
     * @param identifier
     */
    public void setCurrentResourceRecordIdentifier(String identifier) {
        this.currentResourceRecordIdentifier = identifier;
    }

    /**
     * Method to convert any string to integer
     *
     * @param string
     * @return
     */
    private Integer convertToInteger(String string) {
        Integer number = null;

        try {
            if(!string.isEmpty()) {
                number = new Integer(string);
            }
        } catch(NumberFormatException nfe) { }

        return number;
    }

    /**
     * Method to convert a number to an integer
     * @param i
     * @return
     */
    private Boolean convertToBoolean(Integer i) {
        if(i == 1) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * Method to test the conversion without having to startup the gui
     *
     * @param args
     */
    public static void main(String[] args) throws JSONException {
        String host = "http://archives-dev.library.illinois.edu/archondev/miami";
        ArchonClient archonClient = new ArchonClient(host, "admin", "admin");
        archonClient.getSession();

        ASpaceMapper mapper = new ASpaceMapper();

        // the json object containing list of records
        JSONObject records;

        records = archonClient.getDigitalObjectRecords();
        System.out.println("Total Digital Object Records: " + records.length());

        Iterator<String> keys = records.sortedKeys();
        while(keys.hasNext()) {
            String key  = keys.next();
            JSONObject record = records.getJSONObject(key);

            System.out.println("Converting digital object record: " + record.get("Title"));

            try {
                mapper.convertDigitalObject(record);

                JSONArray components = record.getJSONArray("components");

                for(int i = 0; i < components.length(); i++) {
                    JSONObject component = components.getJSONObject(i);
                    mapper.convertToDigitalObjectComponent(component);
                    System.out.println("Converting digital object component: " + component.get("Title"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //JSONObject componentRecords = archonClient.getCollectionContentRecords(key);
            //System.out.println("Total Collection Content Records: " + contentRecordsJS.length());
        }
    }
}
