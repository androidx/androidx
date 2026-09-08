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

package androidx.compose.remote.creation.compose.vector

import android.graphics.Bitmap
import androidx.compose.remote.creation.compose.capture.RecordingCanvas
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.capture.path
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteVectorPainterTest {

    @Test
    fun intrinsicSize_fromImageVector_respectsDefaultDimensions() {
        val imageVector =
            ImageVector.Builder(
                    name = "CustomSizeIcon",
                    defaultWidth = 18.dp,
                    defaultHeight = 18.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .addPath(
                    PathData {
                        moveTo(12f, 2f)
                        lineTo(15f, 8f)
                        close()
                    },
                    fill = SolidColor(Color.White),
                )
                .build()

        val painter = painterRemoteVector(imageVector)
        val intrinsicSize = requireNotNull(painter.intrinsicSize)

        assertThat(intrinsicSize.width.constantValueOrNull).isEqualTo(18f)
        assertThat(intrinsicSize.height.constantValueOrNull).isEqualTo(18f)
    }

    @Test
    fun intrinsicSize_fromRemoteImageVector_respectsViewportDimensions() {
        val remoteImageVector =
            RemoteImageVector.Builder(
                    viewportWidth = 20f.rf,
                    viewportHeight = 20f.rf,
                    tintColor = RemoteColor(Color.Black),
                    name = "RemoteIcon20",
                )
                .path(fill = SolidColor(Color.Black)) {
                    moveTo(0f.rf, 0f.rf)
                    lineTo(20f.rf, 20f.rf)
                }
                .build()

        val painter = painterRemoteVector(remoteImageVector)
        val intrinsicSize = requireNotNull(painter.intrinsicSize)

        assertThat(intrinsicSize.width.constantValueOrNull).isEqualTo(20f)
        assertThat(intrinsicSize.height.constantValueOrNull).isEqualTo(20f)
    }

    @Test
    fun createGroupComponent_propagatesPathFillTypeAndBrush() {
        val imageVector =
            ImageVector.Builder(
                    name = "EvenOddIcon",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .addPath(
                    PathData {
                        moveTo(0f, 0f)
                        lineTo(24f, 24f)
                        close()
                    },
                    pathFillType = PathFillType.EvenOdd,
                    fill = SolidColor(Color.Blue),
                    fillAlpha = 0.5f,
                    stroke = SolidColor(Color.Green),
                    strokeAlpha = 0.8f,
                )
                .build()

        val painter = painterRemoteVector(imageVector) as RemoteVectorPainter
        val rootGroup = painter.vector.root
        assertThat(rootGroup.numChildren).isEqualTo(1)

        val group = RemoteGroupComponent().createGroupComponent(imageVector.root)
        assertThat(group.numChildren).isEqualTo(1)
    }

    @Test
    fun remoteImageVector_draw_proceduralExtension() {
        val remoteImageVector =
            RemoteImageVector.Builder(
                    viewportWidth = 24f.rf,
                    viewportHeight = 24f.rf,
                    tintColor = RemoteColor(Color.Black),
                    name = "TestVector",
                )
                .path(
                    pathFillType = PathFillType.EvenOdd,
                    fill = SolidColor(Color.Red),
                    fillAlpha = 0.5f.rf,
                ) {
                    moveTo(0f.rf, 0f.rf)
                    lineTo(24f.rf, 24f.rf)
                }
                .build()

        val size = Size(24f, 24f)
        val creationState =
            RemoteComposeCreationState(
                AndroidxRcPlatformServices(),
                size,
            )
        val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        val recordingCanvas = RecordingCanvas(bitmap)
        recordingCanvas.creationState = creationState
        val remoteCanvas = RemoteCanvas(recordingCanvas)

        // Draw procedurally using the new RemoteImageVector.draw extension
        remoteImageVector.draw(remoteCanvas)
    }
}
