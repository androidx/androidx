/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.core.impl.utils

import android.util.Range
import androidx.camera.core.impl.utils.RangeUtil.filterFixedRanges
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
class RangeUtilTest {

    @Test
    fun filterFixedRanges_filtersCorrectly() {
        // Arrange.
        val originalSet =
            setOf(
                Range(10, 20), // Not fixed
                Range(30, 30), // Fixed
                Range(40, 50), // Not fixed
                Range(60, 60), // Fixed
            )

        // Act.
        val result = originalSet.filterFixedRanges()

        // Assert.
        assertThat(result).containsExactly(Range(30, 30), Range(60, 60))
    }
}
