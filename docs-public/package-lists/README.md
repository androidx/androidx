# Package lists

A `package-list` file is a list of the packages documented at a particular refdocs site. They are
used to enable linking to external documentation from the AndroidX docs.

To add a new package list, create a directory and add a `url` file containing the base URL of the
refdocs. For instance, in the Android refdocs, the `android.app.Fragment` class is documented at
`https://developer.android.com/reference/android/app/Fragment`, so the base URL for the refdocs is
`https://developer.android.com/reference`.

From `frameworks/support`, run `development/referenceDocs/UpdatePackageLists.main.kts` to download
the `package-list` file for the refdocs (the script requires the [Kotlin CLI compiler](https://kotlinlang.org/docs/command-line.html)
to be on the `PATH`. If the script fails to download a `package-list` file, it will also try to
download `element-list`, which is created by newer versions of the javadoc tool.

The following files can optionally be defined to control how the update script processes the
`package-list`:
* `download-url`: The script to update package lists expects that the `package-list` or
  `element-list` is located directly under the base URL from the `url` file. If it is located at a
  different URL, that can be set in the `download-url` file.
* `format`: A `package-list` can optionally have a `format` line that tells dackka how to structure
  the links to packages in the list. If the downloaded `package-list` file does not list a format
  but the default does not generate the correct links, the update script can prepend the `format`
  file to the `package-list` (the valid formats are
  [defined in dokka](https://github.com/Kotlin/dokka/blob/master/dokka-subprojects/plugin-base/src/main/kotlin/org/jetbrains/dokka/base/resolvers/shared/RecognizedLinkFormat.kt)).
* `filter`: A filter file contains a regular expression. When updating package lists with the
  script, only the lines in the downloaded `package-list` file that match the regex are kept. An
  example usage of this is for the `javase8` package list. Most `java` packages are documented
  through the `android` refdocs, so the `javase8` filter keeps only `java.awt` and `javax` packages
  which are not included in the android documentation.
