/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.foundation.shape.AbsoluteCutCornerShape
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class AnimatedCornerShapeTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun convertsRoundedCornerShape() {
        val roundedCornerShape = RoundedCornerShape(10.dp)

        val roundedPolygon =
            roundedCornerShape.toRoundedPolygonOrNull(
                size = Size(100f, 100f),
                density = Density(density = 2f),
                layoutDirection = LayoutDirection.Ltr
            )

        assertThat(roundedPolygon).isNotNull()
        assertThat(roundedPolygon!!.calculateBounds()).isEqualTo(floatArrayOf(0f, 0f, 1f, 1f))
        assertThat(roundedPolygon.cubics.size).isEqualTo(9)

        val points =
            roundedPolygon.cubics.flatMap {
                listOf(Offset(it.anchor0X, it.anchor0Y), Offset(it.anchor1X, it.anchor1Y))
            }
        assertThat(points)
            .containsAtLeast(
                Offset(1.0f, 0.8f),
                Offset(0.8f, 1.0f),
                Offset(0.2f, 1.0f),
                Offset(0.0f, 0.8f),
                Offset(0.0f, 0.2f),
                Offset(0.2f, 0.0f),
                Offset(0.8f, 0.0f),
                Offset(1.0f, 0.2f),
            )
    }

    @Test
    fun convertsCutCornerShape() {
        val cutCornerShape = CutCornerShape(10.dp)

        val roundedPolygon =
            cutCornerShape.toRoundedPolygonOrNull(
                size = Size(100f, 100f),
                density = Density(density = 2f),
                layoutDirection = LayoutDirection.Ltr
            )

        assertThat(roundedPolygon).isNotNull()
        assertThat(roundedPolygon!!.calculateBounds()).isEqualTo(floatArrayOf(0f, 0f, 1f, 1f))
        assertThat(roundedPolygon.cubics.size).isEqualTo(8)

        val points =
            roundedPolygon.cubics.flatMap {
                listOf(Offset(it.anchor0X, it.anchor0Y), Offset(it.anchor1X, it.anchor1Y))
            }
        assertThat(points)
            .containsAtLeast(
                Offset(1.0f, 0.8f),
                Offset(0.8f, 1.0f),
                Offset(0.2f, 1.0f),
                Offset(0.0f, 0.8f),
                Offset(0.0f, 0.2f),
                Offset(0.2f, 0.0f),
                Offset(0.8f, 0.0f),
                Offset(1.0f, 0.2f),
            )
    }

    @Test
    fun convertsAbsoluteRoundedCornerShape() {
        val roundedCornerShape = AbsoluteRoundedCornerShape(5.dp)

        val roundedPolygon =
            roundedCornerShape.toRoundedPolygonOrNull(
                size = Size(100f, 100f),
                density = Density(density = 2f),
                layoutDirection = LayoutDirection.Ltr
            )

        assertThat(roundedPolygon).isNotNull()
        assertThat(roundedPolygon!!.calculateBounds()).isEqualTo(floatArrayOf(0f, 0f, 1f, 1f))
        assertThat(roundedPolygon.cubics.size).isEqualTo(9)

        val points =
            roundedPolygon.cubics.flatMap {
                listOf(Offset(it.anchor0X, it.anchor0Y), Offset(it.anchor1X, it.anchor1Y))
            }
        assertThat(points)
            .containsAtLeast(
                Offset(1.0f, 0.9f),
                Offset(0.9f, 1.0f),
                Offset(0.1f, 1.0f),
                Offset(0.0f, 0.9f),
                Offset(0.0f, 0.1f),
                Offset(0.1f, 0.0f),
                Offset(0.9f, 0.0f),
                Offset(1.0f, 0.1f),
            )
    }

    @Test
    fun convertsAbsoluteCutCornerShape() {
        val cutCornerShape = AbsoluteCutCornerShape(5.dp)

        val roundedPolygon =
            cutCornerShape.toRoundedPolygonOrNull(
                size = Size(100f, 100f),
                density = Density(density = 2f),
                layoutDirection = LayoutDirection.Ltr
            )

        assertThat(roundedPolygon).isNotNull()
        assertThat(roundedPolygon!!.calculateBounds()).isEqualTo(floatArrayOf(0f, 0f, 1f, 1f))
        assertThat(roundedPolygon.cubics.size).isEqualTo(8)

        val points =
            roundedPolygon.cubics.flatMap {
                listOf(Offset(it.anchor0X, it.anchor0Y), Offset(it.anchor1X, it.anchor1Y))
            }
        assertThat(points)
            .containsAtLeast(
                Offset(1.0f, 0.9f),
                Offset(0.9f, 1.0f),
                Offset(0.1f, 1.0f),
                Offset(0.0f, 0.9f),
                Offset(0.0f, 0.1f),
                Offset(0.1f, 0.0f),
                Offset(0.9f, 0.0f),
                Offset(1.0f, 0.1f),
            )
    }
}
