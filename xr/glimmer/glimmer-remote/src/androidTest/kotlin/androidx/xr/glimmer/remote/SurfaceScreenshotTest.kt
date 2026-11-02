/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.glimmer.remote

import android.content.Context
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRectangleShape
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.player.compose.RcPlayerTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class SurfaceScreenshotTest {

    @get:Rule(order = 0) val composeTestRule = RcPlayerTestRule()

    @get:Rule(order = 1) val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_DIRECTORY)

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun enabled() {
        composeTestRule.setRemoteContent(context) {
            ContentWrapper {
                RemoteBox(
                    RemoteModifier.fillMaxSize()
                        .surface(
                            enabled = true.rb,
                            pressed = false.rb,
                            focused = false.rb,
                        )
                )
            }
        }

        composeTestRule.assertRootAgainstGolden("surface_enabled", screenshotRule)
    }

    @Test
    fun focused() {
        composeTestRule.setRemoteContent(context) {
            ContentWrapper {
                RemoteBox(
                    RemoteModifier.fillMaxSize()
                        .surface(enabled = true.rb, pressed = false.rb, focused = true.rb)
                )
            }
        }

        composeTestRule.assertRootAgainstGolden("surface_focused", screenshotRule)
    }

    @Test
    fun focusedAndPressed() {
        composeTestRule.setRemoteContent(context) {
            ContentWrapper {
                RemoteBox(
                    RemoteModifier.fillMaxSize()
                        .surface(enabled = true.rb, pressed = true.rb, focused = true.rb)
                )
            }
        }

        composeTestRule.assertRootAgainstGolden("surface_focused_and_pressed", screenshotRule)
    }

    @Test
    fun disabled() {
        composeTestRule.setRemoteContent(context) {
            ContentWrapper {
                RemoteBox(
                    RemoteModifier.fillMaxSize()
                        .surface(
                            enabled = false.rb,
                            pressed = false.rb,
                            focused = false.rb,
                        )
                )
            }
        }

        composeTestRule.assertRootAgainstGolden("surface_disabled", screenshotRule)
    }

    @Test
    fun circleShape() {
        composeTestRule.setRemoteContent(context) {
            ContentWrapper {
                RemoteBox(RemoteModifier.fillMaxSize().surface(shape = RemoteCircleShape))
            }
        }

        composeTestRule.assertRootAgainstGolden("surface_circleShape", screenshotRule)
    }

    @Test
    fun rectangleShape() {
        composeTestRule.setRemoteContent(context) {
            ContentWrapper {
                RemoteBox(RemoteModifier.fillMaxSize().surface(shape = RemoteRectangleShape))
            }
        }

        composeTestRule.assertRootAgainstGolden("surface_rectangleShape", screenshotRule)
    }

    @Composable
    @RemoteComposable
    private fun ContentWrapper(content: @Composable () -> Unit) {
        RemoteBox(
            modifier = RemoteModifier.fillMaxSize().background(Color.Black.rc).padding(12.rdp),
            contentAlignment = RemoteAlignment.Center,
            content = content,
        )
    }
}
