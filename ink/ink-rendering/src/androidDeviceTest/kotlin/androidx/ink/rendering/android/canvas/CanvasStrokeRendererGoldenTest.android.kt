/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.rendering.android.canvas

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.graphics.withMatrix
import androidx.ink.brush.ExperimentalInkAnimationApi
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.TextureBitmapStore
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.toMatrix
import androidx.ink.rendering.test.AbstractStrokeRendererTest
import androidx.ink.rendering.test.R
import androidx.ink.storage.decode
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInputBatch
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.rules.ErrorCollector

@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@OptIn(ExperimentalInkCustomBrushApi::class, ExperimentalInkAnimationApi::class)
class CanvasStrokeRendererGoldenTest : AbstractStrokeRendererTest() {

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @get:Rule val errorCollector = ErrorCollector()

    override fun loadCursiveHelloInputs(): ImmutableStrokeInputBatch {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        return appContext.resources.openRawResource(R.raw.cursive_stylus_inputbatch).use {
            StrokeInputBatch.decode(it)
        }
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val textureStore = TextureBitmapStore { id ->
        when (id) {
            TEXTURE_ID_CHECKERBOARD -> R.drawable.checkerboard
            TEXTURE_ID_CIRCLE -> R.drawable.circle
            TEXTURE_ID_AIRPLANE_EMOJI -> R.drawable.airplane_emoji
            TEXTURE_ID_POOP_EMOJI -> R.drawable.poop_emoji
            TEXTURE_ID_NUMBERS_32X32_60FRAMES -> R.drawable.numbers_32x32_60frames
            else -> null
        }?.let { BitmapFactory.decodeResource(context.resources, it) }
    }
    private val renderer = CanvasStrokeRenderer.create(textureStore)

    override fun renderAndCompareToGolden(
        stroke: Stroke,
        transform: AffineTransform,
        imageWidth: Int,
        imageHeight: Int,
        goldenName: String,
        animatorClockStateMillis: Long,
    ) {
        val matrix = transform.toMatrix()
        val bitmap =
            ImageDiffer.createBitmap(imageWidth, imageHeight, DEFAULT_BACKGROUND_COLOR) { canvas ->
                canvas.withMatrix(matrix) {
                    renderer.draw(canvas, stroke, matrix, animatorClockStateMillis)
                }
            }
        ImageDiffer.diffBitmapWithGolden(
            screenshotRule,
            errorCollector,
            this::class.simpleName,
            bitmap,
            goldenName,
        )
    }

    override fun renderAndCompareToGolden(
        inProgressStroke: InProgressStroke,
        transform: AffineTransform,
        imageWidth: Int,
        imageHeight: Int,
        goldenName: String,
        animatorClockStateMillis: Long,
    ) {
        val matrix = transform.toMatrix()
        val bitmap =
            ImageDiffer.createBitmap(imageWidth, imageHeight, DEFAULT_BACKGROUND_COLOR) { canvas ->
                canvas.withMatrix(matrix) {
                    renderer.draw(canvas, inProgressStroke, matrix, animatorClockStateMillis)
                }
            }
        ImageDiffer.diffBitmapWithGolden(
            screenshotRule,
            errorCollector,
            this::class.simpleName,
            bitmap,
            goldenName,
        )
    }

    override fun isSupported(testCase: TestCase): Boolean =
        Build.VERSION.SDK_INT >= testCase.minAndroidSdk

    override fun assertLazyAssertsPass() {
        // Lazy image diffing is supported only in the upstream version.
    }

    private companion object {
        const val DEFAULT_BACKGROUND_COLOR = 0x0 // Transparent
    }
}
