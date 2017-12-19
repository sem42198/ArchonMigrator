ArchonMigrator
============

This is a small Java desktop Program for migrating Archon data into an ArchivesSpace instance which replaces the original migration tool.

####Features of this Tool

* Migrates records directly in ArchivesSpace version 2.x 
* Migrates records into non-empty ArchivesSpace instance
* Similar user interface as the AT migration tool.
* More robust, failure to migrate single record generally won't result in the tool stopping
* Improve migration report generation. Only relevant information is provided to users.
* No need to do any post processing on migrated data to remove empty or unused Classification or Location records
* Migrate both linked and unlinked digital objects.
* Automatically remove bbcode tags in titles and replace them with html code or just strip them out
* Correctly link related Creator records

####Using

1. Download and run the ArchonMigrator.jar file.
2. Enter the connection information for the Archon and ArchivesSpace instances
4. To download the digital object files stored in the database along with copying the meta data, select "Download Digital Object Files" and set an appropriate download folder by using the "Set Download Folder" button.
5. Select the "Set Default Repository" to set which Repository Accession and Unlinked digital Objects are copied to. The default one selected, is to copy Accession records to the same repository of any Collection records they are linked to, or the first repository if they are not. You can also select a specific repository from the drop down list.
6. Press the "Copy To Archives Space" button to start the migration process.


####Notes For Developers

To build this tool, use the IDE of your choice to build a standalone jar executable. Most of development work will probably take place on the following classes.

* ASpaceCopyUtil - This class essentially controls the migration process and interacts with the UI (dbCopyFrame) as well as the classes listed below.
* ASpaceMapper - Converts Archon JSON records to the ArchivesSpace JSON model.
* ASpaceClient - Saves records to ASpace through http post.
* ASpaceEnumUtil - Converts an Archon enum ID to the corresponding ASpace enum. Usually called by ASpaceMapper.