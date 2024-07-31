/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MutableBoxTest {

    @Test
    fun defaultConstructor_shouldHaveAllZero() {
        val defaultBox = MutableBox()

        assertThat(defaultBox.xMin).isZero()
        assertThat(defaultBox.yMin).isZero()
        assertThat(defaultBox.xMax).isZero()
        assertThat(defaultBox.yMax).isZero()
    }

    @Test
    fun fillFromCenterAndDimensions_correctlyModifiesMutableBox() {
        val rect = MutableBox().fillFromCenterAndDimensions(ImmutablePoint(20f, -50f), 10f, 20f)

        assertThat(rect.xMin).isEqualTo(15f)
        assertThat(rect.xMax).isEqualTo(25f)
        assertThat(rect.yMin).isEqualTo(-60f)
        assertThat(rect.yMax).isEqualTo(-40f)
        assertThat(rect.width).isEqualTo(10f)
        assertThat(rect.height).isEqualTo(20f)
    }

    @Test
    fun fillFromTwoPoints_correctlyModifiesMutableBox() {
        val rect =
            MutableBox().fillFromTwoPoints(MutablePoint(20f, -50f), ImmutablePoint(-70f, 100f))

        assertThat(rect.xMin).isEqualTo(-70f)
        assertThat(rect.xMax).isEqualTo(20f)
        assertThat(rect.yMin).isEqualTo(-50f)
        assertThat(rect.yMax).isEqualTo(100f)
        assertThat(rect.width).isEqualTo(90f)
        assertThat(rect.height).isEqualTo(150f)
    }

    @Test
    fun minMaxFields_whenAllZeroes_allAreZero() {
        val zeroes = MutableBox().fillFromTwoPoints(ImmutablePoint(0F, 0F), ImmutablePoint(0F, 0F))
        assertThat(zeroes.xMin).isEqualTo(0F)
        assertThat(zeroes.yMin).isEqualTo(0F)
        assertThat(zeroes.xMax).isEqualTo(0F)
        assertThat(zeroes.yMax).isEqualTo(0F)
    }

    @Test
    fun minMaxFields_whenDeclaredInMinMaxOrder_matchOrder() {
        val inOrder =
            MutableBox().fillFromTwoPoints(ImmutablePoint(-1F, -2F), ImmutablePoint(3F, 4F))
        assertThat(inOrder.xMin).isEqualTo(-1F)
        assertThat(inOrder.yMin).isEqualTo(-2F)
        assertThat(inOrder.xMax).isEqualTo(3F)
        assertThat(inOrder.yMax).isEqualTo(4F)
    }

    @Test
    fun minMaxFields_whenDeclaredOutOfOrder_doNotMatchOrder() {
        val outOfOrder =
            MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(-3F, -4F))
        assertThat(outOfOrder.xMin).isEqualTo(-3F)
        assertThat(outOfOrder.yMin).isEqualTo(-4F)
        assertThat(outOfOrder.xMax).isEqualTo(1F)
        assertThat(outOfOrder.yMax).isEqualTo(2F)
    }

    @Test
    fun widthHeight_whenAllZeroes_areAllZero() {
        val zeroes = MutableBox().fillFromTwoPoints(ImmutablePoint(0F, 0F), ImmutablePoint(0F, 0F))

        assertThat(zeroes.width).isEqualTo(0)
        assertThat(zeroes.height).isEqualTo(0)
    }

    @Test
    fun widthHeight_whenDeclaredInOrder_areCorrectValues() {
        val inOrder =
            MutableBox().fillFromTwoPoints(ImmutablePoint(-1F, -2F), ImmutablePoint(3F, 4F))

        assertThat(inOrder.width).isEqualTo(4F)
        assertThat(inOrder.height).isEqualTo(6F)
    }

    @Test
    fun widthHeight_whenDeclaredOutOfOrder_areCorrectValues() {
        val outOfOrder =
            MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(-3F, -4F))

        assertThat(outOfOrder.width).isEqualTo(4F)
        assertThat(outOfOrder.height).isEqualTo(6F)
    }

    @Test
    fun widthHeight_whenValuesChanged_areCorrectValues() {
        val rect = MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(-3F, -4F))

        rect.fillFromTwoPoints(MutablePoint(-20f, -5f), ImmutablePoint(30f, 7f))

        assertThat(rect.width).isEqualTo(50F)
        assertThat(rect.height).isEqualTo(12F)
    }

    @Test
    fun setXBounds_whenInOrder_changesXMinAndXMax() {
        val rect = MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        rect.setXBounds(5F, 7F)

        assertThat(rect)
            .isEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(5F, 2F), ImmutablePoint(7F, 4F))
            )
    }

    @Test
    fun setXBounds_whenNotInOrder_changesXMinAndXMax() {
        val rect = MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        rect.setXBounds(7F, 5F)

        assertThat(rect)
            .isEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(5F, 2F), ImmutablePoint(7F, 4F))
            )
    }

    @Test
    fun setYBounds_whenInOrder_changesXMinAndXMax() {
        val rect = MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        rect.setYBounds(6F, 8F)

        assertThat(rect)
            .isEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 6F), ImmutablePoint(3F, 8F))
            )
    }

    @Test
    fun setYBounds_whenNotInOrder_changesXMinAndXMax() {
        val rect = MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        rect.setYBounds(8F, 6F)

        assertThat(rect)
            .isEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 6F), ImmutablePoint(3F, 8F))
            )
    }

    @Test
    fun populateFrom_correctlyPopulatesFromBox() {
        val source = ImmutableBox.fromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))
        val dest = MutableBox().populateFrom(source)

        assertThat(dest)
            .isEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))
            )
    }

    @Test
    fun equals_whenSameInstance_returnsTrueAndSameHashCode() {
        val rect = MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        assertThat(rect).isEqualTo(rect)
        assertThat(rect.hashCode()).isEqualTo(rect.hashCode())
    }

    @Test
    fun equals_whenDifferentType_returnsFalse() {
        val rect = MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        assertThat(rect).isNotEqualTo(ImmutablePoint(1F, 2F))
    }

    @Test
    fun equals_whenSameValues_returnsTrueAndSameHashCode() {
        val rect = MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))
        val other = MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        assertThat(rect).isEqualTo(other)
        assertThat(rect.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun equals_whenSameValuesOutOfOrder_returnsTrueAndSameHashCode() {
        val rect = MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))
        val other = MutableBox().fillFromTwoPoints(ImmutablePoint(3F, 4F), ImmutablePoint(1F, 2F))

        assertThat(rect).isEqualTo(other)
        assertThat(rect.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun equals_whenDifferentXMin_returnsFalse() {
        val rect = MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        assertThat(rect)
            .isNotEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(-1F, 2F), ImmutablePoint(3F, 4F))
            )
    }

    @Test
    fun equals_whenDifferentYMin_returnsFalse() {
        val rect = MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        assertThat(rect)
            .isNotEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(1F, -2F), ImmutablePoint(3F, 4F))
            )
    }

    @Test
    fun equals_whenDifferentXMax_returnsFalse() {
        val rect = MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        assertThat(rect)
            .isNotEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(30F, 4F))
            )
    }

    @Test
    fun equals_whenDifferentYMax_returnsFalse() {
        val rect = MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        assertThat(rect)
            .isNotEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 40F))
            )
    }

    @Test
    fun copy_returnsEqualValueThatCannotModifyOriginal() {
        val rect = MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        val copy = rect.copy()
        assertThat(copy).isEqualTo(rect)

        copy.fillFromTwoPoints(ImmutablePoint(5F, 6F), ImmutablePoint(7F, 8F))
        assertThat(rect)
            .isEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))
            )
    }

    @Test
    fun overwriteFromValues_whenInOrder_changesAllValues() {
        val rect = MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        rect.fillFromTwoPoints(ImmutablePoint(5F, 6F), ImmutablePoint(7F, 8F))

        assertThat(rect.xMin).isEqualTo(5F)
        assertThat(rect.yMin).isEqualTo(6F)
        assertThat(rect.xMax).isEqualTo(7F)
        assertThat(rect.yMax).isEqualTo(8F)
    }

    @Test
    fun overwriteFromValues_whenOutOfOrder_changesAllValues() {
        val rect = MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        rect.fillFromTwoPoints(ImmutablePoint(-1F, -2F), ImmutablePoint(-3F, -4F))

        assertThat(rect.xMin).isEqualTo(-3F)
        assertThat(rect.yMin).isEqualTo(-4F)
        assertThat(rect.xMax).isEqualTo(-1F)
        assertThat(rect.yMax).isEqualTo(-2F)
    }

    @Test
    fun center_modifiesMutablePoint() {
        val rect = MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 20F), ImmutablePoint(3F, 40F))
        val outCenter = MutablePoint()
        rect.center(outCenter)

        assertThat(outCenter).isEqualTo(MutablePoint(2F, 30F))
    }

    @Test
    fun corners_modifiesMutablePoints() {
        val rect = MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 20F), ImmutablePoint(3F, 40F))
        val p0 = MutablePoint()
        val p1 = MutablePoint()
        val p2 = MutablePoint()
        val p3 = MutablePoint()
        rect.corners(p0, p1, p2, p3)

        assertThat(p0).isEqualTo(MutablePoint(1F, 20F))
        assertThat(p1).isEqualTo(MutablePoint(3F, 20F))
        assertThat(p2).isEqualTo(MutablePoint(3F, 40F))
        assertThat(p3).isEqualTo(MutablePoint(1F, 40F))
    }

    @Test
    fun contains_returnsCorrectValuesWithPoint() {
        val rect =
            MutableBox().fillFromTwoPoints(ImmutablePoint(10F, 600F), ImmutablePoint(40F, 900F))
        val innerPoint = ImmutablePoint(30F, 700F)
        val outerPoint = ImmutablePoint(70F, 2000F)

        assertThat(rect.contains(innerPoint)).isTrue()
        assertThat(rect.contains(outerPoint)).isFalse()
    }

    @Test
    fun contains_returnsCorrectValuesWithBox() {
        val outerRect =
            MutableBox().fillFromTwoPoints(ImmutablePoint(10F, 600F), ImmutablePoint(40F, 900F))
        val innerRect =
            MutableBox().fillFromTwoPoints(ImmutablePoint(20F, 700F), ImmutablePoint(30F, 800F))

        assertThat(outerRect.contains(innerRect)).isTrue()
        assertThat(innerRect.contains(outerRect)).isFalse()
    }
}
