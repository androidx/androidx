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

package androidx.ui.material.shape

import androidx.test.filters.SmallTest
import androidx.ui.baseui.shape.Shape
import androidx.ui.baseui.shape.corner.CornerSize
import androidx.ui.baseui.shape.corner.CornerSizes
import androidx.ui.core.Density
import androidx.ui.core.PxSize
import androidx.ui.core.px
import androidx.ui.core.withDensity
import androidx.ui.engine.geometry.Outline
import androidx.ui.painting.Path
import androidx.ui.painting.PathOperation
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class CutCornerShapeTest {

    private val density = Density(2f)
    private val size = PxSize(100.px, 150.px)

    @Test
    fun cutCornersUniformCorners() {
        val cut = CutCornerShape(
            CornerSizes(
                CornerSize(
                    10.px
                )
            )
        )

        val outline = cut.toOutline() as Outline.Generic
        assertPathsEquals(outline.path, Path().apply {
            moveTo(0f, 10f)
            lineTo(10f, 0f)
            lineTo(90f, 0f)
            lineTo(100f, 10f)
            lineTo(100f, 140f)
            lineTo(90f, 150f)
            lineTo(10f, 150f)
            lineTo(0f, 140f)
            close()
        })
    }

    @Test
    fun cutCornersDifferentCorners() {
        val size1 = 12f
        val size2 = 22f
        val size3 = 32f
        val size4 = 42f
        val cut = CutCornerShape(
            CornerSizes(
                CornerSize(size1.px),
                CornerSize(size2.px),
                CornerSize(size3.px),
                CornerSize(size4.px)
            )
        )

        val outline = cut.toOutline() as Outline.Generic
        assertPathsEquals(outline.path, Path().apply {
            moveTo(0f, 12f)
            lineTo(12f, 0f)
            lineTo(78f, 0f)
            lineTo(100f, 22f)
            lineTo(100f, 118f)
            lineTo(68f, 150f)
            lineTo(42f, 150f)
            lineTo(0f, 108f)
            close()
        })
    }

    private fun Shape.toOutline() = withDensity(density) {
        createOutline(size)
    }
}

fun assertPathsEquals(path1: Path, path2: Path) {
    val diff = Path()
    val reverseDiff = Path()
    Assert.assertTrue(
        diff.op(path1, path2, PathOperation.difference) &&
                reverseDiff.op(path2, path1, PathOperation.difference) &&
                diff.isEmpty &&
                reverseDiff.isEmpty
    )
}
