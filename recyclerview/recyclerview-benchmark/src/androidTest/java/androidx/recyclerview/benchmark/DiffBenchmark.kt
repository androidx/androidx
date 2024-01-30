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

package androidx.recyclerview.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class DiffBenchmark(
    val input: Input
) {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun runDiff() {
        benchmarkRule.measureRepeated {
            val result = DiffUtil.calculateDiff(input.callback, input.detectMoves)
            if (input.dispatchUpdates) {
                result.dispatchUpdatesTo(dummyUpdateCallback)
            }
        }
    }

    companion object {
        private val dummyUpdateCallback = object : ListUpdateCallback {
            override fun onChanged(position: Int, count: Int, payload: Any?) {
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
            }

            override fun onInserted(position: Int, count: Int) {
            }

            override fun onRemoved(position: Int, count: Int) {
            }
        }

        @JvmStatic
        @Parameterized.Parameters(name = "input_{0}")
        fun params() = listOf(
            Input(
                name = "no_changes",
                before = (0..1000).toList(),
                after = (0..1000).toList()
            ),
            Input(
                name = "prepend",
                before = (0..1000).toList(),
                after = (-100..-1).toList() + (0..1000)
            ),
            Input(
                name = "append",
                before = (0..1000).toList(),
                after = (0..1000).toList() + (0..100)
            ),
            Input(
                name = "move_large_chunk",
                before = (0..1000).toList(),
                after = (0..200).toList() + (301..1000).toList() + (201..300).toList()
            ),
            Input(
                name = "delete_from_middle",
                before = (0..1000).toList(),
                after = (0..200).toList() + (301..1000).toList()
            ),
            Input(
                name = "delete_1_item",
                before = (0..1000).toList(),
                after = (0..299).toList() + (301..1000).toList()
            ),
            Input(
                name = "move_from_beginning_to_end",
                before = (0..1000).toList(),
                after = (100..1000).toList() + (0..99).toList()
            ),
            Input(
                name = "move_from_end_to_beginning",
                before = (0..1000).toList(),
                after = (900..1000).toList() + (0..899).toList()
            )
        ).flatMap {
            listOf(
                it,
                it.copy(detectMoves = false)
            )
        }.flatMap {
            listOf(
                it,
                it.copy(dispatchUpdates = false)
            )
        }
    }

    data class Input(
        val name: String,
        val before: List<Int>,
        val after: List<Int>,
        val dispatchUpdates: Boolean = true,
        val detectMoves: Boolean = true
    ) {
        val callback = object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                before[oldItemPosition] == after[newItemPosition]

            override fun getOldListSize() = before.size

            override fun getNewListSize() = after.size

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                before[oldItemPosition] == after[newItemPosition]
        }

        override fun toString() = name +
            "_dispatchUpdates_$dispatchUpdates" +
            "_detectMoves_$detectMoves" +
            "_size_[${before.size}_${after.size}]"
    }
}
