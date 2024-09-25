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

package androidx.window.layout.adapter

import android.graphics.Rect
import android.os.Build
import android.util.TypedValue
import androidx.annotation.RequiresApi
import androidx.test.core.app.ActivityScenario
import androidx.window.TestActivity
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.computeWindowSizeClass
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator
import kotlin.test.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

class WindowSizeClassFactoryTest {

    @Test
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun test_calculating_from_window_metrics_matches_conversion() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        ActivityScenario.launch(TestActivity::class.java).onActivity { activity ->
            val windowMetrics =
                WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity)
            val displayMetrics = activity.resources.displayMetrics
            val widthDp =
                TypedValue.deriveDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    windowMetrics.bounds.width().toFloat(),
                    displayMetrics
                )
            val heightDp =
                TypedValue.deriveDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    windowMetrics.bounds.height().toFloat(),
                    displayMetrics
                )

            val sizeClassFromWindowMetrics =
                WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(windowMetrics)

            val sizeClassFromDp =
                WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(widthDp, heightDp)

            assertEquals(sizeClassFromWindowMetrics, sizeClassFromDp)
        }
    }

    @Test
    fun test_calculating_from_window_metrics_identity_w_compact_h_compact() {
        ActivityScenario.launch(TestActivity::class.java).onActivity { activity ->
            val displayMetrics = activity.resources.displayMetrics
            val sizeClass = WindowSizeClass(0, 0)

            val rawWidth =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeClass.minWidthDp.toFloat(),
                    displayMetrics
                )
            val rawHeight =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeClass.minHeightDp.toFloat(),
                    displayMetrics
                )
            val bounds = Rect(0, 0, rawWidth.toInt(), rawHeight.toInt())

            val metrics = WindowMetrics(bounds, displayMetrics.density)

            val calculatedSizeClass = WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(metrics)

            assertEquals(sizeClass, calculatedSizeClass)
        }
    }

    @Test
    fun test_calculating_from_window_metrics_identity_w_compact_h_medium() {
        ActivityScenario.launch(TestActivity::class.java).onActivity { activity ->
            val displayMetrics = activity.resources.displayMetrics
            val sizeClass = WindowSizeClass(0, HEIGHT_DP_MEDIUM_LOWER_BOUND)

            val rawWidth =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeClass.minWidthDp.toFloat(),
                    displayMetrics
                )
            val rawHeight =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeClass.minHeightDp.toFloat(),
                    displayMetrics
                )
            val bounds = Rect(0, 0, rawWidth.toInt(), rawHeight.toInt())

            val metrics = WindowMetrics(bounds, displayMetrics.density)

            val calculatedSizeClass = WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(metrics)

            assertEquals(sizeClass, calculatedSizeClass)
        }
    }

    @Test
    fun test_calculating_from_window_metrics_identity_w_compact_h_expanded() {
        ActivityScenario.launch(TestActivity::class.java).onActivity { activity ->
            val displayMetrics = activity.resources.displayMetrics
            val sizeClass = WindowSizeClass(0, HEIGHT_DP_EXPANDED_LOWER_BOUND)

            val rawWidth =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeClass.minWidthDp.toFloat(),
                    displayMetrics
                )
            val rawHeight =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeClass.minHeightDp.toFloat(),
                    displayMetrics
                )
            val bounds = Rect(0, 0, rawWidth.toInt(), rawHeight.toInt())

            val metrics = WindowMetrics(bounds, displayMetrics.density)

            val calculatedSizeClass = WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(metrics)

            assertEquals(sizeClass, calculatedSizeClass)
        }
    }

    @Test
    fun test_calculating_from_window_metrics_identity_w_medium_h_compact() {
        ActivityScenario.launch(TestActivity::class.java).onActivity { activity ->
            val displayMetrics = activity.resources.displayMetrics
            val sizeClass = WindowSizeClass(WIDTH_DP_MEDIUM_LOWER_BOUND, 0)

            val rawWidth =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeClass.minWidthDp.toFloat(),
                    displayMetrics
                )
            val rawHeight =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeClass.minHeightDp.toFloat(),
                    displayMetrics
                )
            val bounds = Rect(0, 0, rawWidth.toInt(), rawHeight.toInt())

            val metrics = WindowMetrics(bounds, displayMetrics.density)

            val calculatedSizeClass = WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(metrics)

            assertEquals(sizeClass, calculatedSizeClass)
        }
    }

    @Test
    fun test_calculating_from_window_metrics_identity_w_medium_h_medium() {
        ActivityScenario.launch(TestActivity::class.java).onActivity { activity ->
            val displayMetrics = activity.resources.displayMetrics
            val sizeClass =
                WindowSizeClass(WIDTH_DP_MEDIUM_LOWER_BOUND, HEIGHT_DP_MEDIUM_LOWER_BOUND)

            val rawWidth =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeClass.minWidthDp.toFloat(),
                    displayMetrics
                )
            val rawHeight =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeClass.minHeightDp.toFloat(),
                    displayMetrics
                )
            val bounds = Rect(0, 0, rawWidth.toInt(), rawHeight.toInt())

            val metrics = WindowMetrics(bounds, displayMetrics.density)

            val calculatedSizeClass = WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(metrics)

            assertEquals(sizeClass, calculatedSizeClass)
        }
    }

    @Test
    fun test_calculating_from_window_metrics_identity_w_medium_h_expanded() {
        ActivityScenario.launch(TestActivity::class.java).onActivity { activity ->
            val displayMetrics = activity.resources.displayMetrics
            val sizeClass =
                WindowSizeClass(WIDTH_DP_MEDIUM_LOWER_BOUND, HEIGHT_DP_EXPANDED_LOWER_BOUND)

            val rawWidth =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeClass.minWidthDp.toFloat(),
                    displayMetrics
                )
            val rawHeight =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeClass.minHeightDp.toFloat(),
                    displayMetrics
                )
            val bounds = Rect(0, 0, rawWidth.toInt(), rawHeight.toInt())

            val metrics = WindowMetrics(bounds, displayMetrics.density)

            val calculatedSizeClass = WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(metrics)

            assertEquals(sizeClass, calculatedSizeClass)
        }
    }

    @Test
    fun test_calculating_from_window_metrics_identity_w_expanded_h_compact() {
        ActivityScenario.launch(TestActivity::class.java).onActivity { activity ->
            val displayMetrics = activity.resources.displayMetrics
            val sizeClass = WindowSizeClass(WIDTH_DP_EXPANDED_LOWER_BOUND, 0)

            val rawWidth =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeClass.minWidthDp.toFloat(),
                    displayMetrics
                )
            val rawHeight =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeClass.minHeightDp.toFloat(),
                    displayMetrics
                )
            val bounds = Rect(0, 0, rawWidth.toInt(), rawHeight.toInt())

            val metrics = WindowMetrics(bounds, displayMetrics.density)

            val calculatedSizeClass = WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(metrics)

            assertEquals(sizeClass, calculatedSizeClass)
        }
    }

    @Test
    fun test_calculating_from_window_metrics_identity_w_expanded_h_medium() {
        ActivityScenario.launch(TestActivity::class.java).onActivity { activity ->
            val displayMetrics = activity.resources.displayMetrics
            val sizeClass =
                WindowSizeClass(WIDTH_DP_EXPANDED_LOWER_BOUND, HEIGHT_DP_MEDIUM_LOWER_BOUND)

            val rawWidth =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeClass.minWidthDp.toFloat(),
                    displayMetrics
                )
            val rawHeight =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeClass.minHeightDp.toFloat(),
                    displayMetrics
                )
            val bounds = Rect(0, 0, rawWidth.toInt(), rawHeight.toInt())

            val metrics = WindowMetrics(bounds, displayMetrics.density)

            val calculatedSizeClass = WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(metrics)

            assertEquals(sizeClass, calculatedSizeClass)
        }
    }

    @Test
    fun test_calculating_from_window_metrics_identity_w_expanded_h_expanded() {
        ActivityScenario.launch(TestActivity::class.java).onActivity { activity ->
            val displayMetrics = activity.resources.displayMetrics
            val sizeClass =
                WindowSizeClass(WIDTH_DP_EXPANDED_LOWER_BOUND, HEIGHT_DP_EXPANDED_LOWER_BOUND)

            val rawWidth =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeClass.minWidthDp.toFloat(),
                    displayMetrics
                )
            val rawHeight =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeClass.minHeightDp.toFloat(),
                    displayMetrics
                )
            val bounds = Rect(0, 0, rawWidth.toInt(), rawHeight.toInt())

            val metrics = WindowMetrics(bounds, displayMetrics.density)

            val calculatedSizeClass = WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(metrics)

            assertEquals(sizeClass, calculatedSizeClass)
        }
    }
}
