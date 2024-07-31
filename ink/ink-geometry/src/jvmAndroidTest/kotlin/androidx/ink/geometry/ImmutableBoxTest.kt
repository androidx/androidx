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
class ImmutableBoxTest {

    @Test
    fun fromCenterAndDimensions_constructsCorrectImmutableBox() {
        val rect = ImmutableBox.fromCenterAndDimensions(ImmutablePoint(20f, -50f), 10f, 20f)

        assertThat(rect.xMin).isEqualTo(15f)
        assertThat(rect.xMax).isEqualTo(25f)
        assertThat(rect.yMin).isEqualTo(-60f)
        assertThat(rect.yMax).isEqualTo(-40f)
        assertThat(rect.width).isEqualTo(10f)
        assertThat(rect.height).isEqualTo(20f)
    }

    @Test
    fun fromTwoPoints_constructsCorrectImmutableBox() {
        val rect = ImmutableBox.fromTwoPoints(ImmutablePoint(20f, -50f), MutablePoint(-70f, 100f))

        assertThat(rect.xMin).isEqualTo(-70f)
        assertThat(rect.xMax).isEqualTo(20f)
        assertThat(rect.yMin).isEqualTo(-50f)
        assertThat(rect.yMax).isEqualTo(100f)
        assertThat(rect.width).isEqualTo(90f)
        assertThat(rect.height).isEqualTo(150f)
    }

    @Test
    fun minMaxFields_whenAllZeroes_allAreZero() {
        val zeroes = ImmutableBox.fromTwoPoints(ImmutablePoint(0F, 0F), ImmutablePoint(0F, 0F))
        assertThat(zeroes.xMin).isEqualTo(0F)
        assertThat(zeroes.yMin).isEqualTo(0F)
        assertThat(zeroes.xMax).isEqualTo(0F)
        assertThat(zeroes.yMax).isEqualTo(0F)
    }

    @Test
    fun minMaxFields_whenDeclaredInMinMaxOrder_matchOrder() {
        val inOrder = ImmutableBox.fromTwoPoints(ImmutablePoint(-1F, -2F), ImmutablePoint(3F, 4F))
        assertThat(inOrder.xMin).isEqualTo(-1F)
        assertThat(inOrder.yMin).isEqualTo(-2F)
        assertThat(inOrder.xMax).isEqualTo(3F)
        assertThat(inOrder.yMax).isEqualTo(4F)
    }

    @Test
    fun minMaxFields_whenDeclaredOutOfOrder_doNotMatchOrder() {
        val outOfOrder =
            ImmutableBox.fromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(-3F, -4F))
        assertThat(outOfOrder.xMin).isEqualTo(-3F)
        assertThat(outOfOrder.yMin).isEqualTo(-4F)
        assertThat(outOfOrder.xMax).isEqualTo(1F)
        assertThat(outOfOrder.yMax).isEqualTo(2F)
    }

    @Test
    fun widthHeight_whenAllZeroes_areAllZero() {
        val zeroes = ImmutableBox.fromTwoPoints(ImmutablePoint(0F, 0F), ImmutablePoint(0F, 0F))

        assertThat(zeroes.width).isEqualTo(0)
        assertThat(zeroes.height).isEqualTo(0)
    }

    @Test
    fun widthHeight_whenDeclaredInOrder_areCorrectValues() {
        val inOrder = ImmutableBox.fromTwoPoints(ImmutablePoint(-1F, -2F), ImmutablePoint(3F, 4F))

        assertThat(inOrder.width).isEqualTo(4F)
        assertThat(inOrder.height).isEqualTo(6F)
    }

    @Test
    fun widthHeight_whenDeclaredOutOfOrder_areCorrectValues() {
        val outOfOrder =
            ImmutableBox.fromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(-3F, -4F))

        assertThat(outOfOrder.width).isEqualTo(4F)
        assertThat(outOfOrder.height).isEqualTo(6F)
    }

    @Test
    fun equals_whenSameInstance_returnsTrueAndSameHashCode() {
        val immutableBox =
            ImmutableBox.fromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        assertThat(immutableBox).isEqualTo(immutableBox)
        assertThat(immutableBox.hashCode()).isEqualTo(immutableBox.hashCode())
    }

    @Test
    fun equals_whenDifferentType_returnsFalse() {
        val immutableBox =
            ImmutableBox.fromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        assertThat(immutableBox).isNotEqualTo(ImmutablePoint(1F, 2F))
    }

    @Test
    fun equals_whenSameInterfacePropertiesAndDifferentType_returnsTrue() {
        val point1 = ImmutablePoint(1F, 2F)
        val point2 = ImmutablePoint(3F, 4F)
        val immutableBox = ImmutableBox.fromTwoPoints(point1, point2)
        val mutableBox = MutableBox().fillFromTwoPoints(point1, point2)

        assertThat(immutableBox).isEqualTo(mutableBox)
        assertThat(immutableBox.hashCode()).isEqualTo(mutableBox.hashCode())
    }

    @Test
    fun equals_whenSameValues_returnsTrueAndSameHashCode() {
        val immutableBox =
            ImmutableBox.fromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))
        val other = ImmutableBox.fromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        assertThat(immutableBox).isEqualTo(other)
        assertThat(immutableBox.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun equals_whenSameValuesOutOfOrder_returnsTrueAndSameHashCode() {
        val immutableBox =
            ImmutableBox.fromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))
        val other = ImmutableBox.fromTwoPoints(ImmutablePoint(3F, 4F), ImmutablePoint(1F, 2F))

        assertThat(immutableBox).isEqualTo(other)
        assertThat(immutableBox.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun equals_whenDifferentXMin_returnsFalse() {
        val immutableBox =
            ImmutableBox.fromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        assertThat(immutableBox)
            .isNotEqualTo(
                ImmutableBox.fromTwoPoints(ImmutablePoint(-1F, 2F), ImmutablePoint(3F, 4F))
            )
    }

    @Test
    fun equals_whenDifferentYMin_returnsFalse() {
        val immutableBox =
            ImmutableBox.fromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        assertThat(immutableBox)
            .isNotEqualTo(
                ImmutableBox.fromTwoPoints(ImmutablePoint(1F, -2F), ImmutablePoint(3F, 4F))
            )
    }

    @Test
    fun equals_whenDifferentXMax_returnsFalse() {
        val immutableBox =
            ImmutableBox.fromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        assertThat(immutableBox)
            .isNotEqualTo(
                ImmutableBox.fromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(30F, 4F))
            )
    }

    @Test
    fun equals_whenDifferentYMax_returnsFalse() {
        val immutableBox =
            ImmutableBox.fromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        assertThat(immutableBox)
            .isNotEqualTo(
                ImmutableBox.fromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 40F))
            )
    }

    @Test
    fun newMutable_matchesValues() {
        val immutableBox =
            ImmutableBox.fromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        assertThat(immutableBox.newMutable())
            .isEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))
            )
    }

    @Test
    fun fillMutable_correctlyModifiesOutput() {
        val immutableBox =
            ImmutableBox.fromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))
        val output = MutableBox()

        immutableBox.fillMutable(output)

        assertThat(output)
            .isEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))
            )
    }

    @Test
    fun center_modifiesMutablePoint() {
        val immutableBox =
            ImmutableBox.fromTwoPoints(ImmutablePoint(1F, 20F), ImmutablePoint(3F, 40F))
        val outCenter = MutablePoint()
        immutableBox.center(outCenter)

        assertThat(outCenter).isEqualTo(MutablePoint(2F, 30F))
    }

    @Test
    fun corners_modifiesMutablePoints() {
        val rect = ImmutableBox.fromTwoPoints(ImmutablePoint(1F, 20F), ImmutablePoint(3F, 40F))
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
        val rect = ImmutableBox.fromTwoPoints(ImmutablePoint(10F, 600F), ImmutablePoint(40F, 900F))
        val innerPoint = ImmutablePoint(30F, 700F)
        val outerPoint = ImmutablePoint(70F, 2000F)

        assertThat(rect.contains(innerPoint)).isTrue()
        assertThat(rect.contains(outerPoint)).isFalse()
    }

    @Test
    fun contains_returnsCorrectValuesWithBox() {
        val outerRect =
            ImmutableBox.fromTwoPoints(ImmutablePoint(10F, 600F), ImmutablePoint(40F, 900F))
        val innerRect =
            ImmutableBox.fromTwoPoints(ImmutablePoint(20F, 700F), ImmutablePoint(30F, 800F))

        assertThat(outerRect.contains(innerRect)).isTrue()
        assertThat(innerRect.contains(outerRect)).isFalse()
    }

    @Test
    fun copy_withNoArguments_returnsThis() {
        val original = ImmutableBox.fromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))

        assertThat(original.copy()).isSameInstanceAs(original)
    }

    @Test
    fun copy_withArguments_makesCopy() {
        val x1 = 1F
        val y1 = 2F
        val x2 = 3F
        val y2 = 4F
        val original = ImmutableBox.fromTwoPoints(ImmutablePoint(x1, y1), ImmutablePoint(x2, y2))
        // Different values that won't result in the min/max in either x or y dimension flipping.
        val differentX1 = 0.5F
        val differentY1 = 1.5F
        val differentX2 = 2.5F
        val differentY2 = 3.5F

        // Change all values.
        assertThat(original.copy(differentX1, differentY1, differentX2, differentY2))
            .isEqualTo(
                ImmutableBox.fromTwoPoints(
                    ImmutablePoint(differentX1, differentY1),
                    ImmutablePoint(differentX2, differentY2),
                )
            )

        // Change x1.
        assertThat(original.copy(x1 = differentX1))
            .isEqualTo(
                ImmutableBox.fromTwoPoints(ImmutablePoint(differentX1, y1), ImmutablePoint(x2, y2))
            )

        // Change y1.
        assertThat(original.copy(y1 = differentY1))
            .isEqualTo(
                ImmutableBox.fromTwoPoints(ImmutablePoint(x1, differentY1), ImmutablePoint(x2, y2))
            )

        // Change x2.
        assertThat(original.copy(x2 = differentX2))
            .isEqualTo(
                ImmutableBox.fromTwoPoints(ImmutablePoint(x1, y1), ImmutablePoint(differentX2, y2))
            )

        // Change y2.
        assertThat(original.copy(y2 = differentY2))
            .isEqualTo(
                ImmutableBox.fromTwoPoints(ImmutablePoint(x1, y1), ImmutablePoint(x2, differentY2))
            )
    }

    @Test
    fun copy_withArgumentsThatReverseBounds_makesCopyWith() {
        val x1 = 1F
        val y1 = 2F
        val x2 = 3F
        val y2 = 4F
        val original = ImmutableBox.fromTwoPoints(ImmutablePoint(x1, y1), ImmutablePoint(x2, y2))
        // Different value that results in the min/max in x dimension flipping.
        val differentX1 = 5F

        // Change x1 will flip x1 and x2 values.
        assertThat(original.copy(x1 = differentX1))
            .isEqualTo(
                ImmutableBox.fromTwoPoints(ImmutablePoint(x2, y1), ImmutablePoint(differentX1, y2))
            )
    }
}
