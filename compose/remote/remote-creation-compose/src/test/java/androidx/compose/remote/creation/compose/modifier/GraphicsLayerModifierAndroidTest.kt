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

import android.util.Log
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.remote.core.operations.layout.modifiers.GraphicsLayerModifierOperation
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@SdkSuppress(minSdkVersion = 29)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class GraphicsLayerModifierAndroidTest {

    @Test
    fun defaultRectangleShape_encodesShapeRectWithoutRadius() {
        val (op, attrs, _) = recordAndEvaluate { RemoteModifier.graphicsLayer() }
        val values = op.values

        assertThat(attrs[GraphicsLayerModifierOperation.SHAPE])
            .isEqualTo(GraphicsLayerModifierOperation.SHAPE_RECT)
        assertThat(attrs).doesNotContainKey(GraphicsLayerModifierOperation.SHAPE_RADIUS)
        assertThat(values[GraphicsLayerModifierOperation.SHAPE].intValue)
            .isEqualTo(GraphicsLayerModifierOperation.SHAPE_RECT)
        assertThat(values[GraphicsLayerModifierOperation.SHAPE_RADIUS].needsToWrite()).isFalse()
    }

    @Test
    fun circleShape_encodesShapeRoundRectWithBaselineConstantRadius() {
        val (op, _, _) =
            recordAndEvaluate {
                RemoteModifier.graphicsLayer(shape = CircleShape)
            }

        assertThat(op.values[GraphicsLayerModifierOperation.SHAPE].intValue)
            .isEqualTo(GraphicsLayerModifierOperation.SHAPE_ROUND_RECT)
        assertThat(op.values[GraphicsLayerModifierOperation.SHAPE_RADIUS].value).isEqualTo(40f)
    }

    @Test
    fun roundedCornerShape_encodesShapeRoundRectWithBaselineConstantRadius() {
        val (op, _, _) =
            recordAndEvaluate {
                RemoteModifier.graphicsLayer(shape = RoundedCornerShape(12.dp))
            }

        assertThat(op.values[GraphicsLayerModifierOperation.SHAPE].intValue)
            .isEqualTo(GraphicsLayerModifierOperation.SHAPE_ROUND_RECT)
        assertThat(op.values[GraphicsLayerModifierOperation.SHAPE_RADIUS].value).isEqualTo(40f)
    }

    @Test
    fun graphicsLayerScopeLambda_roundedCornerShape_propagatesShapeAndAlpha() {
        val (op, _, _) =
            recordAndEvaluate {
                RemoteModifier.graphicsLayer {
                    alpha = 0.5f.rf
                    shape = RoundedCornerShape(10.dp)
                }
            }

        assertThat(op.values[GraphicsLayerModifierOperation.SHAPE].intValue)
            .isEqualTo(GraphicsLayerModifierOperation.SHAPE_ROUND_RECT)
        assertThat(op.values[GraphicsLayerModifierOperation.SHAPE_RADIUS].value).isEqualTo(40f)
        assertThat(op.values[GraphicsLayerModifierOperation.ALPHA].value).isEqualTo(0.5f)
    }

    @Test
    fun graphicsLayerScopeLambda_circleShape_propagatesShapeRoundRectWithBaselineRadius() {
        val (op, _, _) =
            recordAndEvaluate {
                RemoteModifier.graphicsLayer { shape = CircleShape }
            }

        assertThat(op.values[GraphicsLayerModifierOperation.SHAPE].intValue)
            .isEqualTo(GraphicsLayerModifierOperation.SHAPE_ROUND_RECT)
        assertThat(op.values[GraphicsLayerModifierOperation.SHAPE_RADIUS].value).isEqualTo(40f)
    }

    @Test
    fun graphicsLayerScopeLambda_defaultShape_encodesShapeRect() {
        val (op, _, _) = recordAndEvaluate { RemoteModifier.graphicsLayer {} }

        assertThat(op.values[GraphicsLayerModifierOperation.SHAPE].intValue)
            .isEqualTo(GraphicsLayerModifierOperation.SHAPE_RECT)
    }

    @Test
    fun unsupportedShape_fallsBackToRectAndLogsWarning() {
        val customComposeShape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline = Outline.Rectangle(Rect.Zero)

                override fun toString(): String = "CustomComposeShape"
            }
        ShadowLog.clear()

        val (op, attrs, _) =
            recordAndEvaluate {
                RemoteModifier.graphicsLayer(shape = customComposeShape)
            }

        assertThat(attrs[GraphicsLayerModifierOperation.SHAPE])
            .isEqualTo(GraphicsLayerModifierOperation.SHAPE_RECT)
        assertThat(op.values[GraphicsLayerModifierOperation.SHAPE].intValue)
            .isEqualTo(GraphicsLayerModifierOperation.SHAPE_RECT)
        val logs = ShadowLog.getLogsForTag("GraphicsLayerModifier")
        assertThat(logs).isNotEmpty()
        assertThat(logs.last().type).isEqualTo(Log.WARN)
        assertThat(logs.last().msg)
            .isEqualTo("Unsupported Shape in graphicsLayer: CustomComposeShape")
    }
}
