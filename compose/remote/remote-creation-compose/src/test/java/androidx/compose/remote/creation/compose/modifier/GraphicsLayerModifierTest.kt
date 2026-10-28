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

import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.capture.RemoteDensityBehavior
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteOutline
import androidx.compose.remote.creation.compose.shapes.RemoteRectangleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteCompositingStrategy
import androidx.compose.remote.creation.compose.state.RemoteTileMode
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@SdkSuppress(minSdkVersion = 29)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class GraphicsLayerModifierTest {

    @Test
    fun remoteRectangleShape_encodesShapeRectWithoutRadius() {
        val (op, attrs, _) =
            recordAndEvaluate {
                RemoteModifier.graphicsLayer(shape = RemoteRectangleShape)
            }
        val values = op.values

        assertThat(attrs[GraphicsLayerModifierOperation.SHAPE])
            .isEqualTo(GraphicsLayerModifierOperation.SHAPE_RECT)
        assertThat(attrs).doesNotContainKey(GraphicsLayerModifierOperation.SHAPE_RADIUS)
        assertThat(values[GraphicsLayerModifierOperation.SHAPE].intValue)
            .isEqualTo(GraphicsLayerModifierOperation.SHAPE_RECT)
        assertThat(values[GraphicsLayerModifierOperation.SHAPE_RADIUS].needsToWrite()).isFalse()
    }

    @Test
    fun remoteCircleShape_encodesShapeCircle() {
        val (op, _, _) =
            recordAndEvaluate {
                RemoteModifier.graphicsLayer(shape = RemoteCircleShape)
            }

        assertThat(op.values[GraphicsLayerModifierOperation.SHAPE].intValue)
            .isEqualTo(GraphicsLayerModifierOperation.SHAPE_CIRCLE)
        assertThat(op.values[GraphicsLayerModifierOperation.SHAPE_RADIUS].needsToWrite()).isFalse()
    }

    @Test
    fun remoteRoundedCornerShape_rdpPixels_resolvesScaledRadius() {
        val (op, _, _) =
            recordAndEvaluate(density = 2f, densityBehavior = RemoteDensityBehavior.Pixels) {
                RemoteModifier.graphicsLayer(shape = RemoteRoundedCornerShape(16.rdp))
            }

        assertThat(op.values[GraphicsLayerModifierOperation.SHAPE].intValue)
            .isEqualTo(GraphicsLayerModifierOperation.SHAPE_ROUND_RECT)
        assertThat(op.values[GraphicsLayerModifierOperation.SHAPE_RADIUS].value).isEqualTo(32f)
    }

    @Test
    fun remoteRoundedCornerShape_rdpDensityBehaviorDp_resolvesUnscaledRadius() {
        val (op, _, _) =
            recordAndEvaluate(density = 2f, densityBehavior = RemoteDensityBehavior.Dp) {
                RemoteModifier.graphicsLayer(shape = RemoteRoundedCornerShape(16.rdp))
            }

        assertThat(op.values[GraphicsLayerModifierOperation.SHAPE].intValue)
            .isEqualTo(GraphicsLayerModifierOperation.SHAPE_ROUND_RECT)
        assertThat(op.values[GraphicsLayerModifierOperation.SHAPE_RADIUS].value).isEqualTo(16f)
    }

    @Test
    fun remoteRoundedCornerShape_rf_resolvesPixelRadius() {
        val (op, _, _) =
            recordAndEvaluate(density = 2f, densityBehavior = RemoteDensityBehavior.Pixels) {
                RemoteModifier.graphicsLayer(shape = RemoteRoundedCornerShape(24f.rf))
            }

        assertThat(op.values[GraphicsLayerModifierOperation.SHAPE].intValue)
            .isEqualTo(GraphicsLayerModifierOperation.SHAPE_ROUND_RECT)
        assertThat(op.values[GraphicsLayerModifierOperation.SHAPE_RADIUS].value).isEqualTo(24f)
    }

    @Test
    fun remoteRoundedCornerShape_percent_resolvesPercentOfMinDimension() {
        val (op, _, _) =
            recordAndEvaluate(
                density = 2f,
                densityBehavior = RemoteDensityBehavior.Pixels,
                componentWidth = 100f,
                componentHeight = 80f,
            ) {
                RemoteModifier.graphicsLayer(shape = RemoteRoundedCornerShape(25))
            }

        assertThat(op.values[GraphicsLayerModifierOperation.SHAPE].intValue)
            .isEqualTo(GraphicsLayerModifierOperation.SHAPE_ROUND_RECT)
        assertThat(op.values[GraphicsLayerModifierOperation.SHAPE_RADIUS].value).isEqualTo(20f)
    }

    @Test
    fun unsupportedRemoteShape_omitsShapeAndRadius() {
        val customRemoteShape =
            object : RemoteShape {
                override fun createOutline(
                    size: RemoteSize,
                    layoutDirection: LayoutDirection,
                    density: RemoteDensity,
                ): RemoteOutline {
                    throw UnsupportedOperationException()
                }
            }

        val (_, attrs, _) =
            recordAndEvaluate {
                RemoteModifier.graphicsLayer(shape = customRemoteShape)
            }

        assertThat(attrs).doesNotContainKey(GraphicsLayerModifierOperation.SHAPE)
        assertThat(attrs).doesNotContainKey(GraphicsLayerModifierOperation.SHAPE_RADIUS)
    }

    @Test
    fun remoteCompositingStrategy_encodesOffscreenAndModulateAlpha() {
        val (offscreenOp, offscreenAttrs, _) =
            recordAndEvaluate {
                RemoteModifier.graphicsLayer(
                    shape = RemoteRectangleShape,
                    compositingStrategy = RemoteCompositingStrategy.Offscreen,
                )
            }
        assertThat(offscreenAttrs[GraphicsLayerModifierOperation.COMPOSITING_STRATEGY]).isEqualTo(1)
        assertThat(offscreenOp.values[GraphicsLayerModifierOperation.COMPOSITING_STRATEGY].intValue)
            .isEqualTo(1)

        val (modulateOp, modulateAttrs, _) =
            recordAndEvaluate {
                RemoteModifier.graphicsLayer(
                    shape = RemoteRectangleShape,
                    compositingStrategy = RemoteCompositingStrategy.ModulateAlpha,
                )
            }
        assertThat(modulateAttrs[GraphicsLayerModifierOperation.COMPOSITING_STRATEGY]).isEqualTo(2)
        assertThat(modulateOp.values[GraphicsLayerModifierOperation.COMPOSITING_STRATEGY].intValue)
            .isEqualTo(2)
    }

    @Test
    fun blurEffect_remoteTileModeMirror_encodesBlurRadiusAndTileMode() {
        val (op, attrs, _) =
            recordAndEvaluate {
                RemoteModifier.graphicsLayer(
                    shape = RemoteRectangleShape,
                    renderEffect = RemoteBlurEffect(8f, 12f, RemoteTileMode.Mirror),
                )
            }

        assertThat(attrs[GraphicsLayerModifierOperation.BLUR_TILE_MODE])
            .isEqualTo(GraphicsLayerModifierOperation.TILE_MODE_MIRROR)
        assertThat(op.values[GraphicsLayerModifierOperation.BLUR_RADIUS_X].value).isEqualTo(8f)
        assertThat(op.values[GraphicsLayerModifierOperation.BLUR_RADIUS_Y].value).isEqualTo(12f)
        assertThat(op.values[GraphicsLayerModifierOperation.BLUR_TILE_MODE].intValue)
            .isEqualTo(GraphicsLayerModifierOperation.TILE_MODE_MIRROR)
    }
}
