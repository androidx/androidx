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

package androidx.compose.remote.creation.compose.modifier

import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRectangleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteColor
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI.Companion.DefaultContainerSize
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class ClipModifierTest {
    @get:Rule
    val composeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )

    private val gridScreenshotUI = GridScreenshotUI()

    @Test
    fun grid() =
        composeTestRule.runScreenshotTest {
            val clips =
                listOf<Pair<String, @Composable RemoteModifier.() -> RemoteModifier>>(
                    "RectangleShape" to { clip(RemoteRectangleShape) },
                    "CircleShape" to { clip(RemoteCircleShape) },
                    "RoundedCornerShape size" to { clip(RemoteRoundedCornerShape(size = 10.rdp)) },
                    "RoundedCornerShape percent 25" to
                        {
                            clip(RemoteRoundedCornerShape(percent = 25))
                        },
                    "RoundedCornerShape percent 50" to
                        {
                            clip(RemoteRoundedCornerShape(percent = 50))
                        },
                    "RoundedCornerShape custom size" to
                        {
                            clip(RemoteRoundedCornerShape(topStart = 10.rdp, bottomEnd = 10.rdp))
                        },
                )

            gridScreenshotUI.GridContent(
                sequence {
                        for ((name, clipFn) in clips) {
                            yield(
                                name to
                                    @RemoteComposable @Composable {
                                        RemoteBox(
                                            modifier =
                                                RemoteModifier.size(DefaultContainerSize)
                                                    .clipFn()
                                                    .background(Color.Red)
                                        )
                                    }
                            )
                        }
                    }
                    .toList()
            )
        }

    @Test
    fun clipWithRemoteRoundedCornerShape_rtl() =
        composeTestRule.runScreenshotTest(creationComposableWrapper = ComposableWrappers.rtl) {
            val shapes =
                listOf(
                    "topStart" to RemoteRoundedCornerShape(topStart = 20.rdp),
                    "topEnd" to RemoteRoundedCornerShape(topEnd = 20.rdp),
                    "bottomStart" to RemoteRoundedCornerShape(bottomStart = 20.rdp),
                    "bottomEnd" to RemoteRoundedCornerShape(bottomEnd = 20.rdp),
                )

            val items = mutableListOf<Pair<String, @RemoteComposable @Composable () -> Unit>>()

            for ((name, shape) in shapes) {
                items.add(
                    name to
                        @RemoteComposable @Composable {
                            RemoteBox(
                                modifier =
                                    RemoteModifier.size(DefaultContainerSize)
                                        .clip(shape)
                                        .background(Color.Red)
                            )
                        }
                )
            }

            gridScreenshotUI.GridContent(items)
        }

    @Test
    fun clipWithDrawWithContent() =
        composeTestRule.runScreenshotTest {
            RemoteBox(
                modifier =
                    RemoteModifier.size(50.rdp)
                        .clip(RemoteRoundedCornerShape(size = 20.rdp))
                        .drawWithContent {
                            val paint = RemotePaint()
                            paint.color = RemoteColor(Color.Blue)
                            drawRect(paint = paint)
                            drawContent()
                        }
            )
        }

    @Test
    fun clipWithBackground() =
        composeTestRule.runScreenshotTest {
            val color = rememberNamedRemoteColor("test", Color.Blue)
            RemoteBox(
                modifier =
                    RemoteModifier.size(50.rdp)
                        .clip(RemoteRoundedCornerShape(size = 20.rdp))
                        .background(color)
            )
        }
}
