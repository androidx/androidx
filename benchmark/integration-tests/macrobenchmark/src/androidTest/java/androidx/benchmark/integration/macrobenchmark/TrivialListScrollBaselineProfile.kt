/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.benchmark.integration.macrobenchmark

import android.content.Intent
import android.graphics.Point
import androidx.benchmark.macro.ExperimentalBaselineProfilesApi
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@LargeTest
@SdkSuppress(minSdkVersion = 29)
@OptIn(ExperimentalBaselineProfilesApi::class)
class TrivialListScrollBaselineProfile {
    @get:Rule
    val baselineRule = BaselineProfileRule()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instrumentation)
    }

    @Test
    fun baselineProfiles() {
        baselineRule.collectBaselineProfile(
            packageName = "androidx.benchmark.integration.macrobenchmark.target",
            profileBlock = {
                val intent = Intent()
                intent.action = ACTION
                startActivityAndWait(intent)
                val recycler = device.wait(
                    Until.findObject(
                        By.res(
                            PACKAGE_NAME,
                            RESOURCE_ID
                        )
                    ),
                    TIMEOUT
                )
                // Setting a gesture margin is important otherwise gesture nav is triggered.
                recycler.setGestureMargin(device.displayWidth / 5)
                repeat(10) {
                    // From center we scroll 2/3 of it which is 1/3 of the screen.
                    recycler.drag(Point(0, recycler.visibleCenter.y / 3))
                    device.waitForIdle()
                }
            }
        )
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.benchmark.integration.macrobenchmark.target"
        private const val ACTION =
            "androidx.benchmark.integration.macrobenchmark.target.RECYCLER_VIEW"
        private const val RESOURCE_ID = "recycler"
        // The timeout
        private const val TIMEOUT = 2000L
    }
}
