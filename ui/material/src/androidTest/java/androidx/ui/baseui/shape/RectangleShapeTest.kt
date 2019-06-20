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

package androidx.ui.baseui.shape

import androidx.test.filters.SmallTest
import androidx.ui.core.Density
import androidx.ui.core.PxSize
import androidx.ui.core.px
import androidx.ui.core.toRect
import androidx.ui.core.withDensity
import androidx.ui.engine.geometry.Outline
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class RectangleShapeTest {

    private val density = Density(2f)
    private val size = PxSize(100.px, 150.px)

    @Test
    fun rectangularShapeWithCorrectSize() {
        val rectangular = RectangleShape()

        val outline = rectangular.toOutline() as Outline.Rectangle
        Truth.assertThat(outline.rect).isEqualTo(size.toRect())
    }

    private fun Shape.toOutline() = withDensity(density) {
        createOutline(size)
    }
}
