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

package androidx.wear.compose.remote.material3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [35], qualifiers = "w1000dp-h500dp")
@RunWith(RobolectricTestRunner::class)
class RemoteSegmentedCircularProgressIndicatorUnitTest {

    @get:Rule val captureRule = RemoteCaptureTestRule()

    @Test
    fun progress_segmentCountZero_throws() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                capture {
                    RemoteSegmentedCircularProgressIndicator(
                        segmentCount = 0.ri,
                        progress = 0.5f.rf,
                    )
                }
            }
        assertThat(exception).hasMessageThat().isEqualTo("segmentCount must be at least 1, was 0")
    }

    @Test
    fun progress_segmentCountNegative_throws() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                capture {
                    RemoteSegmentedCircularProgressIndicator(
                        segmentCount = (-1).ri,
                        progress = 0.5f.rf,
                    )
                }
            }
        assertThat(exception).hasMessageThat().isEqualTo("segmentCount must be at least 1, was -1")
    }

    @Test
    fun binary_segmentCountZero_throws() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                capture {
                    RemoteSegmentedCircularProgressIndicator(
                        segmentCount = 0.ri,
                        segmentValue = { true.rb },
                    )
                }
            }
        assertThat(exception).hasMessageThat().isEqualTo("segmentCount must be at least 1, was 0")
    }

    @Test
    fun segmentCountNonConstant_throws() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                capture {
                    val dynamicCount = RemoteInt.createNamedRemoteInt("segments", 5)
                    RemoteSegmentedCircularProgressIndicator(
                        segmentCount = dynamicCount,
                        progress = 0.5f.rf,
                    )
                }
            }
        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("segmentCount must be a constant RemoteInt")
    }

    @Test
    fun segmentCountOne_doesNotThrow() {
        capture {
            RemoteSegmentedCircularProgressIndicator(segmentCount = 1.ri, progress = 0.5f.rf)
        }
        capture {
            RemoteSegmentedCircularProgressIndicator(
                segmentCount = 1.ri,
                segmentValue = { true.rb },
            )
        }
    }

    @Test
    fun progress_segmentIntroAnimation_scalesDotFromSmallToFull() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val displayInfo = createCreationDisplayInfo(context, Size(500f, 500f))
        val doc = runBlocking {
            captureRule.captureDocument(context = context, creationDisplayInfo = displayInfo) {
                val progress = RemoteFloat.createNamedRemoteFloat("progress", 0f)
                RemoteSegmentedCircularProgressIndicator(
                    segmentCount = 5.ri,
                    progress = progress,
                    modifier = RemoteModifier.size(150.rdp),
                    colors =
                        RemoteProgressIndicatorDefaults.colors(
                            indicatorColor = Color.Red.rc,
                            trackColor = Color.Blue.rc,
                        ),
                )
            }
        }
        val remoteContext =
            AndroidRemoteContext().apply { density = context.resources.displayMetrics.density }
        val initBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        remoteContext.useCanvas(Canvas(initBitmap))
        remoteContext.setAnimationTime(0f)
        doc.initializeContext(remoteContext)
        doc.paint(remoteContext, Theme.UNSPECIFIED)

        // Advance progress slightly above 0 (as in a long timer) at t=0s to trigger the ~0.85s
        // spring intro animation on the first segment dot.
        remoteContext.setNamedFloatOverride("USER:progress", 0.001f)
        doc.paint(remoteContext, Theme.UNSPECIFIED)

        val earlyDotBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        remoteContext.useCanvas(Canvas(earlyDotBitmap))
        remoteContext.setAnimationTime(0.15f)
        doc.paint(remoteContext, Theme.UNSPECIFIED)

        val settledDotBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        remoteContext.useCanvas(Canvas(settledDotBitmap))
        remoteContext.setAnimationTime(1.0f)
        doc.paint(remoteContext, Theme.UNSPECIFIED)

        val earlyDotRedPixels = earlyDotBitmap.countPixelsMatching {
            it.red > 0.3f && it.blue < 0.7f
        }
        val settledDotRedPixels = settledDotBitmap.countPixelsMatching {
            it.red > 0.3f && it.blue < 0.7f
        }
        assertThat(earlyDotRedPixels).isGreaterThan(0)
        assertThat(earlyDotRedPixels).isLessThan(settledDotRedPixels)
    }

    private fun Bitmap.countPixelsMatching(predicate: (Color) -> Boolean): Int {
        var count = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (predicate(Color(getPixel(x, y)))) count++
            }
        }
        return count
    }

    private fun capture(content: @Composable @RemoteComposable () -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking {
            captureRule.captureDocument(
                context = context,
                creationDisplayInfo = createCreationDisplayInfo(context, Size(500f, 500f)),
                content = content,
            )
        }
    }
}
