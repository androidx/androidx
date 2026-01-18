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

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI.Companion.DefaultContainerSize
import androidx.compose.remote.player.compose.test.utils.screenshot.TargetPlayer
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
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
class ClipModifierTest {
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
            val clips =
                listOf<@Composable RemoteModifier.() -> RemoteModifier>(
                    { this },
                    { clip(RectangleShape) },
                    { clip(CircleShape) },
                    { clip(CircleShape, DpSize(44.dp, 32.dp)) },
                    { clip(RoundedCornerShape(size = 10.dp)) },
                    { clip(RoundedCornerShape(percent = 25)) },
                    { clip(RoundedCornerShape(percent = 50)) },
                    { clip(RoundedCornerShape(topStart = 10.dp, bottomEnd = 10.dp)) },
                )

            gridScreenshotUI.GridContent(
                sequence {
                        for (clipFn in clips) {
                            yield(
                                @RemoteComposable @Composable {
                                    RemoteBox {
                                        RemoteBox(
                                            modifier =
                                                RemoteModifier.size(DefaultContainerSize)
                                                    .clipFn()
                                                    .background(Color.Red)
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
