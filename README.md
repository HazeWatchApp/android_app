Public repository for HazeWatch application for Android.

More details at http://hazewatchapp.com.

Please contribute and make a pull request for your changes.

Instructions for building:
--------------------------
1. Create a Google Analytics ID with a string resource ID of: **ga_tracking_id**. Alternatively remove *Google Analytics* from the project.
2. Fork [APIMs data project](https://github.com/HazeWatchApp/apims_data) and have the link to **index_v2.json** in the project as a String resource with the ID of: **data_source**.
3. Create a file called crashlytics.properties and create the following entry:
    * 'fabricApiKey=[your fabric API key] (alternatively remove *Crashlytics* from the project)
4. To do a release build, modify gradle.properties, pointing to a file containing the follwing properties:
    * storeFile (path to signing keystore)
    * keyAlias (key alias)
    * storePassword (store password)
    * keyPassword (key password)