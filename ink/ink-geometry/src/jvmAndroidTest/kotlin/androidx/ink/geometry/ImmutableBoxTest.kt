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
        val rect = ImmutableBox.fromCenterAndDimensions(ImmutableVec(20f, -50f), 10f, 20f)

        assertThat(rect.xMin).isEqualTo(15f)
        assertThat(rect.xMax).isEqualTo(25f)
        assertThat(rect.yMin).isEqualTo(-60f)
        assertThat(rect.yMax).isEqualTo(-40f)
        assertThat(rect.width).isEqualTo(10f)
        assertThat(rect.height).isEqualTo(20f)
    }

    @Test
    fun fromTwoPoints_constructsCorrectImmutableBox() {
        val rect = ImmutableBox.fromTwoPoints(ImmutableVec(20f, -50f), MutableVec(-70f, 100f))

        assertThat(rect.xMin).isEqualTo(-70f)
        assertThat(rect.xMax).isEqualTo(20f)
        assertThat(rect.yMin).isEqualTo(-50f)
        assertThat(rect.yMax).isEqualTo(100f)
        assertThat(rect.width).isEqualTo(90f)
        assertThat(rect.height).isEqualTo(150f)
    }

    @Test
    fun minMaxFields_whenAllZeroes_allAreZero() {
        val zeroes = ImmutableBox.fromTwoPoints(ImmutableVec(0F, 0F), ImmutableVec(0F, 0F))
        assertThat(zeroes.xMin).isEqualTo(0F)
        assertThat(zeroes.yMin).isEqualTo(0F)
        assertThat(zeroes.xMax).isEqualTo(0F)
        assertThat(zeroes.yMax).isEqualTo(0F)
    }

    @Test
    fun minMaxFields_whenDeclaredInMinMaxOrder_matchOrder() {
        val inOrder = ImmutableBox.fromTwoPoints(ImmutableVec(-1F, -2F), ImmutableVec(3F, 4F))
        assertThat(inOrder.xMin).isEqualTo(-1F)
        assertThat(inOrder.yMin).isEqualTo(-2F)
        assertThat(inOrder.xMax).isEqualTo(3F)
        assertThat(inOrder.yMax).isEqualTo(4F)
    }

    @Test
    fun minMaxFields_whenDeclaredOutOfOrder_doNotMatchOrder() {
        val outOfOrder = ImmutableBox.fromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(-3F, -4F))
        assertThat(outOfOrder.xMin).isEqualTo(-3F)
        assertThat(outOfOrder.yMin).isEqualTo(-4F)
        assertThat(outOfOrder.xMax).isEqualTo(1F)
        assertThat(outOfOrder.yMax).isEqualTo(2F)
    }

    @Test
    fun widthHeight_whenAllZeroes_areAllZero() {
        val zeroes = ImmutableBox.fromTwoPoints(ImmutableVec(0F, 0F), ImmutableVec(0F, 0F))

        assertThat(zeroes.width).isEqualTo(0)
        assertThat(zeroes.height).isEqualTo(0)
    }

    @Test
    fun widthHeight_whenDeclaredInOrder_areCorrectValues() {
        val inOrder = ImmutableBox.fromTwoPoints(ImmutableVec(-1F, -2F), ImmutableVec(3F, 4F))

        assertThat(inOrder.width).isEqualTo(4F)
        assertThat(inOrder.height).isEqualTo(6F)
    }

    @Test
    fun widthHeight_whenDeclaredOutOfOrder_areCorrectValues() {
        val outOfOrder = ImmutableBox.fromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(-3F, -4F))

        assertThat(outOfOrder.width).isEqualTo(4F)
        assertThat(outOfOrder.height).isEqualTo(6F)
    }

    @Test
    fun equals_whenSameInstance_returnsTrueAndSameHashCode() {
        val immutableBox = ImmutableBox.fromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(3F, 4F))

        assertThat(immutableBox).isEqualTo(immutableBox)
        assertThat(immutableBox.hashCode()).isEqualTo(immutableBox.hashCode())
    }

    @Test
    fun equals_whenDifferentType_returnsFalse() {
        val immutableBox = ImmutableBox.fromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(3F, 4F))

        assertThat(immutableBox).isNotEqualTo(ImmutableVec(1F, 2F))
    }

    @Test
    fun equals_whenSameInterfacePropertiesAndDifferentType_returnsTrue() {
        val point1 = ImmutableVec(1F, 2F)
        val point2 = ImmutableVec(3F, 4F)
        val immutableBox = ImmutableBox.fromTwoPoints(point1, point2)
        val mutableBox = MutableBox().populateFromTwoPoints(point1, point2)

        assertThat(immutableBox).isEqualTo(mutableBox)
        assertThat(immutableBox.hashCode()).isEqualTo(mutableBox.hashCode())
    }

    @Test
    fun equals_whenSameValues_returnsTrueAndSameHashCode() {
        val immutableBox = ImmutableBox.fromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(3F, 4F))
        val other = ImmutableBox.fromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(3F, 4F))

        assertThat(immutableBox).isEqualTo(other)
        assertThat(immutableBox.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun equals_whenSameValuesOutOfOrder_returnsTrueAndSameHashCode() {
        val immutableBox = ImmutableBox.fromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(3F, 4F))
        val other = ImmutableBox.fromTwoPoints(ImmutableVec(3F, 4F), ImmutableVec(1F, 2F))

        assertThat(immutableBox).isEqualTo(other)
        assertThat(immutableBox.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun equals_whenDifferentXMin_returnsFalse() {
        val immutableBox = ImmutableBox.fromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(3F, 4F))

        assertThat(immutableBox)
            .isNotEqualTo(ImmutableBox.fromTwoPoints(ImmutableVec(-1F, 2F), ImmutableVec(3F, 4F)))
    }

    @Test
    fun equals_whenDifferentYMin_returnsFalse() {
        val immutableBox = ImmutableBox.fromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(3F, 4F))

        assertThat(immutableBox)
            .isNotEqualTo(ImmutableBox.fromTwoPoints(ImmutableVec(1F, -2F), ImmutableVec(3F, 4F)))
    }

    @Test
    fun equals_whenDifferentXMax_returnsFalse() {
        val immutableBox = ImmutableBox.fromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(3F, 4F))

        assertThat(immutableBox)
            .isNotEqualTo(ImmutableBox.fromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(30F, 4F)))
    }

    @Test
    fun equals_whenDifferentYMax_returnsFalse() {
        val immutableBox = ImmutableBox.fromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(3F, 4F))

        assertThat(immutableBox)
            .isNotEqualTo(ImmutableBox.fromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(3F, 40F)))
    }

    @Test
    fun computeCenter_modifiesMutablePoint() {
        val immutableBox = ImmutableBox.fromTwoPoints(ImmutableVec(1F, 20F), ImmutableVec(3F, 40F))
        val outCenter = MutableVec()
        immutableBox.computeCenter(outCenter)

        assertThat(outCenter).isEqualTo(MutableVec(2F, 30F))
    }

    @Test
    fun computeCenter_returnsCorrectValue() {
        val immutableBox = ImmutableBox.fromTwoPoints(ImmutableVec(1F, 20F), ImmutableVec(3F, 40F))
        val centerVec = immutableBox.computeCenter()

        assertThat(centerVec).isEqualTo(MutableVec(2F, 30F))
    }

    @Test
    fun computeCorners_modifiesMutableVecs() {
        val rect = ImmutableBox.fromTwoPoints(ImmutableVec(1F, 20F), ImmutableVec(3F, 40F))
        val p0 = MutableVec()
        val p1 = MutableVec()
        val p2 = MutableVec()
        val p3 = MutableVec()
        rect.computeCorners(p0, p1, p2, p3)

        assertThat(p0).isEqualTo(MutableVec(1F, 20F))
        assertThat(p1).isEqualTo(MutableVec(3F, 20F))
        assertThat(p2).isEqualTo(MutableVec(3F, 40F))
        assertThat(p3).isEqualTo(MutableVec(1F, 40F))
    }

    @Test
    fun computeCorners_returnsCorrectValues() {
        val rect = ImmutableBox.fromTwoPoints(ImmutableVec(1F, 20F), ImmutableVec(3F, 40F))
        val cornersList = rect.computeCorners()

        assertThat(cornersList).hasSize(4)
        assertThat(cornersList.get(0)).isEqualTo(MutableVec(1F, 20F))
        assertThat(cornersList.get(1)).isEqualTo(MutableVec(3F, 20F))
        assertThat(cornersList.get(2)).isEqualTo(MutableVec(3F, 40F))
        assertThat(cornersList.get(3)).isEqualTo(MutableVec(1F, 40F))
    }

    @Test
    fun contains_returnsCorrectValuesWithPoint() {
        val rect = ImmutableBox.fromTwoPoints(ImmutableVec(10F, 600F), ImmutableVec(40F, 900F))
        val innerPoint = ImmutableVec(30F, 700F)
        val outerPoint = ImmutableVec(70F, 2000F)

        assertThat(rect.contains(innerPoint)).isTrue()
        assertThat(rect.contains(outerPoint)).isFalse()
    }

    @Test
    fun contains_returnsCorrectValuesWithBox() {
        val outerRect = ImmutableBox.fromTwoPoints(ImmutableVec(10F, 600F), ImmutableVec(40F, 900F))
        val innerRect = ImmutableBox.fromTwoPoints(ImmutableVec(20F, 700F), ImmutableVec(30F, 800F))

        assertThat(outerRect.contains(innerRect)).isTrue()
        assertThat(innerRect.contains(outerRect)).isFalse()
    }

    @Test
    fun toImmutable_returnsSelf() {
        val box = ImmutableBox.fromTwoPoints(ImmutableVec(10F, 20F), ImmutableVec(30F, 40F))
        val output = box.toImmutable()

        assertThat(output).isSameInstanceAs(box)
    }
}
