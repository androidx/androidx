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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.modifiers.ComponentModifiers
import androidx.compose.remote.core.operations.layout.modifiers.DrawContentOperation
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DrawWithContentModifierTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun remoteBox_chainedDrawWithContentWithIntermediatePadding_executesInOuterToInnerOrder() =
        runTest {
            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        creationDisplayInfo =
                            RemoteCreationDisplayInfo(width = 100, height = 100, densityDpi = 160),
                    ) {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(100.rdp)
                                    .drawWithContent {
                                        drawRect(
                                            paint = RemotePaint { color = Color.Red.rc },
                                            topLeft = RemoteOffset.Zero,
                                            size = size,
                                        )
                                        drawContent()
                                        drawRect(
                                            paint = RemotePaint { color = Color.Green.rc },
                                            topLeft = RemoteOffset.Zero,
                                            size = RemoteSize(width, 10f.rf),
                                        )
                                    }
                                    .padding(20.rdp)
                                    .drawWithContent {
                                        drawRect(
                                            paint = RemotePaint { color = Color.Blue.rc },
                                            topLeft = RemoteOffset.Zero,
                                            size = size,
                                        )
                                        drawContent()
                                    }
                        ) {}
                    }
                    .bytes

            assertThat(countDrawContentOperations(bytes)).isEqualTo(1)

            val bitmap = renderWithViewPlayer(bytes, 100, 100)

            val topStrip = bitmap.getPixel(50, 5)
            assertThat(AndroidColor.green(topStrip)).isGreaterThan(200)
            assertThat(AndroidColor.red(topStrip)).isLessThan(50)

            val outerPadding = bitmap.getPixel(10, 50)
            assertThat(AndroidColor.red(outerPadding)).isGreaterThan(200)
            assertThat(AndroidColor.blue(outerPadding)).isLessThan(50)

            val innerContent = bitmap.getPixel(50, 50)
            assertThat(AndroidColor.blue(innerContent)).isGreaterThan(200)
            assertThat(AndroidColor.red(innerContent)).isLessThan(50)
        }

    @Test
    fun remoteCanvas_chainedDrawWithContent_executesInOuterToInnerOrder() = runTest {
        val bytes =
            captureSingleRemoteDocument(
                    context = context,
                    creationDisplayInfo =
                        RemoteCreationDisplayInfo(width = 100, height = 100, densityDpi = 160),
                ) {
                    RemoteCanvas(
                        modifier =
                            RemoteModifier.size(100.rdp)
                                .drawWithContent {
                                    drawRect(
                                        paint = RemotePaint { color = Color.Red.rc },
                                        topLeft = RemoteOffset.Zero,
                                        size = RemoteSize(100f.rf, 100f.rf),
                                    )
                                    drawContent()
                                    drawRect(
                                        paint = RemotePaint { color = Color.Green.rc },
                                        topLeft = RemoteOffset.Zero,
                                        size = RemoteSize(100f.rf, 10f.rf),
                                    )
                                }
                                .drawWithContent {
                                    drawRect(
                                        paint = RemotePaint { color = Color.Blue.rc },
                                        topLeft = RemoteOffset(20f.rf, 20f.rf),
                                        size = RemoteSize(60f.rf, 60f.rf),
                                    )
                                    drawContent()
                                }
                    ) {
                        drawRect(
                            paint = RemotePaint { color = Color.Yellow.rc },
                            topLeft = RemoteOffset(40f.rf, 40f.rf),
                            size = RemoteSize(20f.rf, 20f.rf),
                        )
                    }
                }
                .bytes

        assertThat(countDrawContentOperations(bytes)).isEqualTo(1)

        val bitmap = renderWithViewPlayer(bytes, 100, 100)

        val topStrip = bitmap.getPixel(50, 5)
        assertThat(AndroidColor.green(topStrip)).isGreaterThan(200)

        val outerPixel = bitmap.getPixel(10, 50)
        assertThat(AndroidColor.red(outerPixel)).isGreaterThan(200)

        val midPixel = bitmap.getPixel(25, 50)
        assertThat(AndroidColor.blue(midPixel)).isGreaterThan(200)

        val centerPixel = bitmap.getPixel(50, 50)
        assertThat(AndroidColor.red(centerPixel)).isGreaterThan(200)
        assertThat(AndroidColor.green(centerPixel)).isGreaterThan(200)
        assertThat(AndroidColor.blue(centerPixel)).isLessThan(50)
    }

    private fun renderWithViewPlayer(bytes: ByteArray, width: Int, height: Int): Bitmap {
        val player =
            RemoteComposePlayer(context).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                setDocument(bytes)
            }
        player.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        player.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        player.draw(Canvas(bitmap))
        return bitmap
    }

    private fun countDrawContentOperations(bytes: ByteArray): Int {
        val document =
            CoreDocument().apply {
                initFromBuffer(RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(bytes)))
            }
        fun countIn(ops: List<Operation>): Int = ops.sumOf { op ->
            when (op) {
                is DrawContentOperation -> 1
                is ComponentModifiers -> countIn(op.list)
                is Component -> countIn(op.list)
                else -> 0
            }
        }
        return countIn(document.operations)
    }
}
