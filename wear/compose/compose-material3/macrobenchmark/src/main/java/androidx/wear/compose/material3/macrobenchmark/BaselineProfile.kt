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
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.filters.LargeTest
import androidx.wear.compose.material3.macrobenchmark.common.baselineprofile.BaselineProfileScreens
import java.lang.Thread.sleep
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
                iterateAllPages(pageCount = BaselineProfileScreens.size)
            }
        )
    }

    private fun MacrobenchmarkScope.iterateAllPages(pageCount: Int) {
        val screenWidth = device.displayWidth
        val screenHeight = device.displayHeight
        val startX = (screenWidth * 0.9).toInt()
        val y = (screenHeight * 0.9).toInt()
        val endX = (screenWidth * 0.1).toInt()
        for (i in 0 until pageCount) {
            BaselineProfileScreens[i].exercise.invoke(this)
            device.waitForIdle()
            sleep(1_000L)
            device.swipe(startX, y, endX, y, 10)
            device.waitForIdle()
            sleep(1_000L)
        }
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.wear.compose.material3.macrobenchmark.target"
        private const val BASELINE_ACTIVITY = "$PACKAGE_NAME.BASELINE_ACTIVITY"
    }
}
