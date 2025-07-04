/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.viewfinder.view

import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.camera.viewfinder.core.ScaleType
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRect
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SdkSuppress(minSdkVersion = 33) // Required for screenshot tests
@LargeTest
@RunWith(Parameterized::class)
class ViewfinderViewBitmapTest(
    private val implementationMode: ImplementationMode,
    private val scaleType: ScaleType,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "impl={0},scaleType={1}")
        fun data() =
            setOf(ImplementationMode.EMBEDDED, ImplementationMode.EXTERNAL).flatMap { impl ->
                setOf(
                        ScaleType.FILL_CENTER,
                        ScaleType.FILL_START,
                        ScaleType.FILL_END,
                        ScaleType.FIT_CENTER,
                        ScaleType.FIT_START,
                        ScaleType.FIT_END,
                    )
                    .map { scaleType -> arrayOf(impl, scaleType) }
            }
    }

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_CAMERA_VIEWFINDER_VIEW)

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun bitmapCapturesCorrectly() = runViewfinderTest {
        val surfaceRequest =
            ViewfinderSurfaceRequest(
                width = ANY_WIDTH,
                height = ANY_HEIGHT,
                implementationMode = implementationMode,
            )

        withTimeout(REQUEST_TIMEOUT) {
                withContext(Dispatchers.Main) {
                    viewfinder.scaleType = scaleType
                    viewfinder.transformationInfo = ANY_TRANSFORMATION_INFO
                    viewfinder.requestSurfaceSession(viewfinderSurfaceRequest = surfaceRequest)
                }
            }
            .use { surfaceSession ->
                surfaceSession.surface.lockHardwareCanvas().let { canvas ->
                    try {
                        ContextCompat.getDrawable(
                                context,
                                androidx.camera.viewfinder.view.test.R.drawable.baseline_face_2_24,
                            )
                            ?.let { drawable ->
                                // Map input drawable to the output surface
                                val dstRect =
                                    RectF(
                                            0f,
                                            0f,
                                            drawable.intrinsicWidth.toFloat(),
                                            drawable.intrinsicHeight.toFloat(),
                                        )
                                        .also { srcRect ->
                                            Matrix()
                                                .apply {
                                                    setRectToRect(
                                                        srcRect,
                                                        RectF(
                                                            0f,
                                                            0f,
                                                            canvas.width.toFloat(),
                                                            canvas.height.toFloat(),
                                                        ),
                                                        Matrix.ScaleToFit.CENTER,
                                                    )
                                                }
                                                .mapRect(srcRect)
                                        }

                                canvas.drawColor(Color.GRAY)

                                drawable.bounds = dstRect.toRect()
                                drawable.draw(canvas)
                            }
                    } finally {
                        surfaceSession.surface.unlockCanvasAndPost(canvas)
                    }
                }
            }

        val outputBitmap = withContext(Dispatchers.Main) { viewfinder.bitmap }
        assertThat(outputBitmap).isNotNull()
        outputBitmap?.assertAgainstGolden(
            rule = screenshotRule,
            goldenIdentifier = "upright_face_${"$scaleType".lowercase()}",
        )
    }
}
