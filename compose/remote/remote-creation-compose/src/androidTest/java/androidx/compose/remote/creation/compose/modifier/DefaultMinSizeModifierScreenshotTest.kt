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
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screenshot tests for [RemoteModifier.defaultMinSize].
 *
 * Visual layout setup:
 * - Outer container under test: [RemoteBox] with red background ([Color.Red]), sized by modifiers
 *   and wrapping content.
 * - Inner child: [RemoteBox] with blue background ([Color.Blue]) and fixed dimensions, serving as
 *   the controlled content load.
 *
 * The inner blue child is essential because without content, a wrap-content container measures to
 * 0x0. Placing an inner child of known dimensions tests both sides of the inequality:
 * - When child < min: the outer red container clamps up to the default minimum size, leaving red
 *   space around the blue child.
 * - When child > min: the outer red container expands to wrap the child, covered by the blue child.
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class DefaultMinSizeModifierScreenshotTest {
    @get:Rule
    val composeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    private val gridScreenshotUI = GridScreenshotUI()

    @Test
    fun grid() = composeTestRule.runScreenshotTest {
        val tests =
            listOf<Pair<String, @RemoteComposable @Composable () -> Unit>>(
                // 1. Content sizing vs default minimum (unconstrained outer container)
                "child 20x20 < min 50\n-> clamps to 50x50" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.defaultMinSize(
                                        minWidth = 50.rdp,
                                        minHeight = 50.rdp,
                                    )
                                    .background(Color.Red.rc)
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(20.rdp).background(Color.Blue.rc)
                            )
                        }
                    },
                "child 80x80 > min 50\n-> wraps to 80x80" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.defaultMinSize(
                                        minWidth = 50.rdp,
                                        minHeight = 50.rdp,
                                    )
                                    .background(Color.Red.rc)
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(80.rdp).background(Color.Blue.rc)
                            )
                        }
                    },

                // 2. Explicit size modifier BEFORE defaultMinSize (creation time override)
                "size(80) before min(50)\n-> explicit 80x80 wins" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(80.rdp)
                                    .defaultMinSize(minWidth = 50.rdp, minHeight = 50.rdp)
                                    .background(Color.Red.rc)
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(20.rdp).background(Color.Blue.rc)
                            )
                        }
                    },
                "size(20) before min(50)\n-> explicit 20x20 wins" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(20.rdp)
                                    .defaultMinSize(minWidth = 50.rdp, minHeight = 50.rdp)
                                    .background(Color.Red.rc)
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(10.rdp).background(Color.Blue.rc)
                            )
                        }
                    },
                "width(20) before min(50)\n-> 20x50 (width fixed)" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.width(20.rdp)
                                    .defaultMinSize(minWidth = 50.rdp, minHeight = 50.rdp)
                                    .background(Color.Red.rc)
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(10.rdp).background(Color.Blue.rc)
                            )
                        }
                    },
                "height(20) before min(50)\n-> 50x20 (height fixed)" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.height(20.rdp)
                                    .defaultMinSize(minWidth = 50.rdp, minHeight = 50.rdp)
                                    .background(Color.Red.rc)
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(10.rdp).background(Color.Blue.rc)
                            )
                        }
                    },

                // 3. Explicit size modifier AFTER defaultMinSize (player time interaction)
                "min(50) then size(80)\n-> player renders 80x80" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.defaultMinSize(
                                        minWidth = 50.rdp,
                                        minHeight = 50.rdp,
                                    )
                                    .size(80.rdp)
                                    .background(Color.Red.rc)
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(20.rdp).background(Color.Blue.rc)
                            )
                        }
                    },
                "min(50) then size(20)\n-> player clamps 50x50" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.defaultMinSize(
                                        minWidth = 50.rdp,
                                        minHeight = 50.rdp,
                                    )
                                    .size(20.rdp)
                                    .background(Color.Red.rc)
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(10.rdp).background(Color.Blue.rc)
                            )
                        }
                    },

                // 4. Stacked / multiple defaultMinSize modifiers
                "min(40) then min(70)\n-> 1st modifier (40) wins" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.defaultMinSize(
                                        minWidth = 40.rdp,
                                        minHeight = 40.rdp,
                                    )
                                    .defaultMinSize(minWidth = 70.rdp, minHeight = 70.rdp)
                                    .background(Color.Red.rc)
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(10.rdp).background(Color.Blue.rc)
                            )
                        }
                    },
                "min(40) pad min(70)\n-> 40x40 (1st min wins)" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.defaultMinSize(
                                        minWidth = 40.rdp,
                                        minHeight = 40.rdp,
                                    )
                                    .background(Color.Gray.rc)
                                    .padding(10.rdp)
                                    .defaultMinSize(minWidth = 70.rdp, minHeight = 70.rdp)
                                    .background(Color.Red.rc)
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(10.rdp).background(Color.Blue.rc)
                            )
                        }
                    },

                // 5. Independent single-axis minimum constraints
                "minWidth(50) only\n-> 50x20 (wraps height)" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.defaultMinSize(minWidth = 50.rdp)
                                    .background(Color.Red.rc)
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(20.rdp).background(Color.Blue.rc)
                            )
                        }
                    },
                "minHeight(50) only\n-> 20x50 (wraps width)" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.defaultMinSize(minHeight = 50.rdp)
                                    .background(Color.Red.rc)
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(20.rdp).background(Color.Blue.rc)
                            )
                        }
                    },
            )

        gridScreenshotUI.GridContent(tests)
    }
}
