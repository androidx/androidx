/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.UiDevice
import androidx.wear.compose.material3.macrobenchmark.common.BaselineProfileScreens
import org.junit.Rule
import org.junit.Test

// This test generates a baseline profile rules file that can be parsed to produce the
// baseline-prof.txt files for the Wear Compose libraries.
// 1) Build and run debug build of androidx.wear.compose.material3.macrobenchmark-target
//    (not minified, because we need non-obfuscated method/class names)
// 2) Run this BaselineProfile test then click 'Baseline profile results' link
// 3) Build profileparser:
//    If necessary, include it in settings.gradle:
//      includeProject(":wear:compose:integration-tests:profileparser", [BuildType.MAIN])
//    ./gradlew :wear:compose:integration-tests:profileparser:assemble
// 4) Run profileparser for wear.compose.material3
//    From <workspace>/frameworks/support:
//    java -jar
//
// ../../out/androidx/wear/compose/integration-tests/profileparser/build/libs/profileparser-all.jar
//      <input-generated-file eg ./wear/compose3/BaselineProfile_profile-baseline-prof.txt>
//      <library-name e.g. androidx/wear/compose/material3>
//      <output-file eg ./wear/compose/compose-material3/src/main/baseline-prof.txt>
@LargeTest
class BaselineProfile {

    @get:Rule val baselineRule = BaselineProfileRule()

    @Test
    fun profile() {
        baselineRule.collect(
            packageName = PACKAGE_NAME,
            profileBlock = {
                val intent = Intent()
                intent.action = BASELINE_ACTIVITY
                startActivityAndWait(intent)
                device.waitForIdle()
                device.iterateAllPages(pageCount = BaselineProfileScreens.size)
            }
        )
    }

    private fun UiDevice.iterateAllPages(pageCount: Int) {
        // Get screen dimensions
        val screenWidth = displayWidth
        val screenHeight = displayHeight
        // Calculate swipe start and end points (adjust these as needed)
        val startX = (screenWidth * 0.8).toInt() // 80% of screen width
        val startY = screenHeight / 2 // Middle of the screen
        val endX = (screenWidth * 0.2).toInt() // 20% of screen width
        val endY = startY
        for (i in 1 until pageCount) {
            swipe(startX, startY, endX, endY, 10)
            waitForIdle()
        }
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.wear.compose.material3.macrobenchmark.target"
        private const val BASELINE_ACTIVITY = "$PACKAGE_NAME.BASELINE_ACTIVITY"
    }
}
