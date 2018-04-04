import androidx.build.dependencies.*
import androidx.build.LibraryGroups
import androidx.build.LibraryVersions

plugins {
    id("SupportAndroidLibraryPlugin")
}

dependencies {
    api(project(":annotation"))
    api(project(":core"))
    api(project(":customview"))

    androidTestImplementation(TEST_RUNNER_TMP, ESPRESSO_EXCLUDE)
    androidTestImplementation(ESPRESSO_CORE_TMP, ESPRESSO_EXCLUDE)
    androidTestImplementation(ESPRESSO_CONTRIB_TMP, ESPRESSO_EXCLUDE)
    androidTestImplementation(MOCKITO_CORE, BYTEBUDDY_EXCLUDE)
    androidTestImplementation(DEXMAKER_MOCKITO, BYTEBUDDY_EXCLUDE)
    androidTestImplementation(project(":internal-testutils")) {
        exclude(group = "androidx.coordinatorlayout", module = "coordinatorlayout")
    }
}

android {
    sourceSets.getByName("main").res.srcDirs("src/main/res", "src/main/res-public")

    buildTypes.all {
        consumerProguardFile("proguard-rules.pro")
    }
}

supportLibrary {
    name = "Android Support Library Coordinator Layout"
    publish = true
    mavenVersion = LibraryVersions.SUPPORT_LIBRARY
    mavenGroup = LibraryGroups.COORDINATORLAYOUT
    inceptionYear = "2011"
    description = "The Support Library is a static library that you can add to your Android application in order to use APIs that are either not available for older platform versions or utility APIs that aren't a part of the framework APIs. Compatible on devices running API 14 or later."
    failOnUncheckedWarnings = false
    failOnDeprecationWarnings = false
}
