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

package androidx.ui.baseui.shape.corner

import androidx.test.filters.SmallTest
import androidx.ui.baseui.shape.Shape
import androidx.ui.core.Density
import androidx.ui.core.PxSize
import androidx.ui.core.px
import androidx.ui.core.toRect
import androidx.ui.core.withDensity
import androidx.ui.engine.geometry.Outline
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Radius
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class RoundedCornerShapeTest {

    private val density = Density(2f)
    private val size = PxSize(100.px, 150.px)

    @Test
    fun roundedUniformCorners() {
        val rounded = RoundedCornerShape(CornerSizes(25))

        val expectedRadius = Radius.circular(25f)
        val outline = rounded.toOutline() as Outline.Rounded
        Truth.assertThat(outline.rrect).isEqualTo(
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
        val rounded = RoundedCornerShape(
            CornerSizes(
                CornerSize(radius1.px),
                CornerSize(radius2.px),
                CornerSize(radius3.px),
                CornerSize(radius4.px)
            )
        )

        val outline = rounded.toOutline() as Outline.Rounded
        Truth.assertThat(outline.rrect).isEqualTo(
            RRect(
                size.toRect(),
                Radius.circular(radius1),
                Radius.circular(radius2),
                Radius.circular(radius3),
                Radius.circular(radius4)
            )
        )
    }

    private fun Shape.toOutline() = withDensity(density) {
        createOutline(size)
    }
}
