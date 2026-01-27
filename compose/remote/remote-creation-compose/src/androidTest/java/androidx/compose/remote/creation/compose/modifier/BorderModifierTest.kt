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
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRectangleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI.Companion.DefaultContainerSize
import androidx.compose.remote.player.compose.test.utils.screenshot.TargetPlayer
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(TestParameterInjector::class)
class BorderModifierTest {
    @TestParameter private lateinit var targetPlayer: TargetPlayer

    @get:Rule
    val composeTestRule: RemoteComposeScreenshotTestRule by lazy {
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            targetPlayer = targetPlayer,
        )
    }

    private val gridScreenshotUI = GridScreenshotUI()

    @Test
    fun grid() =
        composeTestRule.runScreenshotTest {
            val borders =
                listOf<@Composable RemoteModifier.() -> RemoteModifier>(
                    { this },
                    { border(1.rdp, Color.Red.rc) },
                    { border(2.rdp, Color.Red.rc) },
                    { border(3.rdp, Color.Red.rc) },
                    { border(3.rdp, Color.Blue.rc.copy(alpha = 0.5f.rf)) },
                    // Ensure a non constant color
                    { border(3.rdp, Color.Blue.rc.copy(alpha = 0.5f.rf.createReference())) },
                    { border(width = 2.rdp, color = Color.Green.rc, shape = RemoteRectangleShape) },
                    { border(width = 2.rdp, color = Color.Magenta.rc, shape = RemoteCircleShape) },
                    {
                        border(
                            width = 2.rdp,
                            color = Color.Cyan.rc,
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
                        for (borderFn in borders) {
                            yield(
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
}
