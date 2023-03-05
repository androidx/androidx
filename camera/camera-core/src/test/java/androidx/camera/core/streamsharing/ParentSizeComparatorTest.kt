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

package androidx.camera.core.streamsharing

import android.os.Build
import android.util.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [ParentSizeComparator].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ParentSizeComparatorTest {

    companion object {
        const val PREVIEW_PRIORITY = 2
        const val VIDEO_PRIORITY = 5
        val SIZE_1600_900 = Size(1600, 900)
        val SIZE_1800_900 = Size(1800, 900)
        val SIZE_800_600 = Size(800, 600)
        val SIZE_400_300 = Size(400, 300)
        val SIZE_500_400 = Size(500, 400)
        val SIZE_1024_768 = Size(1024, 768)
        val SIZE_2048_1536 = Size(2048, 1536)
        val SIZES = listOf(
            SIZE_1800_900,
            SIZE_1600_900,
            SIZE_500_400,
            SIZE_400_300,
            SIZE_800_600,
            SIZE_2048_1536,
            SIZE_1024_768
        )
        val PRIORITIES = listOf(PREVIEW_PRIORITY, VIDEO_PRIORITY)
    }

    @Test
    fun priorityIsSorted() {
        val comparator = ParentSizeComparator(mapOf(), PRIORITIES)
        assertThat(comparator.sortedPriorities).containsExactly(5, 2).inOrder()
    }

    @Test
    fun sizeSortedByPriorityRankingAndSize() {
        val map = mapOf(
            // Video has higher priority so its ranking is the deciding factor.
            Pair(SIZE_500_400, mapOf(Pair(VIDEO_PRIORITY, 0), Pair(PREVIEW_PRIORITY, 3))),
            // When video ranking is the same, preview ranking is the deciding factor.
            Pair(SIZE_1800_900, mapOf(Pair(VIDEO_PRIORITY, 1), Pair(PREVIEW_PRIORITY, 1))),
            Pair(SIZE_1600_900, mapOf(Pair(VIDEO_PRIORITY, 1), Pair(PREVIEW_PRIORITY, 2))),
            // When preview ranking is the same or missing, the size is the deciding factor.
            Pair(SIZE_800_600, mapOf(Pair(VIDEO_PRIORITY, 2), Pair(PREVIEW_PRIORITY, 1))),
            Pair(SIZE_400_300, mapOf(Pair(VIDEO_PRIORITY, 2), Pair(PREVIEW_PRIORITY, 1))),
            Pair(SIZE_1024_768, mapOf()),
            Pair(SIZE_2048_1536, mapOf()),
        )
        val result = SIZES.sortedWith(ParentSizeComparator(map, PRIORITIES))
        assertThat(result).containsExactly(
            SIZE_500_400,
            SIZE_1800_900,
            SIZE_1600_900,
            SIZE_400_300,
            SIZE_800_600,
            SIZE_2048_1536,
            SIZE_1024_768
        ).inOrder()
    }
}