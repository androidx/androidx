import androidx.build.LibraryGroups
import androidx.build.LibraryVersions
import androidx.build.dependencies.ESPRESSO_CORE_TMP
import androidx.build.dependencies.ESPRESSO_EXCLUDE
import androidx.build.dependencies.TEST_RUNNER_TMP

plugins {
    id("SupportAndroidLibraryPlugin")
}

dependencies {
    api(project(":core"))
    api(project(":annotation"))
    api(project(":interpolator"))
    api(project(":collection"))
    api(project(":legacy-support-core-ui"))

    androidTestImplementation(TEST_RUNNER_TMP, ESPRESSO_EXCLUDE)
    androidTestImplementation(ESPRESSO_CORE_TMP, ESPRESSO_EXCLUDE)
    androidTestImplementation(project(":internal-testutils"))
}

supportLibrary {
    name = "Android Support Custom Tabs"
    publish = true
    mavenVersion = LibraryVersions.SUPPORT_LIBRARY
    mavenGroup = LibraryGroups.BROWSER
    inceptionYear = "2015"
    description = "Android Support Custom Tabs"
    minSdkVersion = 15
}
