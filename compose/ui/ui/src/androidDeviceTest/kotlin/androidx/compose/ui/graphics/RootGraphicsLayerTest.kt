/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.graphics

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.layout.assertCenterPixelColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RootGraphicsLayerTest {
    @get:Rule
    val rule =
        createAndroidComposeRule(
            ComponentActivity::class.java,
            effectContext = StandardTestDispatcher(),
        )

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun rootLayerRedrawnAfterRootRemoval() {
        var showContent by mutableStateOf(true)
        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.Blue).testTag("box")) {
                AndroidView(
                    factory = {
                        ComposeView(it).apply {
                            setContent {
                                if (showContent) {
                                    Box(Modifier.fillMaxSize().background(Color.Red))
                                }
                            }
                        }
                    }
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("box").captureToImage().assertCenterPixelColor(Color.Red)
        showContent = false
        rule.waitForIdle()
        rule.onNodeWithTag("box").captureToImage().assertCenterPixelColor(Color.Blue)
    }
}
