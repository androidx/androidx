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
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@LargeTest
@SdkSuppress(minSdkVersion = 29)
@OptIn(ExperimentalBaselineProfilesApi::class)
@Ignore
class GithubBrowserBaselineProfile {

    /**
     * This test is targeting the GitHubBrowserSample at:
     * https://github.com/android/architecture-components-samples
     *
     * We made the following changes to the sample app:
     *
     * * Build a `release` variant of the app with `minifyEnabled` set to `false`.
     * * Use the latest version of SDK tools, and the latest alpha for Android Gradle Plugin.
     * * Add a button to search fragment to make it easy to drive via UiAutomator.
     * * Use the latest alphas of FLAN libraries.
     */

    @get:Rule
    val baselineRule = BaselineProfileRule()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instrumentation)
    }

    @Test
    fun githubBrowserProfiles() {
        baselineRule.collectBaselineProfile(
            packageName = PACKAGE_NAME,
            profileBlock = {
                startActivityAndWait()
                val searchText = device.findObject(By.res(PACKAGE_NAME, REPO_SEARCH_ID))
                searchText?.text = "Test"
                val search = device.findObject(By.res(PACKAGE_NAME, SEARCH_ID))
                search.click()
                device.waitForIdle()
                val condition = Until.hasObject(By.res(PACKAGE_NAME, DESCRIPTION))
                device.wait(condition, 10_000)
                val recycler = device.findObject(By.res(PACKAGE_NAME, RECYCLER_ID))
                // Setting a gesture margin is important otherwise gesture nav is triggered.
                recycler.setGestureMargin(device.displayWidth / 5)
                repeat(10) {
                    // From center we scroll 2/3 of it which is 1/3 of the screen.
                    recycler.drag(Point(0, recycler.visibleCenter.y / 3))
                    device.waitForIdle()
                }
                device.pressBack()
                device.waitForIdle()
            }
        )
    }

    companion object {
        private const val PACKAGE_NAME = "com.android.example.github"
        private const val REPO_SEARCH_ID = "input"
        private const val SEARCH_ID = "search"
        private const val DESCRIPTION = "desc"
        private const val RECYCLER_ID = "repo_list"
    }
}
