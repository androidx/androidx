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
package androidx.compose.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.IntSize
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@MediumTest
class InputInLayerTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun reusedLayerIsReset() {
        val tag = "Outer Box"
        var size = IntSize.Zero
        var showRed by mutableStateOf(true)
        var rootPoint = Offset.Zero
        lateinit var outerLayoutCoordinates: LayoutCoordinates
        rule.setContent {
            Box(
                Modifier.fillMaxSize().testTag(tag).onPlaced {
                    size = it.size
                    outerLayoutCoordinates = it
                }
            ) {
                if (showRed) {
                    Spacer(
                        modifier =
                            Modifier.graphicsLayer(scaleY = 0.5f)
                                .fillMaxSize()
                                .background(Color.Red)
                                .clickable { showRed = false }
                                .onPlaced {
                                    val matrix = Matrix()
                                    outerLayoutCoordinates.transformFrom(it, matrix)
                                    rootPoint = matrix.map(Offset.Zero)
                                },
                    )
                } else {
                    Spacer(
                        modifier =
                            Modifier.graphicsLayer()
                                .fillMaxSize()
                                .background(Color.Blue)
                                .clickable { showRed = true }
                                .onPlaced {
                                    val matrix = Matrix()
                                    outerLayoutCoordinates.transformFrom(it, matrix)
                                    rootPoint = matrix.map(Offset.Zero)
                                },
                    )
                }
            }
        }
        rule.waitForIdle()
        assertThat(showRed).isTrue()
        assertThat(rootPoint.y).isWithin(1f).of(size.height / 4f)
        rule.onNodeWithTag(tag).performTouchInput {
            down(Offset(size.width / 2f, size.height / 2f))
            up()
        }
        rule.waitForIdle()
        assertThat(showRed).isFalse()
        assertThat(rootPoint.y).isWithin(1f).of(0f)
        rule.onNodeWithTag(tag).performTouchInput {
            down(Offset.Zero)
            up()
        }
        rule.waitForIdle()
        assertThat(showRed).isTrue()
        assertThat(rootPoint.y).isWithin(1f).of(size.height / 4f)
    }
}
