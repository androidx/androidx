/*
 * Copyright 2023 The Android Open Source Project
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

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Constraints
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.roundToInt
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ResizingComposeViewTest {

    private lateinit var composeView: ComposeView
    private var layoutHeight = -1
    private var viewHeight = -1

    @get:Rule val rule = createAndroidComposeRule<TestActivity>(StandardTestDispatcher())

    @Before
    fun setup() {
        composeView = ComposeView(rule.activity)
    }

    @Test
    fun whenParentIsMeasuringTwiceWithDifferentConstraints() {
        var height by mutableStateOf(10)
        rule.runOnUiThread {
            val linearLayout = LinearLayout(rule.activity)
            linearLayout.orientation = LinearLayout.VERTICAL
            rule.activity.setContentView(linearLayout)
            linearLayout.addView(
                composeView,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f,
                ),
            )
            linearLayout.addView(
                View(rule.activity),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 10000f),
            )
            composeView.setContent { ResizingChild(layoutHeight = { height }) }
        }

        awaitDrawAndAssertSizes(10)
        rule.runOnUiThread { height = 20 }

        awaitDrawAndAssertSizes(20)
    }

    @Test
    fun whenMeasuredWithWrapContent() {
        var height by mutableStateOf(10)

        rule.runOnUiThread {
            rule.activity.setContentView(composeView, WrapContentLayoutParams)
            composeView.setContent { ResizingChild(layoutHeight = { height }) }
        }

        awaitDrawAndAssertSizes(10)
        rule.runOnUiThread { height = 20 }

        awaitDrawAndAssertSizes(20)
    }

    @Test
    fun whenMeasuredWithFixedConstraints() {
        var childHeight by mutableStateOf(10)
        val viewSize = 30
        val parent = RequestLayoutTrackingFrameLayout(rule.activity)

        rule.runOnUiThread {
            parent.addView(composeView, ViewGroup.LayoutParams(viewSize, viewSize))
            rule.activity.setContentView(parent, WrapContentLayoutParams)
            composeView.setContent {
                ResizingChild(layoutHeight = { childHeight }, viewHeight = { viewSize })
            }
        }

        awaitDrawAndAssertSizes(10, viewSize)
        rule.runOnUiThread {
            childHeight = 20
            parent.requestLayoutCalled = false
        }

        awaitDrawAndAssertSizes(20, viewSize)
        // as the ComposeView is measured with fixed size parent shouldn't be remeasured
        assertThat(parent.requestLayoutCalled).isFalse()
    }

    @Test
    fun whenInsideComposableParentWithFixedSize() {
        var childHeight by mutableStateOf(10)
        val parentSize = 30
        val parent = RequestLayoutTrackingFrameLayout(rule.activity)

        rule.runOnUiThread {
            parent.addView(composeView, WrapContentLayoutParams)
            rule.activity.setContentView(parent, WrapContentLayoutParams)
            composeView.setContent {
                Layout(
                    modifier =
                        Modifier.layout { measurable, _ ->
                            // this modifier sets a fixed size on a parent similarly to how
                            // Modifier.fillMaxSize() or Modifier.size(foo) would do
                            val placeable =
                                measurable.measure(Constraints.fixed(parentSize, parentSize))
                            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                        },
                    content = {
                        ResizingChild(layoutHeight = { childHeight }, viewHeight = { parentSize })
                    },
                ) { measurables, constraints ->
                    val placeable = measurables[0].measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
            }
        }

        awaitDrawAndAssertSizes(10, parentSize)
        rule.runOnUiThread {
            childHeight = 20
            parent.requestLayoutCalled = false
        }

        awaitDrawAndAssertSizes(20, parentSize)
        // as the child is not affecting size parent view shouldn't be remeasured
        assertThat(parent.requestLayoutCalled).isFalse()
    }

    @Test
    fun whenParentIsMeasuringInLayoutBlock() {
        var childHeight by mutableStateOf(10)
        val parentSize = 30
        val parent = RequestLayoutTrackingFrameLayout(rule.activity)

        rule.runOnUiThread {
            parent.addView(composeView, WrapContentLayoutParams)
            rule.activity.setContentView(parent, WrapContentLayoutParams)
            composeView.setContent {
                Layout(
                    content = {
                        ResizingChild(layoutHeight = { childHeight }, viewHeight = { parentSize })
                    }
                ) { measurables, _ ->
                    layout(parentSize, parentSize) {
                        val placeable =
                            measurables[0].measure(Constraints.fixed(parentSize, parentSize))
                        placeable.place(0, 0)
                    }
                }
            }
        }

        awaitDrawAndAssertSizes(10, parentSize)
        rule.runOnUiThread {
            childHeight = 20
            parent.requestLayoutCalled = false
        }

        awaitDrawAndAssertSizes(20, parentSize)
        // as the child is not affecting size parent view shouldn't be remeasured
        assertThat(parent.requestLayoutCalled).isFalse()
    }

    @Test
    fun whenParentIsSettingFixedIntrinsicsSize() {
        var intrinsicsHeight by mutableStateOf(10)
        val parent = RequestLayoutTrackingFrameLayout(rule.activity)

        rule.runOnUiThread {
            parent.addView(composeView, WrapContentLayoutParams)
            rule.activity.setContentView(parent, WrapContentLayoutParams)
            composeView.setContent {
                Layout(
                    modifier =
                        Modifier.layout { measurable, _ ->
                            val intrinsicsSize = measurable.minIntrinsicHeight(Int.MAX_VALUE)
                            val placeable =
                                measurable.measure(
                                    Constraints.fixed(intrinsicsSize, intrinsicsSize)
                                )
                            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                        },
                    content = { IntrinsicsChild(intrinsicsHeight = { intrinsicsHeight }) },
                ) { measurables, constraints ->
                    val placeable = measurables[0].measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
            }
        }

        awaitDrawAndAssertSizes(10)
        rule.runOnUiThread { intrinsicsHeight = 20 }

        awaitDrawAndAssertSizes(20)
    }

    @Test
    fun whenForceRemeasureCalledAndSizeChanged() {
        var childHeight = 10
        val parent = RequestLayoutTrackingFrameLayout(rule.activity)
        var remeasurement: Remeasurement? = null
        rule.runOnUiThread {
            parent.addView(composeView, WrapContentLayoutParams)
            rule.activity.setContentView(parent, WrapContentLayoutParams)
            composeView.setContent {
                ResizingChild(
                    layoutHeight = { childHeight },
                    modifier = RemeasurementElement { remeasurement = it },
                )
            }
        }

        awaitDrawAndAssertSizes(10)
        // Sometimes there's a stray layout request, so wait until the request is done.
        var isLayoutRequested = false
        do {
            rule.runOnUiThread {
                isLayoutRequested = parent.isLayoutRequested
                if (!isLayoutRequested) {
                    parent.requestLayoutCalled = false

                    childHeight = 20
                    remeasurement!!.forceRemeasure()
                }
            }
        } while (isLayoutRequested)

        awaitDrawAndAssertSizes(20)

        rule.runOnUiThread { assertThat(parent.requestLayoutCalled).isTrue() }
    }

    @Test
    fun noRequestLayoutWhenForceRemeasureCalled() {
        val parent = RequestLayoutTrackingFrameLayout(rule.activity)
        var remeasurement: Remeasurement? = null
        rule.runOnUiThread {
            parent.addView(composeView, WrapContentLayoutParams)
            rule.activity.setContentView(parent, WrapContentLayoutParams)
            composeView.setContent {
                ResizingChild(
                    layoutHeight = { 10 },
                    modifier = RemeasurementElement { remeasurement = it },
                )
            }
        }

        awaitDrawAndAssertSizes(10)
        rule.runOnUiThread {
            parent.requestLayoutCalled = false

            remeasurement!!.forceRemeasure()

            assertThat(parent.requestLayoutCalled).isFalse()
        }
    }

    private fun awaitDrawAndAssertSizes(
        expectedLayoutHeight: Int,
        expectedViewHeight: Int = expectedLayoutHeight,
    ) {
        rule.waitForIdle()
        assertWithMessage("Layout size is wrong").that(layoutHeight).isEqualTo(expectedLayoutHeight)
        assertWithMessage("ComposeView size is wrong")
            .that(viewHeight)
            .isEqualTo(expectedViewHeight)
    }

    @Composable
    private fun ResizingChild(
        layoutHeight: () -> Int,
        viewHeight: () -> Int = layoutHeight,
        modifier: Modifier = Modifier,
    ) {
        Layout(
            {},
            modifier.drawBehind {
                this@ResizingComposeViewTest.layoutHeight = size.height.roundToInt()
                this@ResizingComposeViewTest.viewHeight = composeView.measuredHeight
            },
        ) { _, constraints ->
            layout(constraints.maxWidth, layoutHeight()) {}
        }
    }

    @Composable
    private fun IntrinsicsChild(intrinsicsHeight: () -> Int) {
        Layout(
            {},
            Modifier.drawBehind {
                this@ResizingComposeViewTest.layoutHeight = size.height.roundToInt()
                this@ResizingComposeViewTest.viewHeight = composeView.measuredHeight
            },
            object : MeasurePolicy {
                override fun MeasureScope.measure(
                    measurables: List<Measurable>,
                    constraints: Constraints,
                ): MeasureResult {
                    return layout(constraints.maxWidth, constraints.maxHeight) {}
                }

                override fun IntrinsicMeasureScope.minIntrinsicHeight(
                    measurables: List<IntrinsicMeasurable>,
                    width: Int,
                ): Int = intrinsicsHeight()
            },
        )
    }
}

private class RequestLayoutTrackingFrameLayout(context: Context) : FrameLayout(context) {

    var requestLayoutCalled = false

    override fun requestLayout() {
        super.requestLayout()
        requestLayoutCalled = true
    }
}

private val WrapContentLayoutParams =
    ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

private class RemeasurementElement(private val onRemeasurementAvailable: (Remeasurement) -> Unit) :
    ModifierNodeElement<RemeasurementModifierNode>() {
    override fun create() = RemeasurementModifierNode(onRemeasurementAvailable)

    override fun update(node: RemeasurementModifierNode) {
        node.onRemeasurementAvailable = onRemeasurementAvailable
    }

    override fun hashCode(): Int = 242

    override fun equals(other: Any?) = other === this
}

private class RemeasurementModifierNode(onRemeasurementAvailable: (Remeasurement) -> Unit) :
    Modifier.Node() {
    var onRemeasurementAvailable: (Remeasurement) -> Unit = onRemeasurementAvailable
        set(value) {
            field = value
            value(requireLayoutNode())
        }

    override fun onAttach() {
        onRemeasurementAvailable(requireLayoutNode())
    }
}
