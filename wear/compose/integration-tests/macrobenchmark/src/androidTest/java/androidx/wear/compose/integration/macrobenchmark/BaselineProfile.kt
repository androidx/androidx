/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.integration.macrobenchmark.test

import android.content.Intent
import android.graphics.Point
import androidx.benchmark.macro.ExperimentalBaselineProfilesApi
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.testutils.createCompilationParams
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runners.Parameterized

// This test generates a baseline profile rules file that can be parsed to produce the
// baseline-prof.txt files for the Wear Compose libraries.
// 1) Build and install debug build of androidx.wear.compose.integration.macrobenchmark-target
//    onto a device (not minified, because we need non-obsfuscated method/class names)
// 2) Run this test on the device - search for Benchmark in logcat to discover the location
//    of the generated file
//    e.g. /sdcard/Android/media/androidx.wear.compose.integration.macrobenchmark.test/
//         additional_test_output/BaselineProfile_profile-baseline-prof.txt
// 3) Copy the generated file to your workspace:
//    adb pull <path-to-file-from-step-2 e.g. xxx/BaselineProfile_profile-baseline-prof.txt>
//             <workspace path e.g. xxx/frameworks/support/wear/compose/>
// 4) Build profileparser:
//    If necessary, include it in settings.gradle:
//      includeProject(":wear:compose:integration-tests:profileparser",
//                     "wear/compose/integration-tests/profileparser",
//                     [BuildType.MAIN])
//    ./gradlew :wear:compose:integration-tests:profileparser:assemble
// 5) Run profileparser for each of wear.compose.material, wear.compose.foundation and
//    wear.compose.navigation. From <workspace>/frameworks/support:
//    java -jar
//      ../../out/androidx/wear/compose/integration-tests/profileparser/build/libs/profileparser-all.jar
//      <input-generated-file eg ./wear/compose/BaselineProfile_profile-baseline-prof.txt>
//      <library-name e.g. androidx/wear/compose/material>
//      <output-file eg ./wear/compose/compose-material/src/androidMain/baseline-prof.txt>
@LargeTest
@SdkSuppress(minSdkVersion = 29)
@OptIn(ExperimentalBaselineProfilesApi::class)
class BaselineProfile {

    @get:Rule
    val baselineRule = BaselineProfileRule()

    private lateinit var device: UiDevice
    private val ALERT_DIALOG = "alert-dialog"
    private val CONFIRMATION_DIALOG = "confirmation-dialog"
    private val STEPPER = "stepper"

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instrumentation)
    }

    @Test
    fun profile() {
        baselineRule.collectBaselineProfile(
            packageName = PACKAGE_NAME,
            profileBlock = {
                val intent = Intent()
                intent.action = ACTION
                startActivityAndWait(intent)
                testDestination(ALERT_DIALOG)
                testDestination(CONFIRMATION_DIALOG)
                testDestination(STEPPER)

                // Scroll down to view remaining UI elements
                // Setting a gesture margin is important otherwise gesture nav is triggered.
                val list = device.findObject(By.desc(CONTENT_DESCRIPTION))
                list.setGestureMargin(device.displayWidth / 5)
                repeat(25) {
                    list.drag(Point(list.visibleCenter.x, list.visibleCenter.y / 3))
                    device.waitForIdle()
                }
            }
        )
    }

    private fun testDestination(name: String) {
        device.findObject(By.desc(name)).click()
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.wear.compose.integration.macrobenchmark.target"
        private const val ACTION =
            "androidx.wear.compose.integration.macrobenchmark.target.BASELINE_ACTIVITY"

        @Parameterized.Parameters(name = "compilation={0}")
        @JvmStatic
        fun parameters() = createCompilationParams()
    }
}