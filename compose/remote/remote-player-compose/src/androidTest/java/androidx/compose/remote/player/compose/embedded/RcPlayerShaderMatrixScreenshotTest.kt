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

package androidx.compose.remote.player.compose.embedded

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.image
import androidx.compose.remote.creation.compose.shaders.linearGradient
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3.Companion.createRotationAround
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteTileMode
import androidx.compose.remote.creation.compose.state.createNamedRemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.GridScreenshotUI
import androidx.compose.remote.player.compose.test.utils.RemoteEmbeddedScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screenshot tests for `MatrixExpression` and `MatrixConstant` shader matrices rendered in
 * [RcPlayer].
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RcPlayerShaderMatrixScreenshotTest {
    @get:Rule
    val screenshotRule =
        RemoteEmbeddedScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    private val gridScreenshotUI = GridScreenshotUI()

    @Test
    fun shaderMatrices() {
        screenshotRule.runScreenshotTest(
            playComposableWrapper = ComposableWrappers.blackBackground
        ) {
            val tests =
                listOf<Pair<String, @RemoteComposable @Composable () -> Unit>>(
                    "ContentScale.Crop" to
                        @RemoteComposable @Composable { ImageBrushCell(ContentScale.Crop) },
                    "ContentScale.Fit" to
                        @RemoteComposable @Composable { ImageBrushCell(ContentScale.Fit) },
                    "ContentScale.FillBounds" to
                        @RemoteComposable @Composable { ImageBrushCell(ContentScale.FillBounds) },
                    "Crop + 0.35 alpha" to @RemoteComposable @Composable { DimmedCropBrushCell() },
                    "MatrixExpr rot 45" to
                        @RemoteComposable @Composable { RotatedGradientMatrixCell() },
                    "MatrixConst tile" to
                        @RemoteComposable @Composable { MatrixConstantTileCell() },
                )
            gridScreenshotUI.GridContent(tests)
        }
    }

    @Composable
    @RemoteComposable
    private fun ImageBrushCell(contentScale: ContentScale) {
        val banner = remember {
            createNamedRemoteImageBitmap("banner_${contentScale.GetLabel()}") {
                createTargetBannerBitmap().asImageBitmap()
            }
        }
        val brush =
            RemoteBrush.image(
                image = banner,
                tileModeX = TileMode.Decal,
                tileModeY = TileMode.Decal,
                contentScale = contentScale,
            )
        RemoteBox(
            modifier = RemoteModifier.size(100.rdp).background(Color(0xFF263238).rc),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteBox(modifier = RemoteModifier.size(80.rdp).background(brush))
        }
    }

    @Composable
    @RemoteComposable
    private fun DimmedCropBrushCell() {
        val banner = remember {
            createNamedRemoteImageBitmap("banner_dimmed_crop") {
                createTargetBannerBitmap().asImageBitmap()
            }
        }
        RemoteBox(
            modifier = RemoteModifier.size(100.rdp).background(Color.Black.rc),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.size(80.rdp).background(Color.Black.rc).drawWithContent {
                        val imgPaint = RemotePaint {
                            with(
                                RemoteBrush.image(
                                    image = banner,
                                    tileModeX = TileMode.Decal,
                                    tileModeY = TileMode.Decal,
                                    contentScale = ContentScale.Crop,
                                )
                            ) {
                                applyTo(this@RemotePaint, size)
                            }
                            color = color.copy(alpha = 0.35f.rf)
                        }
                        drawRect(paint = imgPaint, topLeft = RemoteOffset.Zero, size = size)
                        drawContent()
                    }
            )
        }
    }

    @Composable
    @RemoteComposable
    private fun RotatedGradientMatrixCell() {
        RemoteBox(
            modifier = RemoteModifier.size(100.rdp).background(Color(0xFF263238).rc),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteCanvas(modifier = RemoteModifier.size(80.rdp)) {
                val gradient =
                    RemoteBrush.linearGradient(
                        colors = listOf(Color.Red.rc, Color.Yellow.rc, Color.Blue.rc),
                        start = RemoteOffset.Zero,
                        end = RemoteOffset(width, 0f.rf),
                    )
                val matrix = createRotationAround(45f.rf, width / 2f.rf, height / 2f.rf)
                val paint = RemotePaint()
                with(gradient) { applyTo(paint, size, matrix) }
                drawRect(paint = paint)
            }
        }
    }

    @Composable
    @RemoteComposable
    private fun MatrixConstantTileCell() {
        RemoteBox(
            modifier = RemoteModifier.size(100.rdp).background(Color(0xFF263238).rc),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.size(80.rdp).clip().drawWithContent {
                        val writer = document as RemoteComposeWriterAndroid
                        val tileBitmap = createQuadrantTileBitmap()
                        val bitmapId = writer.addBitmap(tileBitmap)
                        val matrixId =
                            writer.addMatrixConst(
                                Matrix().apply {
                                    setScale(0.75f, 0.75f)
                                    postRotate(30f)
                                }
                            )
                        writer.rcPaint
                            .setColor(Color.White.toArgb())
                            .setTextureShader(
                                bitmapId,
                                RemoteTileMode.Repeated.value.toShort(),
                                RemoteTileMode.Repeated.value.toShort(),
                                Rc.Texture.FILTER_DEFAULT,
                                0,
                            )
                            .setShaderMatrix(matrixId)
                            .commit()
                        document.drawRect(0f, 0f, 300f, 300f)
                    }
            )
        }
    }

    private companion object {
        fun ContentScale.GetLabel(): String =
            when (this) {
                ContentScale.Crop -> "crop"
                ContentScale.Fit -> "fit"
                ContentScale.FillBounds -> "fill_bounds"
                else -> "other"
            }

        /**
         * Creates a 240x120 banner with Blue left wing (60px), White center square (120x120) with a
         * centered Red circle, and Green right wing (60px).
         */
        fun createTargetBannerBitmap(): Bitmap {
            val width = 240
            val height = 120
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            paint.color = AndroidColor.BLUE
            canvas.drawRect(0f, 0f, 60f, height.toFloat(), paint)

            paint.color = AndroidColor.WHITE
            canvas.drawRect(60f, 0f, 180f, height.toFloat(), paint)

            paint.color = AndroidColor.GREEN
            canvas.drawRect(180f, 0f, width.toFloat(), height.toFloat(), paint)

            paint.color = AndroidColor.RED
            canvas.drawCircle(120f, 60f, 38f, paint)
            return bitmap
        }

        /** Creates an 80x80 2x2 quadrant bitmap (Red, Yellow, Cyan, Blue) for tiled shader test. */
        fun createQuadrantTileBitmap(): Bitmap {
            val bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint()

            paint.color = AndroidColor.RED
            canvas.drawRect(0f, 0f, 40f, 40f, paint)

            paint.color = AndroidColor.YELLOW
            canvas.drawRect(40f, 0f, 80f, 40f, paint)

            paint.color = AndroidColor.CYAN
            canvas.drawRect(0f, 40f, 40f, 80f, paint)

            paint.color = AndroidColor.BLUE
            canvas.drawRect(40f, 40f, 80f, 80f, paint)
            return bitmap
        }
    }
}
