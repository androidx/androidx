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
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.runtime.Composable
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
                                    .background(RemoteColor.White)
                                    .padding(15.rdp)
                                    .drawWithContent {
                                        val paint = RemotePaint().apply { color = RemoteColor.Blue }
                                        drawRect(paint = paint)
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(20.rdp).background(RemoteColor.Red)
                            )
                        }
                    },
                "drawWithContent then padding" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(70.rdp)
                                    .background(RemoteColor.White)
                                    .drawWithContent {
                                        val paint = RemotePaint().apply { color = RemoteColor.Blue }
                                        drawRect(paint = paint)
                                        drawContent()
                                    }
                                    .padding(15.rdp)
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(20.rdp).background(RemoteColor.Red)
                            )
                        }
                    },
                "asymmetric padding" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(70.rdp)
                                    .background(RemoteColor.White)
                                    .padding(
                                        start = 10.rdp,
                                        top = 20.rdp,
                                        end = 15.rdp,
                                        bottom = 5.rdp,
                                    )
                                    .drawWithContent {
                                        val paint = RemotePaint().apply { color = RemoteColor.Blue }
                                        drawRect(paint = paint)
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(20.rdp).background(RemoteColor.Red)
                            )
                        }
                    },
                "offset then padding" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(60.rdp)
                                    .background(RemoteColor.White)
                                    .offset(10.rdp, 10.rdp)
                                    .padding(12.rdp)
                                    .drawWithContent {
                                        val paint = RemotePaint().apply { color = RemoteColor.Blue }
                                        drawRect(paint = paint)
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(15.rdp).background(RemoteColor.Red)
                            )
                        }
                    },
                "rotate then padding" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(60.rdp)
                                    .background(RemoteColor.White)
                                    .rotate(30f.rf)
                                    .padding(12.rdp)
                                    .drawWithContent {
                                        val paint = RemotePaint().apply { color = RemoteColor.Blue }
                                        drawRect(paint = paint)
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(15.rdp).background(RemoteColor.Red)
                            )
                        }
                    },
                "scale then padding" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(60.rdp)
                                    .background(RemoteColor.White)
                                    .scale(0.8f.rf)
                                    .padding(12.rdp)
                                    .drawWithContent {
                                        val paint = RemotePaint().apply { color = RemoteColor.Blue }
                                        drawRect(paint = paint)
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(15.rdp).background(RemoteColor.Red)
                            )
                        }
                    },
                "canvas translate inside" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(70.rdp)
                                    .background(RemoteColor.White)
                                    .padding(15.rdp)
                                    .drawWithContent {
                                        val paint = RemotePaint().apply { color = RemoteColor.Blue }
                                        translate(8f.rf, 8f.rf) {
                                            drawRect(paint = paint)
                                        }
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(15.rdp).background(RemoteColor.Red)
                            )
                        }
                    },
                "canvas rotate inside" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(70.rdp)
                                    .background(RemoteColor.White)
                                    .padding(15.rdp)
                                    .drawWithContent {
                                        val paint = RemotePaint().apply { color = RemoteColor.Blue }
                                        rotate(45f.rf, pivot = RemoteOffset(20f.rf, 20f.rf)) {
                                            drawRect(paint = paint)
                                        }
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(15.rdp).background(RemoteColor.Red)
                            )
                        }
                    },
                "transformed child" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(70.rdp)
                                    .background(RemoteColor.White)
                                    .padding(15.rdp)
                                    .drawWithContent {
                                        val paint = RemotePaint().apply { color = RemoteColor.Blue }
                                        drawRect(paint = paint)
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier =
                                    RemoteModifier.offset(5.rdp, 5.rdp)
                                        .rotate(45f.rf)
                                        .size(15.rdp)
                                        .background(RemoteColor.Red)
                            )
                        }
                    },
                "chained paddings" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(70.rdp)
                                    .background(RemoteColor.White)
                                    .padding(8.rdp)
                                    .padding(8.rdp)
                                    .drawWithContent {
                                        val paint = RemotePaint().apply { color = RemoteColor.Blue }
                                        drawRect(paint = paint)
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(15.rdp).background(RemoteColor.Red)
                            )
                        }
                    },
                "chained offset + rotate" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(60.rdp)
                                    .background(RemoteColor.White)
                                    .offset(8.rdp, 8.rdp)
                                    .rotate(25f.rf)
                                    .padding(10.rdp)
                                    .drawWithContent {
                                        val paint = RemotePaint().apply { color = RemoteColor.Blue }
                                        drawRect(paint = paint)
                                        drawContent()
                                    }
                        ) {
                            RemoteBox(
                                modifier = RemoteModifier.size(15.rdp).background(RemoteColor.Red)
                            )
                        }
                    },
                "shapes inside content" to
                    @RemoteComposable @Composable {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(70.rdp)
                                    .background(RemoteColor.White)
                                    .padding(15.rdp)
                                    .drawWithContent {
                                        val paintBlue =
                                            RemotePaint().apply { color = RemoteColor.Blue }
                                        val paintYellow =
                                            RemotePaint().apply {
                                                color = RemoteColor.Yellow
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
                                modifier = RemoteModifier.size(10.rdp).background(RemoteColor.Red)
                            )
                        }
                    },
            )

        gridScreenshotUI.GridContent(tests)
    }

    @Test
    fun chainedDrawWithContent() = composeTestRule.runScreenshotTest {
        gridScreenshotUI.GridContent(chainedDrawWithContentTests())
    }
}

internal fun chainedDrawWithContentTests():
    List<Pair<String, @RemoteComposable @Composable () -> Unit>> =
    listOf(
        "chained pre + post" to @RemoteComposable @Composable { ChainedPreAndPostDraw() },
        "draw + pad + draw" to @RemoteComposable @Composable { DrawPaddingDraw() },
        "pad + draw + pad + draw" to @RemoteComposable @Composable { PaddingDrawPaddingDraw() },
        "triple chain" to @RemoteComposable @Composable { TripleChainedDrawWithContent() },
        "canvas chained" to @RemoteComposable @Composable { ChainedRemoteCanvas() },
        "card + caller overlay" to @RemoteComposable @Composable { CardWithCallerOverlay() },
    )

@Composable
@RemoteComposable
private fun ChainedPreAndPostDraw() {
    RemoteBox(
        modifier =
            RemoteModifier.size(70.rdp)
                .background(RemoteColor.White)
                .drawWithContent {
                    val paintBlue = RemotePaint { color = RemoteColor.Blue }
                    val paintYellow = RemotePaint { color = RemoteColor.Yellow }
                    drawRect(paint = paintBlue)
                    drawContent()
                    drawRect(
                        paint = paintYellow,
                        topLeft = RemoteOffset.Zero,
                        size = RemoteSize(width, 10f.rf),
                    )
                }
                .drawWithContent {
                    val paintCyan = RemotePaint { color = RemoteColor.Cyan }
                    val paintGreen = RemotePaint { color = RemoteColor.Green }
                    drawRect(
                        paint = paintCyan,
                        topLeft = RemoteOffset(12f.rf, 12f.rf),
                        size = RemoteSize(width - 24f.rf, height - 24f.rf),
                    )
                    drawContent()
                    drawRect(
                        paint = paintGreen,
                        topLeft = RemoteOffset(0f.rf, height - 10f.rf),
                        size = RemoteSize(width, 10f.rf),
                    )
                }
    ) {
        RemoteBox(
            modifier = RemoteModifier.padding(25.rdp).size(20.rdp).background(RemoteColor.Red)
        )
    }
}

@Composable
@RemoteComposable
private fun DrawPaddingDraw() {
    RemoteBox(
        modifier =
            RemoteModifier.size(70.rdp)
                .background(RemoteColor.White)
                .drawWithContent {
                    val paintBlue = RemotePaint { color = RemoteColor.Blue }
                    val paintYellow = RemotePaint { color = RemoteColor.Yellow }
                    drawRect(paint = paintBlue)
                    drawContent()
                    drawRect(
                        paint = paintYellow,
                        topLeft = RemoteOffset.Zero,
                        size = RemoteSize(width, 8f.rf),
                    )
                }
                .padding(14.rdp)
                .drawWithContent {
                    val paintCyan = RemotePaint { color = RemoteColor.Cyan }
                    drawRect(paint = paintCyan)
                    drawContent()
                }
    ) {
        RemoteBox(modifier = RemoteModifier.size(20.rdp).background(RemoteColor.Red))
    }
}

@Composable
@RemoteComposable
private fun PaddingDrawPaddingDraw() {
    RemoteBox(
        modifier =
            RemoteModifier.size(70.rdp)
                .background(RemoteColor.White)
                .padding(8.rdp)
                .drawWithContent {
                    val paintBlue = RemotePaint { color = RemoteColor.Blue }
                    drawRect(paint = paintBlue)
                    drawContent()
                }
                .padding(10.rdp)
                .drawWithContent {
                    val paintYellow = RemotePaint { color = RemoteColor.Yellow }
                    drawRect(paint = paintYellow)
                    drawContent()
                }
    ) {
        RemoteBox(modifier = RemoteModifier.size(16.rdp).background(RemoteColor.Red))
    }
}

@Composable
@RemoteComposable
private fun TripleChainedDrawWithContent() {
    RemoteBox(
        modifier =
            RemoteModifier.size(70.rdp)
                .background(RemoteColor.White)
                .drawWithContent {
                    val paintBlue = RemotePaint { color = RemoteColor.Blue }
                    val paintGreen = RemotePaint { color = RemoteColor.Green }
                    drawRect(paint = paintBlue)
                    drawContent()
                    drawRect(
                        paint = paintGreen,
                        topLeft = RemoteOffset.Zero,
                        size = RemoteSize(16f.rf, 16f.rf),
                    )
                }
                .padding(8.rdp)
                .drawWithContent {
                    val paintCyan = RemotePaint { color = RemoteColor.Cyan }
                    drawRect(paint = paintCyan)
                    drawContent()
                }
                .padding(8.rdp)
                .drawWithContent {
                    val paintYellow = RemotePaint { color = RemoteColor.Yellow }
                    drawRect(paint = paintYellow)
                    drawContent()
                }
    ) {
        RemoteBox(modifier = RemoteModifier.size(18.rdp).background(RemoteColor.Red))
    }
}

@Composable
@RemoteComposable
private fun ChainedRemoteCanvas() {
    RemoteCanvas(
        modifier =
            RemoteModifier.size(70.rdp)
                .drawWithContent {
                    val paintBlue = RemotePaint { color = RemoteColor.Blue }
                    val paintYellow = RemotePaint { color = RemoteColor.Yellow }
                    drawRect(paint = paintBlue)
                    drawContent()
                    drawRect(
                        paint = paintYellow,
                        topLeft = RemoteOffset.Zero,
                        size = RemoteSize(width, 10f.rf),
                    )
                }
                .drawWithContent {
                    val paintCyan = RemotePaint { color = RemoteColor.Cyan }
                    drawRect(
                        paint = paintCyan,
                        topLeft = RemoteOffset(14f.rf, 14f.rf),
                        size = RemoteSize(width - 28f.rf, height - 28f.rf),
                    )
                    drawContent()
                }
    ) {
        val paintRed = RemotePaint { color = RemoteColor.Red }
        drawCircle(paint = paintRed, radius = 14f.rf, center = center)
    }
}

@Composable
@RemoteComposable
private fun CardWithCallerOverlay() {
    SimulatedCard(
        modifier =
            RemoteModifier.drawWithContent {
                val paintGreen = RemotePaint { color = RemoteColor.Green }
                drawContent()
                drawCircle(
                    paint = paintGreen,
                    radius = 10f.rf,
                    center = RemoteOffset(width - 14f.rf, 14f.rf),
                )
            }
    ) {
        RemoteBox(modifier = RemoteModifier.size(20.rdp).background(RemoteColor.Red))
    }
}

@Composable
@RemoteComposable
private fun SimulatedCard(
    modifier: RemoteModifier = RemoteModifier,
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(
        modifier =
            modifier
                .size(70.rdp)
                .drawWithContent {
                    val paintBlue = RemotePaint { color = RemoteColor.Blue }
                    drawRoundRect(
                        paint = paintBlue,
                        cornerRadius = RemoteOffset(16f.rf, 16f.rf),
                    )
                    drawContent()
                }
                .padding(14.rdp),
        content = content,
    )
}
