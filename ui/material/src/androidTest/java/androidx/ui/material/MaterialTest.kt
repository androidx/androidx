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

package androidx.ui.material

import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.Density
import androidx.ui.core.DensityReceiver
import androidx.ui.core.Dp
import androidx.ui.core.IntPx
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.PxSize
import androidx.ui.core.Size
import androidx.ui.core.dp
import androidx.ui.core.round
import androidx.ui.core.withDensity
import androidx.ui.layout.ConstrainedBox
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.Wrap
import androidx.ui.material.surface.Surface
import androidx.ui.test.ComposeTestRule
import com.google.common.truth.Truth

fun ComposeTestRule.setMaterialContent(composable: @Composable() () -> Unit) {
    setContent {
        MaterialTheme {
            Surface {
                composable()
            }
        }
    }
}

private val BigConstraints = DpConstraints(maxWidth = 5000.dp, maxHeight = 5000.dp)

fun ComposeTestRule.setMaterialContentAndTestSizes(
    parentConstraints: DpConstraints = BigConstraints,
    children: @Composable() () -> Unit
): SizeTestSpec {
    var realSize: PxSize? = null
    setMaterialContent {
        Wrap {
            ConstrainedBox(constraints = parentConstraints) {
                OnChildPositioned(onPositioned = { coordinates ->
                    realSize = coordinates.size
                }) {
                    children()
                }
            }
        }
    }
    return SizeTestSpec(realSize!!, density)
}

fun ComposeTestRule.setMaterialContentAndCollectDpSize(
    parentConstraints: DpConstraints = BigConstraints,
    children: @Composable() () -> Unit
): Size {
    return withDensity(density) {
        val pxSize = setMaterialContentAndCollectPixelSize(parentConstraints, children)
        Size(pxSize.width.toDp(), pxSize.height.toDp())
    }
}

fun ComposeTestRule.setMaterialContentAndCollectPixelSize(
    parentConstraints: DpConstraints = BigConstraints,
    children: @Composable() () -> Unit
): PxSize {
    var realSize: PxSize? = null
    setMaterialContent {
        Wrap {
            ConstrainedBox(constraints = parentConstraints) {
                OnChildPositioned(onPositioned = { coordinates ->
                    realSize = coordinates.size
                }) {
                    children()
                }
            }
        }
    }
    return realSize!!
}

class SizeTestSpec(private val size: PxSize, private val density: Density) {
    fun assertHeightEqualsTo(expectedHeight: Dp) =
        assertHeightEqualsTo { expectedHeight.toIntPx() }

    fun assertWidthEqualsTo(expectedWidth: Dp): SizeTestSpec =
        assertWidthEqualsTo { expectedWidth.toIntPx() }

    fun assertIsSquareWithSize(expectedSize: Dp) = assertIsSquareWithSize { expectedSize.toIntPx() }

    fun assertWidthEqualsTo(expectedWidthPx: DensityReceiver.() -> IntPx): SizeTestSpec {
        val widthPx = withDensity(density) {
            expectedWidthPx()
        }
        Truth.assertThat(size.width.round()).isEqualTo(widthPx)
        return this
    }

    fun assertHeightEqualsTo(expectedHeightPx: DensityReceiver.() -> IntPx): SizeTestSpec {
        val heightPx = withDensity(density) {
            expectedHeightPx()
        }
        Truth.assertThat(size.height.round()).isEqualTo(heightPx)
        return this
    }

    fun assertIsSquareWithSize(expectedSquarePx: DensityReceiver.() -> IntPx): SizeTestSpec {
        val squarePx = withDensity(density) {
            expectedSquarePx()
        }
        Truth.assertThat(size.width.round()).isEqualTo(squarePx)
        Truth.assertThat(size.height.round()).isEqualTo(squarePx)
        return this
    }
}