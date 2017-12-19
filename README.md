ArchonMigrator
============

The Archon to ArchivesSpace migration tool enables migration of data from an Archon instance to an ArchivesSpace instance. It is the most comprehensive and efficient option for people who have not previously migrated and want to migrate all of their Archon data to ArchivesSpace.

More instructions and a data mapping are available at on the ArchivesSpace migration tools and data mapping page at http://archivesspace.org/using-archivesspace/migration-tools-and-data-mapping/.

We recommend migrating to the latest version of ArchivesSpace when possible. Archon users who are ready to move to the latest version of ArchivesSpace should use version ?.?.? of the tool. Archon users who wish to move into a pre-1.5.0 version of ArchivesSpace due to the container management changes should use version 1.0.1 of the tool.

## Running the Archon migration tool

1. Unpack the zip file in a convenient location.
2. From the command line, change into the ArchonMigrator directory and execute either "run.bat" (Windows), or "run.sh" (\*nix based systems). On Windows, it is also possible to double click on the "run.bat" file. This brings up the main application window.
3. Enter the connection information for the Archon and ArchivesSpace instances
4. To download the digital object files stored in the database along with copying the metadata, select "Download Digital Object Files" and set an appropriate download folder by using the "Set Download Folder" button.
5. Select the "Set Default Repository" to set which Repository Accession and Unlinked digital Objects are copied to. The default is to copy Accession records to the same repository of any Collection records they are linked to, or the first repository if they are not. You can also select a specific repository from the drop down list.
6. Press the "Copy To Archives Space" button to start the migration process.
