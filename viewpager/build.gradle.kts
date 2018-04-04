import androidx.build.LibraryGroups
import androidx.build.LibraryVersions
import androidx.build.dependencies.*

plugins {
    id("SupportAndroidLibraryPlugin")
}

dependencies {
    api(project(":annotation"))
    api(project(":core"))
    api(project(":customview"))

    androidTestImplementation(TEST_RUNNER_TMP, ESPRESSO_EXCLUDE)
    androidTestImplementation(ESPRESSO_CORE_TMP, ESPRESSO_EXCLUDE)
    androidTestImplementation(MOCKITO_CORE, BYTEBUDDY_EXCLUDE)
    androidTestImplementation(DEXMAKER_MOCKITO, BYTEBUDDY_EXCLUDE)
}

supportLibrary {
    name = "Android Support Library View Pager"
    publish = true
    mavenVersion = LibraryVersions.SUPPORT_LIBRARY
    mavenGroup = LibraryGroups.VIEWPAGER
    inceptionYear = "2018"
    description = "The Support Library is a static library that you can add to your Android application in order to use APIs that are either not available for older platform versions or utility APIs that aren't a part of the framework APIs. Compatible on devices running API 14 or later."
    failOnDeprecationWarnings = false
}
