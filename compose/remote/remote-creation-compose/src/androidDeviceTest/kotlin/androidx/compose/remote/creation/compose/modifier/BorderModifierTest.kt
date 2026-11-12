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

package androidx.compose.remote.creation.compose.modifier

import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.verticalGradient
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRectangleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteTileMode
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI.Companion.DefaultContainerSize
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.runtime.Composable
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
class BorderModifierTest {
    @get:Rule
    val composeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )

    private val gridScreenshotUI = GridScreenshotUI()

    @Test
    fun grid() = composeTestRule.runScreenshotTest {
        val borders =
            listOf<Pair<String, @Composable RemoteModifier.() -> RemoteModifier>>(
                "1dp" to { border(1.rdp, RemoteColor.Red) },
                "2dp" to { border(2.rdp, RemoteColor.Red) },
                "3dp" to { border(3.rdp, RemoteColor.Red) },
                "color alpha" to { border(3.rdp, RemoteColor.Blue.copy(alpha = 0.5f.rf)) },
                // Ensure a non constant color
                "color ref" to
                    {
                        border(3.rdp, RemoteColor.Blue.copy(alpha = 0.5f.rf.createReference()))
                    },
                "RemoteRectangleShape" to
                    {
                        border(
                            width = 2.rdp,
                            color = RemoteColor.Green,
                            shape = RemoteRectangleShape,
                        )
                    },
                "RemoteCircleShape" to
                    {
                        border(
                            width = 2.rdp,
                            color = RemoteColor.Magenta,
                            shape = RemoteCircleShape,
                        )
                    },
                "RemoteRoundedCornerShape" to
                    {
                        border(
                            width = 2.rdp,
                            color = RemoteColor.Cyan,
                            shape = RemoteRoundedCornerShape(size = 10.rf),
                        )
                    },
                //                     Not supported in BorderModifierOperation
                //                    {
                //                        border(
                //                            width = 2.rdp,
                //                            color = Color.Cyan.rc,
                //                            shape = RemoteRoundedCornerShape(size = 10.rdp),
                //                        )
                //                    },
                //                    {
                //                        border(
                //                            width = 2.rdp,
                //                            color = Color.Gray.rc,
                //                            shape = RemoteRoundedCornerShape(percent = 50),
                //                        )
                //                    },
                //                    {
                //                        border(
                //                            width = 2.rdp,
                //                            color = Color.Black.rc,
                //                            shape = RemoteRoundedCornerShape(
                //                                topStart =
                //                                    10.rdp, bottomEnd = 10.rdp
                //                            ),
                //                        )
                //                    },
            )

        gridScreenshotUI.GridContent(
            sequence {
                for ((name, borderFn) in borders) {
                    yield(
                        name to
                            @RemoteComposable @Composable {
                                RemoteBox {
                                    RemoteBox(
                                        modifier =
                                            RemoteModifier.size(DefaultContainerSize).borderFn()
                                    )
                                }
                            }
                    )
                }
            }
                .toList()
        )
    }

    @Test
    fun borderAndBackgroundSolid() {
        composeTestRule.runScreenshotTest {
            RemoteBox(
                modifier =
                    RemoteModifier.fillMaxWidth()
                        .fillMaxHeight()
                        .background(RemoteColor.Red)
                        .border(3.rdp, RemoteColor.Cyan)
            )
        }
    }

    // b/519958284: not drawing the border
    @Test
    fun borderAndBackgroundBrush() {
        composeTestRule.runScreenshotTest {
            val gradient =
                RemoteBrush.verticalGradient(
                    colors = listOf(RemoteColor.Red, RemoteColor.Blue, RemoteColor.Green),
                    startY = 0f.rf,
                    endY = 10000f.rf,
                    tileMode = RemoteTileMode.Repeated,
                )
            RemoteBox(
                modifier =
                    RemoteModifier.fillMaxWidth()
                        .fillMaxHeight()
                        .background(gradient)
                        .border(3.rdp, RemoteColor.Cyan)
            )
        }
    }

    @Test
    fun borderAndBackgroundGrid() = composeTestRule.runScreenshotTest {
        gridScreenshotUI.GridContent(
            listOf(
                "Background (solid) > border" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(DefaultContainerSize)
                                    .background(RemoteColor.Blue)
                                    .border(width = 4.rdp, color = RemoteColor.Red)
                        )
                    },
                "Background (brush) > border" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(DefaultContainerSize)
                                    .background(
                                        RemoteBrush.verticalGradient(
                                            listOf(RemoteColor.Blue, RemoteColor.Cyan)
                                        )
                                    )
                                    .border(width = 4.rdp, color = RemoteColor.Red)
                        )
                    },
                "Empty" to {},
                "Border > background (solid)" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(DefaultContainerSize)
                                    .border(width = 4.rdp, color = RemoteColor.Red)
                                    .background(RemoteColor.Blue)
                        )
                    },
                "Border > background (brush)" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(DefaultContainerSize)
                                    .border(width = 4.rdp, color = RemoteColor.Red)
                                    .background(
                                        RemoteBrush.verticalGradient(
                                            listOf(RemoteColor.Blue, RemoteColor.Cyan)
                                        )
                                    )
                        )
                    },
            )
        )
    }
}
