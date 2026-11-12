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

package androidx.compose.material3

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.tokens.ShapeKeyTokens
import androidx.compose.material3.tokens.ShapeToken
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ShapesTest {

    @Test
    fun shapeDefaults_fullAndNone() {
        assertThat(ShapeDefaults.Full).isEqualTo(CircleShape)
        assertThat(ShapeDefaults.None).isEqualTo(RectangleShape)
    }

    @Test
    fun defaults_fullIsCircleShape_noneIsRectangleShape() {
        val shapes = Shapes()
        assertThat(shapes.full).isEqualTo(CircleShape)
        assertThat(shapes.none).isEqualTo(RectangleShape)
        assertThat(shapes.fromToken(ShapeKeyTokens.CornerFull)).isEqualTo(CircleShape)
        assertThat(shapes.fromToken(ShapeKeyTokens.CornerNone)).isEqualTo(RectangleShape)
        assertThat(shapes.fromToken(ShapeToken(-1))).isEqualTo(RectangleShape)
    }

    @Test
    fun fromToken_usesCustomFullAndNonCornerBasedNone() {
        val customFull = RoundedCornerShape(12.dp)
        val customNone = GenericShape { _, _ -> }
        val shapes = Shapes(full = customFull, none = customNone)

        assertThat(shapes.fromToken(ShapeKeyTokens.CornerFull)).isEqualTo(customFull)
        assertThat(shapes.fromToken(ShapeKeyTokens.CornerNone)).isEqualTo(customNone)
        assertThat(shapes.fromToken(ShapeToken(-1))).isEqualTo(customNone)
    }

    @Test
    fun copy_preservesAndOverridesAllProperties() {
        val customSmall = RoundedCornerShape(6.dp)
        val customLargeIncreased = RoundedCornerShape(22.dp)
        val customFull = RoundedCornerShape(10.dp)
        val customNone = RoundedCornerShape(1.dp)
        val base =
            Shapes(
                small = customSmall,
                largeIncreased = customLargeIncreased,
                full = customFull,
                none = customNone,
            )

        assertThat(base.copy()).isEqualTo(base)

        val updatedSmall = RoundedCornerShape(7.dp)
        val copiedSmall = base.copy(small = updatedSmall)
        assertThat(copiedSmall.small).isEqualTo(updatedSmall)
        assertThat(copiedSmall.largeIncreased).isEqualTo(customLargeIncreased)
        assertThat(copiedSmall.full).isEqualTo(customFull)
        assertThat(copiedSmall.none).isEqualTo(customNone)

        val updatedLargeIncreased = RoundedCornerShape(24.dp)
        val copiedLargeIncreased = base.copy(largeIncreased = updatedLargeIncreased)
        assertThat(copiedLargeIncreased.largeIncreased).isEqualTo(updatedLargeIncreased)
        assertThat(copiedLargeIncreased.full).isEqualTo(customFull)
        assertThat(copiedLargeIncreased.none).isEqualTo(customNone)

        val newFull = RoundedCornerShape(14.dp)
        val newNone = RoundedCornerShape(2.dp)
        val copiedFullAndNone = base.copy(full = newFull, none = newNone)
        assertThat(copiedFullAndNone.full).isEqualTo(newFull)
        assertThat(copiedFullAndNone.none).isEqualTo(newNone)
    }

    @Test
    fun equalsHashCodeToString_includeFullAndNone() {
        val base = Shapes()
        val diffFull = Shapes(full = RoundedCornerShape(12.dp))
        val diffNone = Shapes(none = RoundedCornerShape(2.dp))

        assertThat(base).isNotEqualTo(diffFull)
        assertThat(base.hashCode()).isNotEqualTo(diffFull.hashCode())
        assertThat(base).isNotEqualTo(diffNone)
        assertThat(base.hashCode()).isNotEqualTo(diffNone.hashCode())

        assertThat(base.toString()).contains("full=")
        assertThat(base.toString()).contains("none=")
    }
}
