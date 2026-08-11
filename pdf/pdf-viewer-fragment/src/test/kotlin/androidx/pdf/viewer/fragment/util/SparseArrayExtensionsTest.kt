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

package androidx.pdf.viewer.fragment.util

import android.graphics.RectF
import android.util.SparseArray
import androidx.pdf.content.PageMatchBounds
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SparseArrayExtensionsTest {

    private fun createMatch(textStartIndex: Int = 0): PageMatchBounds =
        PageMatchBounds(bounds = listOf(RectF(10f, 20f, 30f, 40f)), textStartIndex = textStartIndex)

    @Test
    fun countTotalElements_emptySparseArray_returnsZero() {
        val sparseArray = SparseArray<List<PageMatchBounds>>()
        assertThat(sparseArray.countTotalElements()).isEqualTo(0)
    }

    @Test
    fun countTotalElements_pageWithEmptyList_returnsZero() {
        val sparseArray = SparseArray<List<PageMatchBounds>>().apply { put(0, emptyList()) }
        assertThat(sparseArray.countTotalElements()).isEqualTo(0)
    }

    @Test
    fun countTotalElements_singlePage_returnsCorrectCount() {
        val sparseArray =
            SparseArray<List<PageMatchBounds>>().apply {
                put(0, listOf(createMatch(0), createMatch(1), createMatch(2)))
            }
        assertThat(sparseArray.countTotalElements()).isEqualTo(3)
    }

    @Test
    fun countTotalElements_multiplePages_returnsSumOfCounts() {
        val sparseArray =
            SparseArray<List<PageMatchBounds>>().apply {
                put(0, listOf(createMatch(0), createMatch(1)))
                put(2, listOf(createMatch(0), createMatch(1), createMatch(2)))
                put(5, listOf(createMatch(0)))
            }
        assertThat(sparseArray.countTotalElements()).isEqualTo(6)
    }

    @Test
    fun getFlattenedIndex_emptySparseArray_returnsZero() {
        val sparseArray = SparseArray<List<PageMatchBounds>>()
        assertThat(sparseArray.getFlattenedIndex(selectedResultPageNum = 0, resultIndex = 0))
            .isEqualTo(0)
    }

    @Test
    fun getFlattenedIndex_firstMatchOnFirstPage_returnsZero() {
        val sparseArray =
            SparseArray<List<PageMatchBounds>>().apply {
                put(0, listOf(createMatch(0), createMatch(1)))
                put(1, listOf(createMatch(0)))
            }
        assertThat(sparseArray.getFlattenedIndex(selectedResultPageNum = 0, resultIndex = 0))
            .isEqualTo(0)
    }

    @Test
    fun getFlattenedIndex_laterMatchOnFirstPage_returnsResultIndex() {
        val sparseArray =
            SparseArray<List<PageMatchBounds>>().apply {
                put(0, listOf(createMatch(0), createMatch(1), createMatch(2)))
                put(1, listOf(createMatch(0)))
            }
        assertThat(sparseArray.getFlattenedIndex(selectedResultPageNum = 0, resultIndex = 2))
            .isEqualTo(2)
    }

    @Test
    fun getFlattenedIndex_matchOnSubsequentPages_accumulatesPreviousPageCounts() {
        val sparseArray =
            SparseArray<List<PageMatchBounds>>().apply {
                put(1, listOf(createMatch(0), createMatch(1))) // 2 matches on page 1
                put(
                    3,
                    listOf(createMatch(0), createMatch(1), createMatch(2)),
                ) // 3 matches on page 3
                put(7, listOf(createMatch(0), createMatch(1))) // 2 matches on page 7
            }

        // First match on page 3 -> 2 previous matches + index 0 = 2
        assertThat(sparseArray.getFlattenedIndex(selectedResultPageNum = 3, resultIndex = 0))
            .isEqualTo(2)

        // Second match on page 3 -> 2 previous matches + index 1 = 3
        assertThat(sparseArray.getFlattenedIndex(selectedResultPageNum = 3, resultIndex = 1))
            .isEqualTo(3)

        // Third match on page 3 -> 2 previous matches + index 2 = 4
        assertThat(sparseArray.getFlattenedIndex(selectedResultPageNum = 3, resultIndex = 2))
            .isEqualTo(4)

        // Second match on page 7 -> 2 + 3 previous matches + index 1 = 6
        assertThat(sparseArray.getFlattenedIndex(selectedResultPageNum = 7, resultIndex = 1))
            .isEqualTo(6)
    }

    @Test
    fun getFlattenedIndex_pageSmallerThanAllKeys_returnsResultIndex() {
        val sparseArray =
            SparseArray<List<PageMatchBounds>>().apply {
                put(5, listOf(createMatch(0), createMatch(1)))
            }
        assertThat(sparseArray.getFlattenedIndex(selectedResultPageNum = 2, resultIndex = 1))
            .isEqualTo(1)
    }
}
