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

package androidx.compose.ui.modifier

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class ModifierLocalMapTest {
    @Test
    fun `empty modifier local map`() {
        // Act.
        val modifierLocalMap = modifierLocalMapOf()

        // Assert.
        assertThat(modifierLocalMap).isEqualTo(EmptyMap)
    }

    @Test
    fun `modifier local map with a single null entry`() {
        // Arrange.
        val modifierLocal = modifierLocalOf { "" }

        // Act.
        val modifierLocalMap = modifierLocalMapOf(modifierLocal)

        // Assert.
        assertThat(modifierLocalMap).isInstanceOf(SingleLocalMap::class.java)
        assertThat(modifierLocalMap[modifierLocal]).isNull()
    }

    @Test
    fun `modifier local map with two null entries`() {
        // Arrange.
        val stringModifierLocal = modifierLocalOf<String?> { "" }
        val colorModifierLocal = modifierLocalOf<Color?> { Color.Unspecified }

        // Act.
        val modifierLocalMap = modifierLocalMapOf(stringModifierLocal, colorModifierLocal)

        // Assert.
        assertThat(modifierLocalMap).isInstanceOf(MultiLocalMap::class.java)
        assertThat(modifierLocalMap[stringModifierLocal]).isNull()
        assertThat(modifierLocalMap[colorModifierLocal]).isNull()
    }

    @Test
    fun `modifier local map with three null entries`() {
        // Arrange.
        val stringModifierLocal = modifierLocalOf { "" }
        val colorModifierLocal = modifierLocalOf { Color.Unspecified }
        val directionModifierLocal = modifierLocalOf { LayoutDirection.Ltr }

        // Act.
        val modifierLocalMap =
            modifierLocalMapOf(stringModifierLocal, colorModifierLocal, directionModifierLocal)

        // Assert.
        assertThat(modifierLocalMap).isInstanceOf(MultiLocalMap::class.java)
        assertThat(modifierLocalMap[stringModifierLocal]).isNull()
        assertThat(modifierLocalMap[colorModifierLocal]).isNull()
        assertThat(modifierLocalMap[directionModifierLocal]).isNull()
    }

    @Test
    fun `modifier local map with a single entry`() {
        // Arrange.
        val modifierLocal = modifierLocalOf { "" }

        // Act.
        val modifierLocalMap = modifierLocalMapOf(modifierLocal to "single")

        // Assert.
        assertThat(modifierLocalMap).isInstanceOf(SingleLocalMap::class.java)
        assertThat(modifierLocalMap[modifierLocal]).isEqualTo("single")
    }

    @Test
    fun `modifier local map with two values`() {
        // Arrange.
        val stringModifierLocal = modifierLocalOf { "" }
        val colorModifierLocal = modifierLocalOf { Color.Unspecified }

        // Act.
        val modifierLocalMap =
            modifierLocalMapOf(stringModifierLocal to "first", colorModifierLocal to Color.Red)

        // Assert.
        assertThat(modifierLocalMap).isInstanceOf(MultiLocalMap::class.java)
        assertThat(modifierLocalMap[stringModifierLocal]).isEqualTo("first")
        assertThat(modifierLocalMap[colorModifierLocal]).isEqualTo(Color.Red)
    }

    @Test
    fun `modifier local map with three values`() {
        // Arrange.
        val stringModifierLocal = modifierLocalOf { "" }
        val colorModifierLocal = modifierLocalOf { Color.Unspecified }
        val directionModifierLocal = modifierLocalOf { LayoutDirection.Ltr }

        // Act.
        val modifierLocalMap =
            modifierLocalMapOf(
                stringModifierLocal to "first",
                colorModifierLocal to Color.Red,
                directionModifierLocal to LayoutDirection.Rtl
            )

        // Assert.
        assertThat(modifierLocalMap).isInstanceOf(MultiLocalMap::class.java)
        assertThat(modifierLocalMap[stringModifierLocal]).isEqualTo("first")
        assertThat(modifierLocalMap[colorModifierLocal]).isEqualTo(Color.Red)
        assertThat(modifierLocalMap[directionModifierLocal]).isEqualTo(LayoutDirection.Rtl)
    }
}
