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

package androidx.compose.remote.creation.compose.capture

import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.vector.RemotePathNode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteImageVectorTest {

    @Test
    fun toRemoteImageVector_convertsProperties() {
        val imageVector =
            ImageVector.Builder(
                    name = "test_icon",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .path(fill = SolidColor(Color.Red)) {
                    moveTo(0f, 0f)
                    lineTo(24f, 24f)
                }
                .build()

        val remoteImageVector = imageVector.toRemoteImageVector()

        assertThat(remoteImageVector.name).isEqualTo("test_icon")
        assertThat(remoteImageVector.viewportWidth.constantValueOrNull).isEqualTo(24f)
        assertThat(remoteImageVector.viewportHeight.constantValueOrNull).isEqualTo(24f)
    }

    @Test
    fun hasClipPath_withoutClipPath_returnsFalse() {
        val imageVector =
            ImageVector.Builder(
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .path(fill = SolidColor(Color.Red)) {
                    moveTo(0f, 0f)
                    lineTo(24f, 24f)
                }
                .build()

        val remoteImageVector = imageVector.toRemoteImageVector()

        assertThat(remoteImageVector.hasClipPath).isFalse()
    }

    @Test
    fun hasClipPath_withClipPathInGroup_returnsTrue() {
        val imageVector =
            ImageVector.Builder(
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .group(
                    name = "clip_group",
                    clipPathData =
                        listOf(
                            PathNode.MoveTo(0f, 0f),
                            PathNode.LineTo(10f, 10f),
                            PathNode.Close,
                        ),
                ) {
                    path(fill = SolidColor(Color.Red)) {
                        moveTo(0f, 0f)
                        lineTo(24f, 24f)
                    }
                }
                .build()

        val remoteImageVector = imageVector.toRemoteImageVector()

        assertThat(remoteImageVector.hasClipPath).isTrue()
    }

    @Test
    fun hasClipPath_withClipPathInNestedGroup_returnsTrue() {
        val imageVector =
            ImageVector.Builder(
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .group(name = "outer_group") {
                    group(
                        name = "inner_clip_group",
                        clipPathData =
                            listOf(
                                PathNode.MoveTo(0f, 0f),
                                PathNode.LineTo(10f, 10f),
                                PathNode.Close,
                            ),
                    ) {
                        path(fill = SolidColor(Color.Red)) {
                            moveTo(0f, 0f)
                            lineTo(24f, 24f)
                        }
                    }
                }
                .build()

        val remoteImageVector = imageVector.toRemoteImageVector()

        assertThat(remoteImageVector.hasClipPath).isTrue()
    }

    @Test
    fun hasClipPath_builtWithRemoteImageVectorBuilder_withClipPath_returnsTrue() {
        val remoteImageVector =
            RemoteImageVector.Builder(
                    viewportWidth = 24f.rf,
                    viewportHeight = 24f.rf,
                    tintColor = RemoteColor(Color.Black),
                )
                .group(
                    clipPathData =
                        listOf(
                            RemotePathNode.MoveTo(0f.rf, 0f.rf),
                            RemotePathNode.LineTo(10f.rf, 10f.rf),
                            RemotePathNode.Close,
                        )
                ) {
                    path(fill = SolidColor(Color.Red)) {
                        moveTo(0f.rf, 0f.rf)
                        lineTo(24f.rf, 24f.rf)
                    }
                }
                .build()

        assertThat(remoteImageVector.hasClipPath).isTrue()
    }

    @Test
    fun hasClipPath_builtWithRemoteImageVectorBuilder_withoutClipPath_returnsFalse() {
        val remoteImageVector =
            RemoteImageVector.Builder(
                    viewportWidth = 24f.rf,
                    viewportHeight = 24f.rf,
                    tintColor = RemoteColor(Color.Black),
                )
                .path(fill = SolidColor(Color.Red)) {
                    moveTo(0f.rf, 0f.rf)
                    lineTo(24f.rf, 24f.rf)
                }
                .build()

        assertThat(remoteImageVector.hasClipPath).isFalse()
    }
}
