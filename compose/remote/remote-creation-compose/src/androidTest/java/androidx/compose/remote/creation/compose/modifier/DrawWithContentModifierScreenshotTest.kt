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
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
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

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class DrawWithContentModifierScreenshotTest {
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
                "padding then drawWithContent" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(70.rdp)
                                    .background(Color.White.rc)
                                    .padding(15.rdp)
                                    .drawWithContent {
                                        val paint =
                                            RemotePaint().apply { color = RemoteColor(Color.Blue) }
                                        drawRect(paint = paint)
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(20.rdp).background(Color.Red.rc)
                            )
                        }
                    },
                "drawWithContent then padding" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(70.rdp)
                                    .background(Color.White.rc)
                                    .drawWithContent {
                                        val paint =
                                            RemotePaint().apply { color = RemoteColor(Color.Blue) }
                                        drawRect(paint = paint)
                                        drawContent()
                                    }
                                    .padding(15.rdp)
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(20.rdp).background(Color.Red.rc)
                            )
                        }
                    },
                "asymmetric padding" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(70.rdp)
                                    .background(Color.White.rc)
                                    .padding(
                                        start = 10.rdp,
                                        top = 20.rdp,
                                        end = 15.rdp,
                                        bottom = 5.rdp,
                                    )
                                    .drawWithContent {
                                        val paint =
                                            RemotePaint().apply { color = RemoteColor(Color.Blue) }
                                        drawRect(paint = paint)
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(20.rdp).background(Color.Red.rc)
                            )
                        }
                    },
                "offset then padding" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(60.rdp)
                                    .background(Color.White.rc)
                                    .offset(10.rdp, 10.rdp)
                                    .padding(12.rdp)
                                    .drawWithContent {
                                        val paint =
                                            RemotePaint().apply { color = RemoteColor(Color.Blue) }
                                        drawRect(paint = paint)
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(15.rdp).background(Color.Red.rc)
                            )
                        }
                    },
                "rotate then padding" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(60.rdp)
                                    .background(Color.White.rc)
                                    .rotate(30f.rf)
                                    .padding(12.rdp)
                                    .drawWithContent {
                                        val paint =
                                            RemotePaint().apply { color = RemoteColor(Color.Blue) }
                                        drawRect(paint = paint)
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(15.rdp).background(Color.Red.rc)
                            )
                        }
                    },
                "scale then padding" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(60.rdp)
                                    .background(Color.White.rc)
                                    .scale(0.8f.rf)
                                    .padding(12.rdp)
                                    .drawWithContent {
                                        val paint =
                                            RemotePaint().apply { color = RemoteColor(Color.Blue) }
                                        drawRect(paint = paint)
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(15.rdp).background(Color.Red.rc)
                            )
                        }
                    },
                "canvas translate inside" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(70.rdp)
                                    .background(Color.White.rc)
                                    .padding(15.rdp)
                                    .drawWithContent {
                                        val paint =
                                            RemotePaint().apply { color = RemoteColor(Color.Blue) }
                                        translate(8f.rf, 8f.rf) {
                                            drawRect(paint = paint)
                                        }
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(15.rdp).background(Color.Red.rc)
                            )
                        }
                    },
                "canvas rotate inside" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(70.rdp)
                                    .background(Color.White.rc)
                                    .padding(15.rdp)
                                    .drawWithContent {
                                        val paint =
                                            RemotePaint().apply { color = RemoteColor(Color.Blue) }
                                        rotate(45f.rf, pivot = RemoteOffset(20f.rf, 20f.rf)) {
                                            drawRect(paint = paint)
                                        }
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(15.rdp).background(Color.Red.rc)
                            )
                        }
                    },
                "transformed child" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(70.rdp)
                                    .background(Color.White.rc)
                                    .padding(15.rdp)
                                    .drawWithContent {
                                        val paint =
                                            RemotePaint().apply { color = RemoteColor(Color.Blue) }
                                        drawRect(paint = paint)
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier =
                                    RemoteModifier.offset(5.rdp, 5.rdp)
                                        .rotate(45f.rf)
                                        .size(15.rdp)
                                        .background(Color.Red.rc)
                            )
                        }
                    },
                "chained paddings" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(70.rdp)
                                    .background(Color.White.rc)
                                    .padding(8.rdp)
                                    .padding(8.rdp)
                                    .drawWithContent {
                                        val paint =
                                            RemotePaint().apply { color = RemoteColor(Color.Blue) }
                                        drawRect(paint = paint)
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(15.rdp).background(Color.Red.rc)
                            )
                        }
                    },
                "chained offset + rotate" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(60.rdp)
                                    .background(Color.White.rc)
                                    .offset(8.rdp, 8.rdp)
                                    .rotate(25f.rf)
                                    .padding(10.rdp)
                                    .drawWithContent {
                                        val paint =
                                            RemotePaint().apply { color = RemoteColor(Color.Blue) }
                                        drawRect(paint = paint)
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(15.rdp).background(Color.Red.rc)
                            )
                        }
                    },
                "shapes inside content" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(70.rdp)
                                    .background(Color.White.rc)
                                    .padding(15.rdp)
                                    .drawWithContent {
                                        val paintBlue =
                                            RemotePaint().apply { color = RemoteColor(Color.Blue) }
                                        val paintYellow =
                                            RemotePaint().apply {
                                                color = RemoteColor(Color.Yellow)
                                            }
                                        drawRoundRect(
                                            paint = paintBlue,
                                            cornerRadius = RemoteOffset(6f.rf, 6f.rf),
                                        )
                                        drawCircle(
                                            paint = paintYellow,
                                            radius = 8f.rf,
                                            center = center,
                                        )
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(10.rdp).background(Color.Red.rc)
                            )
                        }
                    },
            )

        gridScreenshotUI.GridContent(tests)
    }
}
