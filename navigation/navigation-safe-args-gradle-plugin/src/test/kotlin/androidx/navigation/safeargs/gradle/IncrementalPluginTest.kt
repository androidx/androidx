/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigation.safeargs.gradle

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// Does not work in the Android Studio
@Ignore // b/186177309
@RunWith(Parameterized::class)
class IncrementalPluginTest(private val generateKotlin: Boolean) : BasePluginTest() {

    private val extension = if (generateKotlin) { ".kt" } else { ".java" }

    private fun setupBuildGradle() {
        if (generateKotlin) {
            setupSimpleKotlinBuildGradle()
        } else {
            setupSimpleBuildGradle()
        }
    }

    @Test
    fun incrementalAdd() {
        setupBuildGradle()
        runGradle("assembleDebug").assertSuccessfulTask("assembleDebug")
        val nextLastMod = assertGenerated("debug/$NEXT_DIRECTIONS$extension").lastModified()

        testData("incremental-test-data/add_nav.xml").copyTo(navResource("add_nav.xml"))

        // lastModified has one second precision on certain platforms and jdk versions
        // so sleep for a second
        Thread.sleep(SEC)
        runGradle("assembleDebug").assertSuccessfulTask("assembleDebug")
        assertGenerated("debug/$ADDITIONAL_DIRECTIONS$extension")
        val newNextLastMod = assertGenerated("debug/$NEXT_DIRECTIONS$extension").lastModified()
        MatcherAssert.assertThat(
            newNextLastMod,
            CoreMatchers.`is`(nextLastMod)
        )
    }

    @Test
    fun incrementalModify() {
        setupBuildGradle()
        testData("incremental-test-data/add_nav.xml").copyTo(navResource("add_nav.xml"))

        runGradle("assembleDebug").assertSuccessfulTask("assembleDebug")
        val mainLastMod = assertGenerated("debug/$MAIN_DIRECTIONS$extension").lastModified()
        val additionalLastMod = assertGenerated("debug/$ADDITIONAL_DIRECTIONS$extension")
            .lastModified()
        assertGenerated("debug/$NEXT_DIRECTIONS$extension")

        testData("incremental-test-data/modified_nav.xml").copyTo(
            navResource("nav_test.xml"),
            true
        )

        // lastModified has one second precision on certain platforms and jdk versions
        // so sleep for a second
        Thread.sleep(SEC)
        runGradle("assembleDebug").assertSuccessfulTask("assembleDebug")
        val newMainLastMod = assertGenerated("debug/$MAIN_DIRECTIONS$extension").lastModified()
        // main directions were regenerated
        MatcherAssert.assertThat(
            newMainLastMod,
            CoreMatchers.not(mainLastMod)
        )

        // but additional directions weren't touched
        val newAdditionalLastMod =
            assertGenerated("debug/$ADDITIONAL_DIRECTIONS$extension").lastModified()
        MatcherAssert.assertThat(
            newAdditionalLastMod,
            CoreMatchers.`is`(additionalLastMod)
        )

        assertGenerated("debug/$MODIFIED_NEXT_DIRECTIONS$extension")
        assertNotGenerated("debug/$NEXT_DIRECTIONS$extension")
    }

    @Test
    fun incrementalRemove() {
        setupBuildGradle()
        testData("incremental-test-data/add_nav.xml").copyTo(navResource("add_nav.xml"))

        runGradle("assembleDebug").assertSuccessfulTask("assembleDebug")
        val mainLastMod = assertGenerated("debug/$MAIN_DIRECTIONS$extension").lastModified()
        assertGenerated("debug/$ADDITIONAL_DIRECTIONS$extension")

        val wasRemoved = navResource("add_nav.xml").delete()
        MatcherAssert.assertThat(wasRemoved, CoreMatchers.`is`(true))

        // lastModified has one second precision on certain platforms and jdk versions
        // so sleep for a second
        Thread.sleep(SEC)
        runGradle("assembleDebug").assertSuccessfulTask("assembleDebug")
        val newMainLastMod = assertGenerated("debug/$MAIN_DIRECTIONS$extension").lastModified()
        // main directions weren't touched
        MatcherAssert.assertThat(
            newMainLastMod,
            CoreMatchers.`is`(mainLastMod)
        )

        // but additional directions are removed
        assertNotGenerated("debug/$ADDITIONAL_DIRECTIONS$extension")
    }

    @Test
    fun invalidModify() {
        setupBuildGradle()
        testData("incremental-test-data/add_nav.xml").copyTo(navResource("add_nav.xml"))
        runGradle("generateSafeArgsDebug").assertSuccessfulTask("generateSafeArgsDebug")
        val step1MainLastMod = assertGenerated("debug/$MAIN_DIRECTIONS$extension").lastModified()
        val step1AdditionalLastMod =
            assertGenerated("debug/$ADDITIONAL_DIRECTIONS$extension").lastModified()
        assertGenerated("debug/$NEXT_DIRECTIONS$extension")

        testData("invalid/failing_nav.xml")
            .copyTo(navResource("nav_test.xml"), true)
        Thread.sleep(SEC)
        runAndFailGradle("generateSafeArgsDebug").assertFailingTask("generateSafeArgsDebug")
        val step2MainLastMod = assertGenerated("debug/$MAIN_DIRECTIONS$extension").lastModified()
        // main directions were regenerated
        MatcherAssert.assertThat(
            step2MainLastMod,
            CoreMatchers.not(step1MainLastMod)
        )

        // but additional directions weren't touched
        val step2AdditionalLastMod =
            assertGenerated("debug/$ADDITIONAL_DIRECTIONS$extension").lastModified()
        MatcherAssert.assertThat(
            step2AdditionalLastMod,
            CoreMatchers.`is`(step1AdditionalLastMod)
        )

        val step2ModifiedTime =
            assertGenerated("debug/$MODIFIED_NEXT_DIRECTIONS$extension").lastModified()
        assertNotGenerated("debug/$NEXT_DIRECTIONS$extension")

        testData("incremental-test-data/modified_nav.xml").copyTo(
            navResource("nav_test.xml"),
            true
        )
        Thread.sleep(SEC)
        runGradle("generateSafeArgsDebug").assertSuccessfulTask("generateSafeArgsDebug")

        // additional directions are touched because once task failed,
        // gradle next time makes full run
        val step3AdditionalLastMod =
            assertGenerated("debug/$ADDITIONAL_DIRECTIONS$extension").lastModified()
        MatcherAssert.assertThat(
            step3AdditionalLastMod,
            CoreMatchers.not(step2AdditionalLastMod)
        )

        val step3ModifiedTime =
            assertGenerated("debug/$MODIFIED_NEXT_DIRECTIONS$extension").lastModified()
        MatcherAssert.assertThat(
            step2ModifiedTime,
            CoreMatchers.not(step3ModifiedTime)
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "generateKotlin={0}")
        fun data() = listOf(false) // , true) testing with kotlin is disabled b/165307851
    }
}