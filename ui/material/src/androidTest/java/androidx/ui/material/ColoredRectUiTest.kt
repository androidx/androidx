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

import androidx.test.filters.MediumTest
import androidx.ui.baseui.ColoredRect
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.PxSize
import androidx.ui.core.dp
import androidx.ui.core.round
import androidx.ui.core.withDensity
import androidx.ui.layout.Center
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.graphics.Color
import androidx.ui.test.android.AndroidUiTestRunner
import com.google.common.truth.Truth
import androidx.compose.composer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class ColoredRectUiTest : AndroidUiTestRunner() {

    private val bigConstraints = DpConstraints(
        maxWidth = 5000.dp,
        maxHeight = 5000.dp
    )

    private val color = Color(0xFFFF0000.toInt())

    @Test
    fun coloredRect_fixedSizes() {
        var size: PxSize? = null
        val width = 40.dp
        val height = 71.dp

        setMaterialContent {
            Center {
                Container(constraints = bigConstraints) {
                    OnChildPositioned(onPositioned = { position ->
                        size = position.size
                    }) {
                        ColoredRect(width = width, height = height, color = color)
                    }
                }
            }
        }
        withDensity(density) {
            Truth.assertThat(size?.height?.round()).isEqualTo(height.toIntPx())
            Truth.assertThat(size?.width?.round()).isEqualTo(width.toIntPx())
        }
    }

    @Test
    fun coloredRect_expand_LimitedSizes() {
        var size: PxSize? = null
        val width = 40.dp
        val height = 71.dp

        setMaterialContent {
            Center {
                Container(width = width, height = height) {
                    OnChildPositioned(onPositioned = { position ->
                        size = position.size
                    }) {
                        ColoredRect(color = color)
                    }
                }
            }
        }
        withDensity(density) {
            Truth.assertThat(size?.height?.round()).isEqualTo(height.toIntPx())
            Truth.assertThat(size?.width?.round()).isEqualTo(width.toIntPx())
        }
    }

    @Test
    fun coloredRect_expand_WholeScreenSizes() {
        var size: PxSize? = null

        setMaterialContent {
            Center {
                Container(constraints = bigConstraints) {
                    OnChildPositioned(onPositioned = { position ->
                        size = position.size
                    }) {
                        ColoredRect(color = color)
                    }
                }
            }
        }
        val dm = activityTestRule.activity.resources.displayMetrics
        withDensity(density) {
            Truth.assertThat(size?.height?.round()?.value).isEqualTo(dm.heightPixels)
            Truth.assertThat(size?.width?.round()?.value).isEqualTo(dm.widthPixels)
        }
    }
}