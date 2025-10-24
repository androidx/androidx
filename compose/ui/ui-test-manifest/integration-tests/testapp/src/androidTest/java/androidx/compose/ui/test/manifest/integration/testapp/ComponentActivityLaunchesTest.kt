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

package androidx.compose.ui.test.manifest.integration.testapp

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ComponentActivityLaunchesTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>(StandardTestDispatcher())

    @Test
    fun activity_launches() {
        rule.setContent {}
        // Test does not crash and does not time out
    }

    // Regression test for b/383368165
    // When targeting SDK 35, an activity is edge-to-edge by default and the action bar will overlap
    // the content. We can only detect this by taking a screenshot, assertIsDisplayed() doesn't work
    // due to b/383368165#comment2.
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun activity_notCoveredByActionBar() {
        val color = Color.Red
        val size = 10
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(size.toDp()).background(color).testTag("box"))
            }
        }

        rule.onNodeWithTag("box").captureToImage().let {
            assert(it.width == size && it.height == size) {
                // We don't really need to test this, but better be safe then sorry
                "Screenshot size should be 10x10, but was ${it.width}x${it.height}"
            }
            val map = it.toPixelMap()
            for (y in 0 until map.height) {
                for (x in 0 until map.width) {
                    assert(map[x, y] == color) {
                        "Pixel at ($x, $y) is ${map[x, y]} instead of $color"
                    }
                }
            }
        }
    }
}
