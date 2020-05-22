/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.foundation.shape.corner

import androidx.test.filters.SmallTest
import androidx.ui.geometry.RRect
import androidx.ui.geometry.Radius
import androidx.ui.geometry.Size
import androidx.ui.geometry.toRect
import androidx.ui.graphics.Outline
import androidx.ui.graphics.Shape
import androidx.ui.unit.Density
import androidx.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class RoundedCornerShapeTest {

    private val density = Density(2f)
    private val size = Size(100.0f, 150.0f)

    @Test
    fun roundedUniformCorners() {
        val rounded = RoundedCornerShape(25)

        val expectedRadius = Radius.circular(25f)
        val outline = rounded.toOutline() as Outline.Rounded
        assertThat(outline.rrect).isEqualTo(
            RRect(
                size.toRect(), expectedRadius
            )
        )
    }

    @Test
    fun roundedDifferentRadius() {
        val radius1 = 12f
        val radius2 = 22f
        val radius3 = 32f
        val radius4 = 42f
        val rounded = RoundedCornerShape(radius1, radius2, radius3, radius4)

        val outline = rounded.toOutline() as Outline.Rounded
        assertThat(outline.rrect).isEqualTo(
            RRect(
                size.toRect(),
                Radius.circular(radius1),
                Radius.circular(radius2),
                Radius.circular(radius3),
                Radius.circular(radius4)
            )
        )
    }

    @Test
    fun createsRectangleOutlineForZeroSizedCorners() {
        val rounded = RoundedCornerShape(0.0f, 0.0f, 0.0f, 0.0f)

        assertThat(rounded.toOutline())
            .isEqualTo(Outline.Rectangle(size.toRect()))
    }

    @Test
    fun roundedCornerShapesAreEquals() {
        assertThat(RoundedCornerShape(12.dp))
            .isEqualTo(RoundedCornerShape(12.dp))
    }

    @Test
    fun roundedCornerUpdateAllCornerSize() {
        assertThat(RoundedCornerShape(10.0f).copy(CornerSize(5.dp)))
            .isEqualTo(RoundedCornerShape(5.dp))
    }

    @Test
    fun roundedCornerUpdateTwoCornerSizes() {
        val original = RoundedCornerShape(10.0f).copy(
            topLeft = CornerSize(3.dp),
            bottomLeft = CornerSize(50)
        )

        assertEquals(CornerSize(3.dp), original.topLeft)
        assertEquals(CornerSize(10.0f), original.topRight)
        assertEquals(CornerSize(10.0f), original.bottomRight)
        assertEquals(CornerSize(50), original.bottomLeft)
        assertThat(RoundedCornerShape(10.0f).copy(
            topLeft = CornerSize(3.dp),
            bottomLeft = CornerSize(50)
        )).isEqualTo(RoundedCornerShape(
            topLeft = CornerSize(3.dp),
            topRight = CornerSize(10.0f),
            bottomRight = CornerSize(10.0f),
            bottomLeft = CornerSize(50)
        ))
    }

    private fun Shape.toOutline() = createOutline(size, density)
}
