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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.solidColor
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
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
class RemoteCanvasTest {
    @get:Rule
    val remoteComposeTestRule: RemoteComposeScreenshotTestRule by lazy {
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            matcher = MSSIMMatcher(threshold = 0.9995),
        )
    }

    private val tests =
        listOf<@Composable () -> Unit>(
            ::TestDrawAnchoredText_colorAndTextSize,
            ::TestDrawAnchoredText_brushAndTextSize,
            ::TestDrawAnchoredText_colorExpression,
            ::TestClipRect_intersect,
            ::TestClipRect_difference,
            ::TestDrawPrimitives,
            ::TestRotate,
        )

    @Test
    fun test() {
        val chunkedTests = tests.chunked(3)
        remoteComposeTestRule.runScreenshotTest {
            RemoteColumn {
                for (testRow in chunkedTests) {
                    RemoteRow {
                        for (testItem in testRow) {
                            Container { testItem() }
                            RemoteBox(modifier = RemoteModifier.width(Padding))
                        }
                    }
                    RemoteBox(modifier = RemoteModifier.height(Padding))
                }
            }
        }
    }

    @RemoteComposable
    @Composable
    fun TestDrawAnchoredText_colorAndTextSize() {
        val text = "Hello".rs
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
            val w = width
            drawAnchoredText(
                text = text,
                anchorX = w / 2f,
                anchorY = 40f.rf,
                paint =
                    RemotePaint {
                        color = Color.Red.rc
                        textSize = SMALL_FONT_SIZE.rf
                    },
            )
            drawAnchoredText(
                text = text,
                anchorX = w / 2f,
                anchorY = 80f.rf,
                paint =
                    RemotePaint {
                        color = Color.Green.rc
                        textSize = MEDIUM_FONT_SIZE.rf
                    },
            )
            drawAnchoredText(
                text = text,
                anchorX = w / 2f,
                anchorY = 120f.rf,
                paint =
                    RemotePaint {
                        color = Color.Blue.rc
                        textSize = LARGE_FONT_SIZE.rf
                    },
            )
        }
    }

    @RemoteComposable
    @Composable
    fun TestDrawPrimitives() {
        RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
            val paint = RemotePaint { color = Color.Red.rc }
            drawRect(paint = paint)
            drawCircle(
                paint = RemotePaint { color = Color.Blue.rc },
                center = RemoteOffset(50f.rf, 50f.rf),
                radius = 40f.rf,
            )
            drawArc(
                paint = RemotePaint { color = Color.Green.rc },
                startAngle = 0f.rf,
                sweepAngle = 90f.rf,
                useCenter = true,
                topLeft = RemoteOffset(10f.rf, 10f.rf),
                size = RemoteSize(80f.rf, 80f.rf),
            )
            drawLine(
                paint =
                    RemotePaint {
                        color = Color.Yellow.rc
                        strokeWidth = 5f.rf
                    },
                start = RemoteOffset(0f.rf, 0f.rf),
                end = RemoteOffset(100f.rf, 100f.rf),
            )
        }
    }

    @RemoteComposable
    @Composable
    fun TestRotate() {
        RemoteCanvas(
            modifier =
                RemoteModifier.size(100.rdp).drawWithContent {
                    rotate(45.rf, pivot = RemoteOffset(width / 2f, height / 2f)) { drawContent() }
                }
        ) {
            val w = width
            drawAnchoredText(
                text = "Rotated by Canvas 45°".rs,
                anchorX = 150f.rf,
                anchorY = 80f.rf,
                panX = 1f.rf,
                paint = RemotePaint(),
            )
            rotate((-45).rf) {
                drawAnchoredText(
                    text = "Rotated -45° then by canvas".rs,
                    anchorX = 10f.rf,
                    anchorY = 100f.rf,
                    panX = 1f.rf,
                    paint = RemotePaint(),
                )
            }
        }
    }

    @RemoteComposable
    @Composable
    fun TestDrawAnchoredText_brushAndTextSize() {
        val text = RemoteString("Hello")
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
            val w = width
            drawAnchoredText(
                text = text,
                anchorX = w / 2f,
                anchorY = 40f.rf,
                paint =
                    RemotePaint {
                        applyRemoteBrush(RemoteBrush.solidColor(Color.Red.rc), size)
                        textSize = SMALL_FONT_SIZE.rf
                    },
            )
            drawAnchoredText(
                text = text,
                anchorX = w / 2f,
                anchorY = 80f.rf,
                paint =
                    RemotePaint {
                        applyRemoteBrush(RemoteBrush.solidColor(Color.Green.rc), size)
                        textSize = MEDIUM_FONT_SIZE.rf
                    },
            )
            drawAnchoredText(
                text = text,
                anchorX = w / 2f,
                anchorY = 120f.rf,
                paint =
                    RemotePaint {
                        applyRemoteBrush(RemoteBrush.solidColor(Color.Blue.rc), size)
                        textSize = LARGE_FONT_SIZE.rf
                    },
            )
        }
    }

    @RemoteComposable
    @Composable
    fun TestDrawAnchoredText_colorExpression() {
        val textColor =
            RemoteColor.rgb(
                red = 0.8f.rf,
                green = 0.9f.rf,
                blue = 0.9f.rf,
                // Force a non const expression with createReference
                alpha = 0.9f.rf.createReference(),
            )
        val text = "Visible Hello".rs
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
            val w = width
            drawAnchoredText(
                text = text,
                anchorX = w / 2f,
                anchorY = 40f.rf,
                paint =
                    RemotePaint {
                        color = textColor
                        textSize = SMALL_FONT_SIZE.rf
                    },
            )
        }
    }

    @RemoteComposable
    @Composable
    fun TestClipRect_intersect() {
        ClipRectTest(clipOp = ClipOp.Intersect)
    }

    @RemoteComposable
    @Composable
    fun TestClipRect_difference() {
        // It generates the same output as Intersect: b/464257438
        ClipRectTest(clipOp = ClipOp.Difference)
    }

    @RemoteComposable
    @Composable
    private fun ClipRectTest(clipOp: ClipOp) {
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
            val clipRect1Left = 20f.rf
            val clipRect1Top = 20f.rf
            val clipRect1Right = 60f.rf
            val clipRect1Bottom = 60f.rf

            val clipRect2Left = 40f.rf
            val clipRect2Top = 40f.rf
            val clipRect2Right = 80f.rf
            val clipRect2Bottom = 80f.rf

            clipRect(
                left = clipRect1Left,
                top = clipRect1Top,
                right = clipRect1Right,
                bottom = clipRect1Bottom,
            ) {
                clipRect(
                    left = clipRect2Left,
                    top = clipRect2Top,
                    right = clipRect2Right,
                    bottom = clipRect2Bottom,
                    clipOp = clipOp,
                ) {
                    drawRect(paint = RemotePaint { color = Color.Red.rc })
                }
            }
        }
    }

    @Composable
    @RemoteComposable
    private fun Container(
        modifier: RemoteModifier = RemoteModifier,
        content: @Composable () -> Unit,
    ) {
        RemoteBox(
            modifier = modifier.size(ContainerSize).background(ContainerColor),
            contentAlignment = RemoteAlignment.Center,
            content = content,
        )
    }

    private companion object {
        val Padding = 24.rdp
        val ContainerSize = 100.rdp
        val ContainerColor = Color(0xFFCFD8DC.toInt()).rc
        const val SMALL_FONT_SIZE = 16f
        const val MEDIUM_FONT_SIZE = 32f
        const val LARGE_FONT_SIZE = 48f
    }
}
