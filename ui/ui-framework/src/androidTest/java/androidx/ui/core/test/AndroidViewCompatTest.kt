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
import androidx.compose.Composable
import androidx.compose.Model
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.ui.core.AndroidComposeView
import androidx.ui.core.Layout
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
}

@Model private data class ColorModel(var color: Color)
