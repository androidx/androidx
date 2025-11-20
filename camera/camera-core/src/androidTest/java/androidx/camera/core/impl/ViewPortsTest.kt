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
package androidx.camera.core.impl

import android.annotation.SuppressLint
import android.graphics.Rect
import android.graphics.RectF
import android.util.LayoutDirection
import android.util.Rational
import android.util.Size
import androidx.camera.core.UseCase
import androidx.camera.core.ViewPort
import androidx.camera.core.internal.ViewPorts
import androidx.camera.testing.impl.ConstraintEnclosedTestRunner
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Unit tests for [ViewPorts]. */
@SmallTest
@SuppressLint("UnsupportedTestRunner")
@RunWith(ConstraintEnclosedTestRunner::class)
object ViewPortsTest {
    // Rotation degrees.
    private const val R0 = 0
    private const val R90 = 90
    private const val R180 = 180
    private const val R270 = 270

    // 2:1 aspect ratio wider than 4:3.
    private val WIDE: Rational by lazy { Rational(2, 1) }

    // 1:2 aspect ratio narrower than 3:4.
    private val NARROW: Rational by lazy { Rational(1, 2) }

    /**
     * Parameterized tests for [ViewPorts.getScaledRect] testing all possible input combinations.
     */
    @SmallTest
    @RunWith(Parameterized::class)
    class GetScaledRectTests(
        private val aspectRatio: Rational,
        @ViewPort.LayoutDirection private val layoutDirection: Int,
        private val rotationDegrees: Int,
        @ViewPort.ScaleType private val scaleType: Int,
        private val expectedLeft: Int,
        private val expectedTop: Int,
        private val expectedSize: Size,
    ) {

        @Test
        fun testGetScaledRect() {
            val rect = Rect()
            ViewPorts.getScaledRect(
                    FITTING_RECT,
                    aspectRatio,
                    scaleType,
                    false,
                    layoutDirection,
                    rotationDegrees,
                )
                .round(rect)
            Truth.assertThat(intArrayOf(rect.left, rect.top, rect.width(), rect.height()))
                .isEqualTo(
                    intArrayOf(expectedLeft, expectedTop, expectedSize.width, expectedSize.height)
                )
        }

        companion object {
            // A 60x40 rect located at (10, 20).
            private val FITTING_RECT = RectF(10F, 20F, 70F, 60F)

            // Size of crop rect for all 4 possible aspect ratio/ fill type combinations.
            private val WIDE_FILL = Size(60, 30)
            private val NARROW_FILL = Size(20, 40)
            private val FIT_SIZE = Size(FITTING_RECT.width().toInt(), FITTING_RECT.height().toInt())
            // Wide viewport & FILL_CENTER. Parameter index 0~7.
            // Wide viewport & FILL_START. Parameter index 7~15.
            // Wide viewport & FILL_END. Parameter index 16~23.
            // Narrow viewport & FILL_CENTER. Parameter index 24~31.
            // Narrow viewport & FILL_START. Parameter index 32~39.
            // Narrow viewport & FILL_END. Parameter index 40~47.

            // FIT always returns the same rect. Parameter index 48~63.
            @JvmStatic
            @get:Parameterized.Parameters
            val parameters: Collection<Array<Any>>
                get() {
                    val result: MutableList<Array<Any>> = ArrayList()
                    // Wide viewport & FILL_CENTER. Parameter index 0~7.
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.LTR,
                            R0,
                            ViewPort.FILL_CENTER,
                            10,
                            25,
                            WIDE_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.LTR,
                            R90,
                            ViewPort.FILL_CENTER,
                            10,
                            25,
                            WIDE_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.LTR,
                            R180,
                            ViewPort.FILL_CENTER,
                            10,
                            25,
                            WIDE_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.LTR,
                            R270,
                            ViewPort.FILL_CENTER,
                            10,
                            25,
                            WIDE_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.RTL,
                            R0,
                            ViewPort.FILL_CENTER,
                            10,
                            25,
                            WIDE_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.RTL,
                            R90,
                            ViewPort.FILL_CENTER,
                            10,
                            25,
                            WIDE_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.RTL,
                            R180,
                            ViewPort.FILL_CENTER,
                            10,
                            25,
                            WIDE_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.RTL,
                            R270,
                            ViewPort.FILL_CENTER,
                            10,
                            25,
                            WIDE_FILL,
                        )
                    )
                    // Wide viewport & FILL_START. Parameter index 7~15.
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.LTR,
                            R0,
                            ViewPort.FILL_START,
                            10,
                            20,
                            WIDE_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.LTR,
                            R90,
                            ViewPort.FILL_START,
                            10,
                            30,
                            WIDE_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.LTR,
                            R180,
                            ViewPort.FILL_START,
                            10,
                            30,
                            WIDE_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.LTR,
                            R270,
                            ViewPort.FILL_START,
                            10,
                            20,
                            WIDE_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.RTL,
                            R0,
                            ViewPort.FILL_START,
                            10,
                            20,
                            WIDE_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.RTL,
                            R90,
                            ViewPort.FILL_START,
                            10,
                            20,
                            WIDE_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.RTL,
                            R180,
                            ViewPort.FILL_START,
                            10,
                            30,
                            WIDE_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.RTL,
                            R270,
                            ViewPort.FILL_START,
                            10,
                            30,
                            WIDE_FILL,
                        )
                    )
                    // Wide viewport & FILL_END. Parameter index 16~23.
                    result.add(
                        arrayOf(WIDE, LayoutDirection.LTR, R0, ViewPort.FILL_END, 10, 30, WIDE_FILL)
                    )
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.LTR,
                            R90,
                            ViewPort.FILL_END,
                            10,
                            20,
                            WIDE_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.LTR,
                            R180,
                            ViewPort.FILL_END,
                            10,
                            20,
                            WIDE_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.LTR,
                            R270,
                            ViewPort.FILL_END,
                            10,
                            30,
                            WIDE_FILL,
                        )
                    )
                    result.add(
                        arrayOf(WIDE, LayoutDirection.RTL, R0, ViewPort.FILL_END, 10, 30, WIDE_FILL)
                    )
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.RTL,
                            R90,
                            ViewPort.FILL_END,
                            10,
                            30,
                            WIDE_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.RTL,
                            R180,
                            ViewPort.FILL_END,
                            10,
                            20,
                            WIDE_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            WIDE,
                            LayoutDirection.RTL,
                            R270,
                            ViewPort.FILL_END,
                            10,
                            20,
                            WIDE_FILL,
                        )
                    )
                    // Narrow viewport & FILL_CENTER. Parameter index 24~31.
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.LTR,
                            R0,
                            ViewPort.FILL_CENTER,
                            30,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.LTR,
                            R90,
                            ViewPort.FILL_CENTER,
                            30,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.LTR,
                            R180,
                            ViewPort.FILL_CENTER,
                            30,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.LTR,
                            R270,
                            ViewPort.FILL_CENTER,
                            30,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.RTL,
                            R0,
                            ViewPort.FILL_CENTER,
                            30,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.RTL,
                            R90,
                            ViewPort.FILL_CENTER,
                            30,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.RTL,
                            R180,
                            ViewPort.FILL_CENTER,
                            30,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.RTL,
                            R270,
                            ViewPort.FILL_CENTER,
                            30,
                            20,
                            NARROW_FILL,
                        )
                    )
                    // Narrow viewport & FILL_START. Parameter index 32~39.
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.LTR,
                            R0,
                            ViewPort.FILL_START,
                            10,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.LTR,
                            R90,
                            ViewPort.FILL_START,
                            10,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.LTR,
                            R180,
                            ViewPort.FILL_START,
                            50,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.LTR,
                            R270,
                            ViewPort.FILL_START,
                            50,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.RTL,
                            R0,
                            ViewPort.FILL_START,
                            50,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.RTL,
                            R90,
                            ViewPort.FILL_START,
                            10,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.RTL,
                            R180,
                            ViewPort.FILL_START,
                            10,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.RTL,
                            R270,
                            ViewPort.FILL_START,
                            50,
                            20,
                            NARROW_FILL,
                        )
                    )
                    // Narrow viewport & FILL_END. Parameter index 40~47.
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.LTR,
                            R0,
                            ViewPort.FILL_END,
                            50,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.LTR,
                            R90,
                            ViewPort.FILL_END,
                            50,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.LTR,
                            R180,
                            ViewPort.FILL_END,
                            10,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.LTR,
                            R270,
                            ViewPort.FILL_END,
                            10,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.RTL,
                            R0,
                            ViewPort.FILL_END,
                            10,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.RTL,
                            R90,
                            ViewPort.FILL_END,
                            50,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.RTL,
                            R180,
                            ViewPort.FILL_END,
                            50,
                            20,
                            NARROW_FILL,
                        )
                    )
                    result.add(
                        arrayOf(
                            NARROW,
                            LayoutDirection.RTL,
                            R270,
                            ViewPort.FILL_END,
                            10,
                            20,
                            NARROW_FILL,
                        )
                    )

                    // FIT always returns the same rect. Parameter index 48~63.
                    for (ratio in arrayOf(WIDE, NARROW)) {
                        for (direction in intArrayOf(LayoutDirection.LTR, LayoutDirection.RTL)) {
                            for (rotation in intArrayOf(R0, R90, R180, R270)) {
                                result.add(
                                    arrayOf(
                                        ratio,
                                        direction,
                                        rotation,
                                        ViewPort.FIT,
                                        10,
                                        20,
                                        FIT_SIZE,
                                    )
                                )
                            }
                        }
                    }
                    return result
                }
        }
    }

    @SmallTest
    @RunWith(Parameterized::class)
    class GetViewPortRectsTests(
        private val sensorSize: Size,
        private val isFrontCamera: Boolean,
        private val aspectRatio: Rational,
        private val rotationDegrees: Int,
        @ViewPort.ScaleType private val scaleType: Int,
        @ViewPort.LayoutDirection private val layoutDirection: Int,
        private val surfaceSizes: Array<Size>,
        private val expectedCropRects: Array<Rect>,
    ) {

        @Test
        fun testGetViewPortRects() {
            // Arrange.
            // Convert the sizes into a UseCase map.
            val orderedUseCases: MutableList<UseCase> = ArrayList()
            val useCaseStreamSpecMap =
                HashMap<UseCase?, StreamSpec?>().apply {
                    for (size in surfaceSizes) {
                        val fakeUseCase = FakeUseCaseConfig.Builder().build()
                        put(fakeUseCase, StreamSpec.builder(size).build())
                        orderedUseCases.add(fakeUseCase)
                    }
                }

            // Act.
            val useCaseCropRects =
                ViewPorts.calculateViewPortRects(
                    Rect(0, 0, sensorSize.width, sensorSize.height),
                    isFrontCamera,
                    aspectRatio,
                    rotationDegrees,
                    scaleType,
                    layoutDirection,
                    useCaseStreamSpecMap,
                )

            // Assert.
            // Converts the map back to sizes array.
            val orderedCropRects: MutableList<Rect?> = ArrayList()
            for (useCase in orderedUseCases) {
                orderedCropRects.add(useCaseCropRects[useCase])
            }
            Truth.assertThat(orderedCropRects.toTypedArray()).isEqualTo(expectedCropRects)
        }

        companion object {
            private val SENSOR_SIZE = Size(8, 8)
            private val SURFACE_WIDE = Size(8, 4)
            private val SURFACE_NARROW = Size(4, 8)
            private const val FRONT_CAMERA = true
            private const val BACK_CAMERA = false // Wide and LTR.

            // Narrow and RTL.

            // Narrow and front camera.
            /**
             * Parameters for testing [ViewPorts.calculateViewPortRects]
             *
             * The goal of the algorithm is to fit-a-minimum/fill-a-maximum 2:1 rectangle with the
             * given rotation/layout-direction to the intersection area (marked with "XXX"). the
             * sensor rect is 8 x 8, narrow surface (marked with "\\\") is 4 x 8 and wide surface is
             * 8 x 4 (marked with "///").The output rect is in the surface' coordinates.
             * <pre>
             * . 0  1  2  3  4  5  6  7  8
             * 1 +-----\\\\\\\\\\\\-----+
             * 2 |     \\\\\\\\\\\\     |
             * 3 //////XXXXXXXXXXXX//////
             * 4 //////XXXXXXXXXXXX//////
             * 5 //////XXXXXXXXXXXX//////
             * 6 //////XXXXXXXXXXXX//////
             * 7 |     \\\\\\\\\\\\     |
             * 8 +-----\\\\\\\\\\\\-----+
             * </pre> *
             *
             * Only test 2 groups of cases. The rest should be tested by [GetScaledRectTests]
             */
            @JvmStatic
            @get:Parameterized.Parameters
            val parameters: Collection<Array<Any>>
                get() {
                    val result: MutableList<Array<Any>> = ArrayList()

                    // Wide and LTR.
                    result.add(
                        arrayOf(
                            SENSOR_SIZE,
                            BACK_CAMERA,
                            WIDE,
                            R0,
                            ViewPort.FILL_START,
                            LayoutDirection.LTR,
                            arrayOf(SURFACE_NARROW, SURFACE_WIDE),
                            arrayOf(Rect(0, 2, 4, 4), Rect(2, 0, 6, 2)),
                        )
                    )
                    result.add(
                        arrayOf(
                            SENSOR_SIZE,
                            BACK_CAMERA,
                            WIDE,
                            R90,
                            ViewPort.FILL_START,
                            LayoutDirection.LTR,
                            arrayOf(SURFACE_NARROW, SURFACE_WIDE),
                            arrayOf(Rect(0, 2, 2, 6), Rect(2, 0, 4, 4)),
                        )
                    )
                    result.add(
                        arrayOf(
                            SENSOR_SIZE,
                            BACK_CAMERA,
                            WIDE,
                            R180,
                            ViewPort.FILL_START,
                            LayoutDirection.LTR,
                            arrayOf(SURFACE_NARROW, SURFACE_WIDE),
                            arrayOf(Rect(0, 4, 4, 6), Rect(2, 2, 6, 4)),
                        )
                    )
                    result.add(
                        arrayOf(
                            SENSOR_SIZE,
                            BACK_CAMERA,
                            WIDE,
                            R270,
                            ViewPort.FILL_START,
                            LayoutDirection.LTR,
                            arrayOf(SURFACE_NARROW, SURFACE_WIDE),
                            arrayOf(Rect(2, 2, 4, 6), Rect(4, 0, 6, 4)),
                        )
                    )

                    // Narrow and RTL.
                    result.add(
                        arrayOf(
                            SENSOR_SIZE,
                            BACK_CAMERA,
                            NARROW,
                            R0,
                            ViewPort.FILL_START,
                            LayoutDirection.RTL,
                            arrayOf(SURFACE_NARROW, SURFACE_WIDE),
                            arrayOf(Rect(2, 2, 4, 6), Rect(4, 0, 6, 4)),
                        )
                    )
                    result.add(
                        arrayOf(
                            SENSOR_SIZE,
                            BACK_CAMERA,
                            NARROW,
                            R90,
                            ViewPort.FILL_START,
                            LayoutDirection.RTL,
                            arrayOf(SURFACE_NARROW, SURFACE_WIDE),
                            arrayOf(Rect(0, 2, 4, 4), Rect(2, 0, 6, 2)),
                        )
                    )
                    result.add(
                        arrayOf(
                            SENSOR_SIZE,
                            BACK_CAMERA,
                            NARROW,
                            R180,
                            ViewPort.FILL_START,
                            LayoutDirection.RTL,
                            arrayOf(SURFACE_NARROW, SURFACE_WIDE),
                            arrayOf(Rect(0, 2, 2, 6), Rect(2, 0, 4, 4)),
                        )
                    )
                    result.add(
                        arrayOf(
                            SENSOR_SIZE,
                            BACK_CAMERA,
                            NARROW,
                            R270,
                            ViewPort.FILL_START,
                            LayoutDirection.RTL,
                            arrayOf(SURFACE_NARROW, SURFACE_WIDE),
                            arrayOf(Rect(0, 4, 4, 6), Rect(2, 2, 6, 4)),
                        )
                    )

                    // Narrow and front camera.
                    result.add(
                        arrayOf(
                            SENSOR_SIZE,
                            FRONT_CAMERA,
                            NARROW,
                            R0,
                            ViewPort.FILL_START,
                            LayoutDirection.LTR,
                            arrayOf(SURFACE_NARROW, SURFACE_WIDE),
                            arrayOf(Rect(2, 2, 4, 6), Rect(4, 0, 6, 4)),
                        )
                    )
                    result.add(
                        arrayOf(
                            SENSOR_SIZE,
                            FRONT_CAMERA,
                            NARROW,
                            R90,
                            ViewPort.FILL_START,
                            LayoutDirection.LTR,
                            arrayOf(SURFACE_NARROW, SURFACE_WIDE),
                            arrayOf(Rect(0, 2, 4, 4), Rect(2, 0, 6, 2)),
                        )
                    )
                    result.add(
                        arrayOf(
                            SENSOR_SIZE,
                            FRONT_CAMERA,
                            NARROW,
                            R180,
                            ViewPort.FILL_START,
                            LayoutDirection.LTR,
                            arrayOf(SURFACE_NARROW, SURFACE_WIDE),
                            arrayOf(Rect(0, 2, 2, 6), Rect(2, 0, 4, 4)),
                        )
                    )
                    result.add(
                        arrayOf(
                            SENSOR_SIZE,
                            FRONT_CAMERA,
                            NARROW,
                            R270,
                            ViewPort.FILL_START,
                            LayoutDirection.LTR,
                            arrayOf(SURFACE_NARROW, SURFACE_WIDE),
                            arrayOf(Rect(0, 4, 4, 6), Rect(2, 2, 6, 4)),
                        )
                    )
                    return result
                }
        }
    }
}
