/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.window.layout.util

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.window.layout.WindowMetricsCalculator
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Tests for the [EngagementModeHelper] class. */
@SmallTest
@RunWith(AndroidJUnit4::class)
class EngagementModeHelperTest {

    @Test
    fun testHasLargeEnoughDisplay_true() {
        val context = mock<Context>()
        val resources = mock<Resources>()
        val windowMetricsCalculator = mock<WindowMetricsCalculator>()
        val metrics =
            DisplayMetrics().apply {
                widthPixels = 3000
                heightPixels = 2000
                xdpi = 200f
                ydpi = 200f
            }
        whenever(context.resources).thenReturn(resources)
        whenever(resources.displayMetrics).thenReturn(metrics)
        whenever(windowMetricsCalculator.computeMaximumWindowMetrics(context))
            .thenReturn(WindowMetricsCalculator.fromDisplayMetrics(metrics))

        val helper = EngagementModeHelperImpl(windowMetricsCalculator)

        assertThat(helper.hasLargeEnoughDisplay(context)).isTrue()
    }

    @Test
    fun testHasLargeEnoughDisplay_false() {
        val context = mock<Context>()
        val resources = mock<Resources>()
        val windowMetricsCalculator = mock<WindowMetricsCalculator>()
        val metrics =
            DisplayMetrics().apply {
                widthPixels = 1000
                heightPixels = 500
                xdpi = 200f
                ydpi = 200f
            }
        whenever(context.resources).thenReturn(resources)
        whenever(resources.displayMetrics).thenReturn(metrics)
        whenever(windowMetricsCalculator.computeMaximumWindowMetrics(context))
            .thenReturn(WindowMetricsCalculator.fromDisplayMetrics(metrics))

        val helper = EngagementModeHelperImpl(windowMetricsCalculator)
        assertThat(helper.hasLargeEnoughDisplay(context)).isFalse()
    }
}
