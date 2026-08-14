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

import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.capture.path
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.Color
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
}
