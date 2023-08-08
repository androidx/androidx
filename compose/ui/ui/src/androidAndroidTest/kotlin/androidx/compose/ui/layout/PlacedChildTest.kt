/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.layout

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class PlacedChildTest {

    private val Tag = "tag"

    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    @Test
    fun remeasureNotPlacedChild() {
        val root = root {
            measurePolicy = UseChildSizeButNotPlace
            add(
                node {
                    wrapChildren = true
                    add(
                        node {
                            size = 10
                        }
                    )
                }
            )
        }

        val delegate = createDelegate(root)

        assertThat(root.height).isEqualTo(10)

        val childWithSize = root.first.first
        childWithSize.size = 20
        childWithSize.requestRemeasure()
        delegate.measureAndLayout()

        assertThat(root.height).isEqualTo(20)
    }

    @Test
    fun addingAndRemovingNotPlacingModifier() {
        var visible by mutableStateOf(false)
        rule.setContent {
            Box(
                Modifier
                    .then(
                        if (visible) Modifier else Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                            }
                        }
                    )
                    .size(10.dp)
                    .testTag(Tag)
            )
        }

        rule.runOnIdle {
            visible = true
        }

        rule.onNodeWithTag(Tag)
            .assertIsDisplayed()

        rule.runOnIdle {
            visible = false
        }

        rule.onNodeWithTag(Tag)
            .assertIsNotDisplayed()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawingOrderIsCorrectWhenAddingAndRemovingNotPlacingModifier() {
        var visible by mutableStateOf(false)
        val size = 4
        val halfSize = 4 / 2
        val sizeDp = with(rule.density) { size.toDp() }
        val halfSizeDp = with(rule.density) { halfSize.toDp() }
        rule.setContent {
            Box(Modifier.background(Color.Black).size(sizeDp).testTag(Tag)) {
                Box(
                    Modifier
                        .then(
                            if (visible) Modifier else Modifier.layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) {
                                }
                            }
                        )
                        .fillMaxSize()
                        .background(Color.Red)
                )
                Box(
                    Modifier
                        .offset(y = halfSizeDp)
                        .height(halfSizeDp)
                        .fillMaxWidth()
                        .background(Color.Green)
                )
            }
        }

        rule.runOnIdle {
            visible = true
        }

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertPixels(expectedSize = IntSize(size, size)) { offset ->
                if (offset.y < halfSize) {
                    Color.Red
                } else {
                    Color.Green
                }
            }

        rule.runOnIdle {
            visible = false
        }

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertPixels(expectedSize = IntSize(size, size)) { offset ->
                if (offset.y < halfSize) {
                    Color.Black
                } else {
                    Color.Green
                }
            }
    }

    @Test
    fun notPlacedChildIsNotCallingPlacingBlockOnItsModifier() {
        var modifier by mutableStateOf<Modifier>(Modifier)
        rule.setContent {
            Layout(content = {
                Box(modifier.size(10.dp))
            }) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(placeable.width, placeable.height) { }
            }
        }

        var measureCount = 0
        var placementCount = 0
        rule.runOnIdle {
            modifier = Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                measureCount++
                layout(placeable.width, placeable.height) {
                    placementCount++
                    placeable.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            assertThat(measureCount).isEqualTo(1)
            assertThat(placementCount).isEqualTo(0)
        }
    }
}

private val UseChildSizeButNotPlace = object : LayoutNode.NoIntrinsicsMeasurePolicy("") {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurables.first().measure(constraints)
        return layout(placeable.width, placeable.height) {
            // do not place
        }
    }
}
