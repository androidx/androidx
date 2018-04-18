import androidx.build.LibraryGroups
import androidx.build.LibraryVersions
import androidx.build.dependencies.*

plugins {
    id("SupportAndroidLibraryPlugin")
}

dependencies {
    api(project(":core"))

    androidTestImplementation(TEST_RUNNER_TMP, ESPRESSO_EXCLUDE)
    androidTestImplementation(ESPRESSO_CORE_TMP, ESPRESSO_EXCLUDE)
}

android {
    sourceSets.getByName("main").res.srcDir("res")
}

supportLibrary {
    name = "Android Percent Support Library"
    publish = true
    mavenVersion = LibraryVersions.SUPPORT_LIBRARY
    mavenGroup = LibraryGroups.PERCENTLAYOUT
    inceptionYear = "2015"
    description = "Android Percent Support Library"
}
