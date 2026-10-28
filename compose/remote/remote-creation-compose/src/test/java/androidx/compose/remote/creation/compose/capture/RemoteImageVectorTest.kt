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

import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.horizontalGradient
import androidx.compose.remote.creation.compose.shaders.linearGradient
import androidx.compose.remote.creation.compose.shaders.radialGradient
import androidx.compose.remote.creation.compose.shaders.solidColor
import androidx.compose.remote.creation.compose.shaders.sweepGradient
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePathFillType
import androidx.compose.remote.creation.compose.state.RemoteStrokeCap
import androidx.compose.remote.creation.compose.state.RemoteStrokeJoin
import androidx.compose.remote.creation.compose.state.createNamedRemoteColor
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.vector.RemotePathNode
import androidx.compose.ui.graphics.Brush
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
                    path(fill = RemoteBrush.solidColor(Color.Red.rc)) {
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
                .path(fill = RemoteBrush.solidColor(Color.Red.rc)) {
                    moveTo(0f.rf, 0f.rf)
                    lineTo(24f.rf, 24f.rf)
                }
                .build()

        assertThat(remoteImageVector.hasClipPath).isFalse()
    }

    @Test
    fun builderPath_supportsRemoteGradientAndDynamicSolidBrushes() {
        val dynamicColor = createNamedRemoteColor("iconFill", Color.Magenta)
        val fillBrush = RemoteBrush.linearGradient(listOf(Color.Red.rc, dynamicColor))
        val strokeBrush = RemoteBrush.radialGradient(listOf(Color.Cyan.rc, Color.Blue.rc))
        val sweepBrush = RemoteBrush.sweepGradient(listOf(Color.Yellow.rc, Color.Green.rc))
        val horizontalBrush = RemoteBrush.horizontalGradient(listOf(Color.White.rc, Color.Black.rc))

        val remoteImageVector =
            RemoteImageVector.Builder(
                    viewportWidth = 24f.rf,
                    viewportHeight = 24f.rf,
                    tintColor = Color.Unspecified.rc,
                )
                .path(
                    name = "gradient_path",
                    fill = fillBrush,
                    fillAlpha = 0.6f.rf,
                    stroke = strokeBrush,
                    strokeAlpha = 0.8f.rf,
                    strokeLineWidth = 2f.rf,
                ) {
                    moveTo(0f.rf, 0f.rf)
                    lineTo(24f.rf, 24f.rf)
                }
                .path(
                    name = "secondary_path",
                    fill = sweepBrush,
                    stroke = horizontalBrush,
                ) {
                    moveTo(2f.rf, 2f.rf)
                    lineTo(22f.rf, 22f.rf)
                }
                .build()

        val firstPath = remoteImageVector.root[0] as RemoteVectorPath
        assertThat(firstPath.fill).isEqualTo(fillBrush)
        assertThat(firstPath.stroke).isEqualTo(strokeBrush)
        assertThat(firstPath.fillAlpha.constantValueOrNull).isEqualTo(0.6f)
        assertThat(firstPath.strokeAlpha.constantValueOrNull).isEqualTo(0.8f)

        val secondPath = remoteImageVector.root[1] as RemoteVectorPath
        assertThat(secondPath.fill).isEqualTo(sweepBrush)
        assertThat(secondPath.stroke).isEqualTo(horizontalBrush)
    }

    @Test
    fun builderRestrictedPathAndAddPath_supportRemoteTypes() {
        val fillBrush = RemoteBrush.horizontalGradient(listOf(Color.Red.rc, Color.Blue.rc))
        val strokeBrush = RemoteBrush.solidColor(Color.Green.rc)

        val remoteImageVector =
            RemoteImageVector.Builder(
                    viewportWidth = 24f.rf,
                    viewportHeight = 24f.rf,
                    tintColor = Color.Unspecified.rc,
                )
                .path(
                    name = "remote_typed_path",
                    fill = fillBrush,
                    stroke = strokeBrush,
                    strokeLineWidth = 2f.rf,
                    strokeLineCap = RemoteStrokeCap.Round,
                    strokeLineJoin = RemoteStrokeJoin.Bevel,
                    pathFillType = RemotePathFillType.EvenOdd,
                ) {
                    moveTo(0f.rf, 0f.rf)
                    lineTo(24f.rf, 24f.rf)
                }
                .addPath(
                    pathData =
                        listOf(
                            RemotePathNode.MoveTo(2f.rf, 2f.rf),
                            RemotePathNode.LineTo(22f.rf, 22f.rf),
                        ),
                    pathFillType = RemotePathFillType.NonZero,
                    name = "direct_add_path",
                    fill = strokeBrush,
                )
                .build()

        val firstPath = remoteImageVector.root[0] as RemoteVectorPath
        assertThat(firstPath.fill).isEqualTo(fillBrush)
        assertThat(firstPath.stroke).isEqualTo(strokeBrush)
        assertThat(firstPath.strokeLineCap).isEqualTo(RemoteStrokeCap.Round)
        assertThat(firstPath.strokeLineJoin).isEqualTo(RemoteStrokeJoin.Bevel)
        assertThat(firstPath.pathFillType).isEqualTo(RemotePathFillType.EvenOdd)

        val secondPath = remoteImageVector.root[1] as RemoteVectorPath
        assertThat(secondPath.name).isEqualTo("direct_add_path")
        assertThat(secondPath.fill).isEqualTo(strokeBrush)
    }

    @Test
    fun toRemoteImageVector_convertsSolidColorAndDropsUnsupportedComposeShaderBrush() {
        val imageVector =
            ImageVector.Builder(
                    name = "mixed_brushes",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .path(
                    fill = SolidColor(Color.Green),
                    stroke = Brush.linearGradient(listOf(Color.Red, Color.Blue)),
                ) {
                    moveTo(0f, 0f)
                    lineTo(24f, 24f)
                }
                .build()

        val remoteImageVector = imageVector.toRemoteImageVector()
        val path = remoteImageVector.root[0] as RemoteVectorPath

        assertThat(path.fill).isEqualTo(RemoteBrush.solidColor(Color.Green.rc))
        assertThat(path.stroke).isNull()
    }
}
