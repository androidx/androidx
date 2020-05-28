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

@file:Suppress("Deprecation")

package androidx.ui.core.test

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.compose.Composable
import androidx.compose.mutableStateOf
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureScope
import androidx.ui.core.Modifier
import androidx.ui.core.Owner
import androidx.ui.core.Ref
import androidx.ui.core.drawLayer
import androidx.ui.graphics.Color
import androidx.ui.graphics.toArgb
import androidx.ui.core.testTag
import androidx.ui.test.assertIsDisplayed
import androidx.ui.test.assertPixels
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByTag
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.runOnUiThread
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.ipx
import junit.framework.TestCase.assertNotNull
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Testing the support for Android Views in Compose UI.
 */
@SmallTest
@RunWith(JUnit4::class)
class AndroidViewCompatTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun simpleLayoutTest() {
        val squareRef = Ref<ColoredSquareView>()
        val squareSize = mutableStateOf(100.ipx)
        var expectedSize = 100
        composeTestRule.setContent {
            Align {
                Layout(
                    modifier = Modifier.testTag("content"),
                    children = @Composable {
                        ColoredSquareView(size = squareSize.value.value, ref = squareRef)
                    }
                ) { measurables, constraints, _ ->
                    assertEquals(1, measurables.size)
                    val placeable = measurables.first().measure(
                        constraints.copy(minWidth = 0.ipx, minHeight = 0.ipx)
                    )
                    assertEquals(placeable.width, expectedSize.ipx)
                    assertEquals(placeable.height, expectedSize.ipx)
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0.ipx, 0.ipx)
                    }
                }
            }
        }
        findByTag("content").assertIsDisplayed()
        val squareView = squareRef.value
        assertNotNull(squareView)
        Espresso
            .onView(instanceOf(ColoredSquareView::class.java))
            .check(matches(isDescendantOfA(instanceOf(Owner::class.java))))
            .check(matches(`is`(squareView)))

        runOnUiThread {
            // Change view attribute using recomposition.
            squareSize.value = 200.ipx
            expectedSize = 200
        }
        findByTag("content").assertIsDisplayed()
        Espresso
            .onView(instanceOf(ColoredSquareView::class.java))
            .check(matches(isDescendantOfA(instanceOf(Owner::class.java))))
            .check(matches(`is`(squareView)))

        runOnUiThread {
            // Change view attribute using the View reference.
            squareView!!.size = 300
            expectedSize = 300
        }
        findByTag("content").assertIsDisplayed()
        Espresso
            .onView(instanceOf(ColoredSquareView::class.java))
            .check(matches(isDescendantOfA(instanceOf(Owner::class.java))))
            .check(matches(`is`(squareView)))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun simpleDrawTest() {
        val squareRef = Ref<ColoredSquareView>()
        val colorModel = mutableStateOf(Color.Blue)
        val squareSize = 100
        var expectedColor = Color.Blue
        composeTestRule.setContent {
            Align {
                Container(Modifier.testTag("content").drawLayer()) {
                    ColoredSquareView(color = colorModel.value, ref = squareRef)
                }
            }
        }
        val squareView = squareRef.value
        assertNotNull(squareView)
        Espresso
            .onView(instanceOf(ColoredSquareView::class.java))
            .check(matches(isDescendantOfA(instanceOf(Owner::class.java))))
            .check(matches(`is`(squareView)))
        val expectedPixelColor = { position: IntPxPosition ->
            if (position.x.value < squareSize && position.y.value < squareSize) {
                expectedColor
            } else {
                Color.White
            }
        }
        findByTag("content")
            .assertIsDisplayed()
            .captureToBitmap()
            .assertPixels(expectedColorProvider = expectedPixelColor)

        runOnUiThread {
            // Change view attribute using recomposition.
            colorModel.value = Color.Green
            expectedColor = Color.Green
        }
        Espresso
            .onView(instanceOf(ColoredSquareView::class.java))
            .check(matches(isDescendantOfA(instanceOf(Owner::class.java))))
            .check(matches(`is`(squareView)))
        findByTag("content")
            .assertIsDisplayed()
            .captureToBitmap()
            .assertPixels(expectedColorProvider = expectedPixelColor)

        runOnUiThread {
            // Change view attribute using the View reference.
            colorModel.value = Color.Red
            expectedColor = Color.Red
        }
        Espresso
            .onView(instanceOf(ColoredSquareView::class.java))
            .check(matches(isDescendantOfA(instanceOf(Owner::class.java))))
            .check(matches(`is`(squareView)))
        findByTag("content")
            .assertIsDisplayed()
            .captureToBitmap()
            .assertPixels(expectedColorProvider = expectedPixelColor)
    }

    // When incoming constraints are fixed.

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_1() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(20, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(30, MeasureSpec.EXACTLY),
            Constraints.fixed(20.ipx, 30.ipx),
            ViewGroup.LayoutParams(40, 50)
        )
    }

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_2() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(20, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(30, MeasureSpec.EXACTLY),
            Constraints.fixed(20.ipx, 30.ipx),
            ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        )
    }

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_3() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(20, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(30, MeasureSpec.EXACTLY),
            Constraints.fixed(20.ipx, 30.ipx),
            ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        )
    }

    // When incoming constraints are finite.

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_4() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(25, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(35, MeasureSpec.EXACTLY),
            Constraints(
                minWidth = 20.ipx, maxWidth = 30.ipx, minHeight = 35.ipx, maxHeight = 45.ipx
            ),
            ViewGroup.LayoutParams(25, 35)
        )
    }

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_5() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(20, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(35, MeasureSpec.EXACTLY),
            Constraints(
                minWidth = 20.ipx, maxWidth = 30.ipx, minHeight = 35.ipx, maxHeight = 45.ipx
            ),
            ViewGroup.LayoutParams(15, 25)
        )
    }

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_6() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(30, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(45, MeasureSpec.EXACTLY),
            Constraints(
                minWidth = 20.ipx, maxWidth = 30.ipx, minHeight = 35.ipx, maxHeight = 45.ipx
            ),
            ViewGroup.LayoutParams(35, 50)
        )
    }

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_7() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(40, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(50, MeasureSpec.AT_MOST),
            Constraints(maxWidth = 40.ipx, maxHeight = 50.ipx),
            ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        )
    }

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_8() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(40, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(50, MeasureSpec.EXACTLY),
            Constraints(maxWidth = 40.ipx, maxHeight = 50.ipx),
            ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        )
    }

    // When incoming constraints are infinite.

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_9() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(25, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(35, MeasureSpec.EXACTLY),
            Constraints(),
            ViewGroup.LayoutParams(25, 35)
        )
    }

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_10() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            Constraints(),
            ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        )
    }

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_11() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            Constraints(),
            ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        )
    }

    private fun testMeasurement_isDoneWithCorrectMeasureSpecs(
        expectedWidthSpec: Int,
        expectedHeightSpec: Int,
        constraints: Constraints,
        layoutParams: ViewGroup.LayoutParams
    ) {
        val viewRef = Ref<MeasureSpecSaverView>()
        val widthMeasureSpecRef = Ref<Int>()
        val heightMeasureSpecRef = Ref<Int>()
        // Unique starting constraints so that new constraints are different and thus recomp is
        // guaranteed.
        val constraintsHolder = mutableStateOf(Constraints.fixed(1234.ipx, 5678.ipx))

        composeTestRule.setContent {
            Container(LayoutConstraints(constraintsHolder.value)) {
                MeasureSpecSaverView(
                    ref = viewRef,
                    widthMeasureSpecRef = widthMeasureSpecRef,
                    heightMeasureSpecRef = heightMeasureSpecRef
                )
            }
        }

        runOnUiThread {
            constraintsHolder.value = constraints
            viewRef.value?.layoutParams = layoutParams
        }

        runOnIdleCompose {
            assertEquals(expectedWidthSpec, widthMeasureSpecRef.value)
            assertEquals(expectedHeightSpec, heightMeasureSpecRef.value)
        }
    }

    @Test
    fun testMeasurement_isDoneWithCorrectMinimumDimensionsSetOnView() {
        val viewRef = Ref<MeasureSpecSaverView>()
        val constraintsHolder = mutableStateOf(Constraints())
        composeTestRule.setContent {
            Container(LayoutConstraints(constraintsHolder.value)) {
                MeasureSpecSaverView(ref = viewRef)
            }
        }
        runOnUiThread {
            constraintsHolder.value = Constraints(minWidth = 20.ipx, minHeight = 30.ipx)
        }

        runOnIdleCompose {
            assertEquals(20, viewRef.value!!.minimumWidth)
            assertEquals(30, viewRef.value!!.minimumHeight)
        }
    }

    class ColoredSquareView(context: Context) : View(context) {
        var size: Int = 100
            set(value) {
                if (value != field) {
                    field = value
                    requestLayout()
                }
            }

        var color: Color = Color.Blue
            set(value) {
                if (value != field) {
                    field = value
                    invalidate()
                }
            }

        // TODO(popam): can we merge the android-view Ref with the one in core?
        var ref: Ref<ColoredSquareView>? = null
            set(value) {
                field = value
                value?.value = this
            }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            setMeasuredDimension(size, size)
        }

        override fun draw(canvas: Canvas?) {
            super.draw(canvas)
            canvas!!.drawRect(
                Rect(0, 0, size, size),
                Paint().apply { color = this@ColoredSquareView.color.toArgb() }
            )
        }
    }

    class MeasureSpecSaverView(context: Context) : View(context) {
        var ref: Ref<MeasureSpecSaverView>? = null
            set(value) {
                field = value
                value?.value = this
            }
        var widthMeasureSpecRef: Ref<Int>? = null
        var heightMeasureSpecRef: Ref<Int>? = null

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            widthMeasureSpecRef?.value = widthMeasureSpec
            heightMeasureSpecRef?.value = heightMeasureSpec
            setMeasuredDimension(0, 0)
        }
    }

    fun LayoutConstraints(childConstraints: Constraints) = object : LayoutModifier {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints,
            layoutDirection: LayoutDirection
        ): MeasureScope.MeasureResult {
            val placeable = measurable.measure(childConstraints)
            return layout(placeable.width, placeable.height) {
                placeable.place(0.ipx, 0.ipx)
            }
        }
    }

    @Composable
    fun Container(
        modifier: Modifier = Modifier,
        children: @Composable () -> Unit
    ) {
        Layout(children, modifier) { measurables, constraints, _ ->
            val placeable = measurables[0].measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0.ipx, 0.ipx)
            }
        }
    }
}
