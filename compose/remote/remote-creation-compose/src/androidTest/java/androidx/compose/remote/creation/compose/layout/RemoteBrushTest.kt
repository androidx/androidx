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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.creation.compose.layout

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.painter.RemotePainter
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.bitmap
import androidx.compose.remote.creation.compose.shaders.horizontalGradient
import androidx.compose.remote.creation.compose.shaders.linearGradient
import androidx.compose.remote.creation.compose.shaders.radialGradient
import androidx.compose.remote.creation.compose.shaders.sweepGradient
import androidx.compose.remote.creation.compose.shaders.verticalGradient
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteState
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteBitmap
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
class RemoteBrushTest {
    @get:Rule
    val remoteComposeTestRule: RemoteComposeScreenshotTestRule by lazy {
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            matcher = MSSIMMatcher(threshold = 0.9995),
        )
    }

    private val colors = listOf(Color.Red.rc, Color.Blue.rc)
    private val dynamicColors =
        listOf(
            // Force non constant values with createReference()
            RemoteColor(
                alpha = 1f.rf.createReference(),
                red = 1f.rf,
                green = 0f.rf,
                blue = 0f.rf,
            ), // Red
            RemoteColor(
                alpha = 1f.rf.createReference(),
                red = 0f.rf,
                green = 0f.rf,
                blue = 1f.rf,
            ), // Blue
        )

    private val tests =
        listOf<Pair<String, @Composable @RemoteComposable () -> Unit>>(
            "LinearGradient_Infinite" to
                {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxSize()
                                .background(
                                    RemoteBrush.linearGradient(
                                        colors = colors,
                                        start = RemoteOffset(0f.rf, 0f.rf),
                                        end = RemoteOffset(Offset.Infinite),
                                    )
                                )
                    )
                },
            "HorizontalGradient_Infinite" to
                {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxSize()
                                .background(
                                    RemoteBrush.horizontalGradient(
                                        colors = colors,
                                        startX = 0f.rf,
                                        endX = Float.POSITIVE_INFINITY.rf,
                                    )
                                )
                    )
                },
            "VerticalGradient_Infinite" to
                {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxSize()
                                .background(
                                    RemoteBrush.verticalGradient(
                                        colors = colors,
                                        startY = 0f.rf,
                                        endY = Float.POSITIVE_INFINITY.rf,
                                    )
                                )
                    )
                },
            "RadialGradient_Infinite" to
                {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxSize()
                                .background(
                                    RemoteBrush.radialGradient(
                                        colors = colors,
                                        center = RemoteOffset(Offset.Infinite),
                                        radius = Float.POSITIVE_INFINITY.rf,
                                    )
                                )
                    )
                },
            "SweepGradient_Infinite" to
                {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxSize()
                                .background(
                                    RemoteBrush.sweepGradient(
                                        colors = colors,
                                        center = RemoteOffset(Offset.Infinite),
                                    )
                                )
                    )
                },
            "LinearGradient_DynamicColors" to
                {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxSize()
                                .background(RemoteBrush.linearGradient(colors = dynamicColors))
                    )
                },
            "RadialGradient_DynamicColors" to
                {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxSize()
                                .background(RemoteBrush.radialGradient(colors = dynamicColors))
                    )
                },
            "SweepGradient_DynamicColors" to
                {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxSize()
                                .background(RemoteBrush.sweepGradient(colors = dynamicColors))
                    )
                },
        )

    @Test
    fun test() {
        val chunkedTests = tests.chunked(3)
        remoteComposeTestRule.runScreenshotTest {
            RemoteColumn {
                for (testRow in chunkedTests) {
                    RemoteRow {
                        for (testItem in testRow) {
                            Container { testItem.second() }
                            RemoteBox(modifier = RemoteModifier.width(Padding))
                        }
                    }
                    RemoteBox(modifier = RemoteModifier.height(Padding))
                }
            }
        }
    }

    @Test
    fun bitmapBrushTest() {
        remoteComposeTestRule.runScreenshotTest {
            val image =
                rememberNamedRemoteBitmap(name = "background", domain = RemoteState.Domain.User) {
                    createImage(400, 400).asImageBitmap()
                }
            val imageSize = RemoteSize(image.width, image.height)
            val topLeftX = 50f
            val topLeftY = 50f
            val matrix33 =
                RemoteMatrix3x3.createTranslateXY(RemoteFloat(topLeftX), RemoteFloat(topLeftY))
            RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.Yellow)) {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp)
                            .background(
                                object : RemotePainter() {
                                    override fun RemoteDrawScope.onDraw() {
                                        val paint =
                                            RemotePaint().apply {
                                                applyRemoteBrush(
                                                    RemoteBrush.bitmap(image),
                                                    remoteSize,
                                                    matrix33,
                                                )
                                            }
                                        this.drawRoundRect(
                                            paint,
                                            RemoteOffset(topLeftX.rf, topLeftY.rf),
                                            imageSize,
                                            RemoteOffset(50.rf, 50.rf),
                                        )
                                    }

                                    override val intrinsicSize: RemoteSize
                                        get() = RemoteSize(image.width, image.height)
                                }
                            )
                )
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
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
            verticalArrangement = RemoteArrangement.Center,
            content = content,
        )
    }

    private companion object {
        val Padding = 24.rdp
        val ContainerSize = 100.rdp
        val ContainerColor = Color(0xFFCFD8DC.toInt()).rc

        // Draws a red cross with a cyan/grey/blue background
        fun createImage(tw: Int, th: Int): Bitmap {
            val image = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
            val paint = Paint()
            val canvas = Canvas(image)
            canvas.drawPaint(
                Paint().apply {
                    shader =
                        RadialGradient(
                            tw * 0.5f,
                            th * 0.5f,
                            tw * 0.9f,
                            intArrayOf(
                                android.graphics.Color.CYAN,
                                android.graphics.Color.LTGRAY,
                                android.graphics.Color.BLUE,
                            ),
                            floatArrayOf(0f, 0.5f, 1f),
                            Shader.TileMode.CLAMP,
                        )
                }
            )
            paint.strokeWidth = 3f
            paint.isAntiAlias = true
            paint.setColor(android.graphics.Color.RED)
            canvas.drawLine(0f, 0f, tw.toFloat(), th.toFloat(), paint)
            canvas.drawLine(0f, th.toFloat(), tw.toFloat(), 0f, paint)
            return image
        }
    }
}
