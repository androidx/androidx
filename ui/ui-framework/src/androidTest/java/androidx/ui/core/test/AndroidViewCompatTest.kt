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

package androidx.ui.core.test

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.View.MeasureSpec
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.compose.Composable
import androidx.compose.Model
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.ui.core.AndroidComposeView
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.LayoutModifier
import androidx.ui.core.ModifierScope
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
import androidx.ui.core.RepaintBoundary
import androidx.ui.core.TestTag
import androidx.ui.graphics.Color
import androidx.ui.graphics.toArgb
import androidx.ui.semantics.Semantics
import androidx.ui.test.assertIsDisplayed
import androidx.ui.test.assertPixels
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByTag
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
 * Corresponds to AndroidViewCompat.kt, testing the support for Android Views in Compose UI.
 */
@SmallTest
@RunWith(JUnit4::class)
class AndroidViewCompatTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun simpleLayoutTest() {
        val squareRef = Ref<ColoredSquareView>()
        val squareSize = OffsetModel(100.ipx)
        var expectedSize = 100
        composeTestRule.setContent {
            TestTag("content") {
                Semantics {
                    Layout(@Composable {
                        ColoredSquareView(size = squareSize.offset.value, ref = squareRef)
                    }) { measurables, constraints ->
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
        }
        findByTag("content").assertIsDisplayed()
        val squareView = squareRef.value
        assertNotNull(squareView)
        Espresso
            .onView(instanceOf(ColoredSquareView::class.java))
            .check(matches(isDescendantOfA(instanceOf(AndroidComposeView::class.java))))
            .check(matches(`is`(squareView)))

        composeTestRule.runOnUiThread {
            // Change view attribute using recomposition.
            squareSize.offset = 200.ipx
            expectedSize = 200
        }
        findByTag("content").assertIsDisplayed()
        Espresso
            .onView(instanceOf(ColoredSquareView::class.java))
            .check(matches(isDescendantOfA(instanceOf(AndroidComposeView::class.java))))
            .check(matches(`is`(squareView)))

        composeTestRule.runOnUiThread {
            // Change view attribute using the View reference.
            squareView!!.size = 300
            expectedSize = 300
        }
        findByTag("content").assertIsDisplayed()
        Espresso
            .onView(instanceOf(ColoredSquareView::class.java))
            .check(matches(isDescendantOfA(instanceOf(AndroidComposeView::class.java))))
            .check(matches(`is`(squareView)))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun simpleDrawTest() {
        val squareRef = Ref<ColoredSquareView>()
        val colorModel = ColorModel(Color.Blue)
        val squareSize = 100
        var expectedColor = Color.Blue
        composeTestRule.setContent {
            TestTag("content") {
                Semantics {
                    RepaintBoundary {
                        ColoredSquareView(color = colorModel.color, ref = squareRef)
                    }
                }
            }
        }
        val squareView = squareRef.value
        assertNotNull(squareView)
        Espresso
            .onView(instanceOf(ColoredSquareView::class.java))
            .check(matches(isDescendantOfA(instanceOf(AndroidComposeView::class.java))))
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

        composeTestRule.runOnUiThread {
            // Change view attribute using recomposition.
            colorModel.color = Color.Green
            expectedColor = Color.Green
        }
        Espresso
            .onView(instanceOf(ColoredSquareView::class.java))
            .check(matches(isDescendantOfA(instanceOf(AndroidComposeView::class.java))))
            .check(matches(`is`(squareView)))
        findByTag("content")
            .assertIsDisplayed()
            .captureToBitmap()
            .assertPixels(expectedColorProvider = expectedPixelColor)

        composeTestRule.runOnUiThread {
            // Change view attribute using the View reference.
            colorModel.color = Color.Red
            expectedColor = Color.Red
        }
        Espresso
            .onView(instanceOf(ColoredSquareView::class.java))
            .check(matches(isDescendantOfA(instanceOf(AndroidComposeView::class.java))))
            .check(matches(`is`(squareView)))
        findByTag("content")
            .assertIsDisplayed()
            .captureToBitmap()
            .assertPixels(expectedColorProvider = expectedPixelColor)
    }

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs() {
        val viewRef = Ref<MeasureSpecSaverView>()
        val widthMeasureSpecRef = Ref<Int>()
        val heightMeasureSpecRef = Ref<Int>()
        val constraintsHolder = ConstraintsModel(Constraints())
        composeTestRule.setContent {
            Container(LayoutConstraints(constraintsHolder.constraints)) {
                MeasureSpecSaverView(
                    ref = viewRef,
                    widthMeasureSpecRef = widthMeasureSpecRef,
                    heightMeasureSpecRef = heightMeasureSpecRef
                )
            }
        }
        fun assertMeasureSpec(
            expectedWidthSpec: Int,
            expectedHeightSpec: Int,
            constraints: Constraints,
            layoutParams: ViewGroup.LayoutParams
        ) {
            composeTestRule.runOnUiThread {
                constraintsHolder.constraints = constraints
                viewRef.value?.layoutParams = layoutParams
            }

            composeTestRule.runOnIdleCompose {
                assertEquals(expectedWidthSpec, widthMeasureSpecRef.value)
                assertEquals(expectedHeightSpec, heightMeasureSpecRef.value)
            }
        }
        // When incoming constraints are fixed.
        assertMeasureSpec(
            MeasureSpec.makeMeasureSpec(20, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(30, MeasureSpec.EXACTLY),
            Constraints.fixed(20.ipx, 30.ipx),
            ViewGroup.LayoutParams(40, 50)
        )
        assertMeasureSpec(
            MeasureSpec.makeMeasureSpec(20, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(30, MeasureSpec.EXACTLY),
            Constraints.fixed(20.ipx, 30.ipx),
            ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        )
        assertMeasureSpec(
            MeasureSpec.makeMeasureSpec(20, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(30, MeasureSpec.EXACTLY),
            Constraints.fixed(20.ipx, 30.ipx),
            ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        )
        // When incoming constraints are finite.
        assertMeasureSpec(
            MeasureSpec.makeMeasureSpec(25, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(35, MeasureSpec.EXACTLY),
            Constraints(
                minWidth = 20.ipx, maxWidth = 30.ipx, minHeight = 35.ipx, maxHeight = 45.ipx
            ),
            ViewGroup.LayoutParams(25, 35)
        )
        assertMeasureSpec(
            MeasureSpec.makeMeasureSpec(20, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(35, MeasureSpec.EXACTLY),
            Constraints(
                minWidth = 20.ipx, maxWidth = 30.ipx, minHeight = 35.ipx, maxHeight = 45.ipx
            ),
            ViewGroup.LayoutParams(15, 25)
        )
        assertMeasureSpec(
            MeasureSpec.makeMeasureSpec(30, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(45, MeasureSpec.EXACTLY),
            Constraints(
                minWidth = 20.ipx, maxWidth = 30.ipx, minHeight = 35.ipx, maxHeight = 45.ipx
            ),
            ViewGroup.LayoutParams(35, 50)
        )
        assertMeasureSpec(
            MeasureSpec.makeMeasureSpec(40, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(50, MeasureSpec.AT_MOST),
            Constraints(maxWidth = 40.ipx, maxHeight = 50.ipx),
            ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        )
        assertMeasureSpec(
            MeasureSpec.makeMeasureSpec(40, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(50, MeasureSpec.EXACTLY),
            Constraints(maxWidth = 40.ipx, maxHeight = 50.ipx),
            ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        )
        // When incoming constraints are infinite.
        assertMeasureSpec(
            MeasureSpec.makeMeasureSpec(25, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(35, MeasureSpec.EXACTLY),
            Constraints(),
            ViewGroup.LayoutParams(25, 35)
        )
        assertMeasureSpec(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            Constraints(),
            ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        )
        assertMeasureSpec(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            Constraints(),
            ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        )
    }

    @Test
    fun testMeasurement_isDoneWithCorrectMinimumDimensionsSetOnView() {
        val viewRef = Ref<MeasureSpecSaverView>()
        val constraintsHolder = ConstraintsModel(Constraints())
        composeTestRule.setContent {
            Container(LayoutConstraints(constraintsHolder.constraints)) {
                MeasureSpecSaverView(ref = viewRef)
            }
        }
        composeTestRule.runOnUiThread {
            constraintsHolder.constraints = Constraints(minWidth = 20.ipx, minHeight = 30.ipx)
        }

        composeTestRule.runOnIdleCompose {
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
        override fun ModifierScope.modifyConstraints(constraints: Constraints): Constraints {
            return childConstraints
        }
    }

    @Composable
    fun Container(
        modifier: Modifier = Modifier.None,
        children: @Composable() () -> Unit
    ) {
        Layout(children, modifier) { measurables, constraints ->
            val placeable = measurables[0].measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0.ipx, 0.ipx)
            }
        }
    }
}

@Model private data class ColorModel(var color: Color)
@Model private data class ConstraintsModel(var constraints: Constraints)
