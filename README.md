ArchonMigrator
============

This is a small Java desktop Program for migrating Archon data into an ArchivesSpace instance which replaces the original migration tool.

Features of this tool are

* Migrates records directly in ArchivesSpace version 1.4.2. NOTE: If migrating from Archon to ArchivesSpace v1.5.x, you need to install ArchivesSpace v1.4.2 and run this migration tool first.  Then upgrade your ArchivesSpace installation to ArchivesSpace v1.5.x.
* Migrates records into non-empty ArchivesSpace instance
* Similar user interface as the AT migration tool.
* More robust, failure to migrate single record generally won't result in the tool stopping
* Improve migration report generation. Only relevant information is provided to users.
* No need to do any post processing on migrated data to remove empty or unused Classification or Location records
* Migrate both linked and unlinked digital objects. The current tool only migrate digital objects linked to collections.
* Automatically remove bbcode tags in titles and replace them with html code or just strip them out
* Correctly link related Creator records

####Using

1. Unpack the zip file in a convenient location.
2. From the command line, change into the ArchonMigrator directory and execute either "run.bat" (Windows), or "run.sh" (*nix based systems). On windows, it is also possible to double click on the "run.bat" file. This brings up the main application window.
3. Enter the connection information for the Archon and ArchivesSpace instances
4. To download the digital object files stored in the database along with copying the meta data, select "Download Digital Object Files" and set an appropriate download folder by using the "Set Download Folder" button.
5. Select the "Set Default Repository" to set which Repository Accession and Unlinked digital Objects are copied to. The default one selected, is to copy Accession records to the same repository of any Collection records they are linked to, or the first repository if they are not. You can also select a specific repository from the drop down list.
6. Press the "Copy To Archives Space" button to start the migration process.
