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

package androidx.compose.runtime.benchmark

import androidx.benchmark.junit4.measureRepeated
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.OrderWith
import org.junit.runner.RunWith
import org.junit.runner.manipulation.Alphanumeric

@LargeTest
@OrderWith(Alphanumeric::class)
@RunWith(AndroidJUnit4::class)
class SnapshotStateAutoboxingBenchmark : ComposeBenchmarkBase() {

    companion object {
        private const val FramesInTenSecondAnimation = 60 * 10 // 60 FPS * 10 Seconds
        private const val FramesInTenSecondHighRefreshAnimation = 120 * 10 // 120 FPS * 10 Seconds
        private const val ManyWritesCount = 1_000_000
    }

    private lateinit var primitiveFloatState: MutableFloatState
    private lateinit var autoboxingFloatState: MutableState<Float>

    @Before
    fun setup() {
        primitiveFloatState = mutableFloatStateOf(-1.0f)
        autoboxingFloatState = mutableStateOf(-1.0f)
    }

    @Test
    fun benchmarkTenSecondAnimationWorthOfBoxedFloatValueWrites() {
        benchmarkBoxedWrites(FramesInTenSecondAnimation)
    }

    @Test
    fun benchmarkTenSecondAnimationWorthOfPrimitiveFloatValueWrites() {
        benchmarkPrimitiveWrites(FramesInTenSecondAnimation)
    }

    @Test
    fun benchmarkTenSecondHighRefreshAnimationWorthOfBoxedFloatValueWrites() {
        benchmarkBoxedWrites(FramesInTenSecondHighRefreshAnimation)
    }

    @Test
    fun benchmarkTenSecondHighRefreshAnimationWorthOfPrimitiveFloatValueWrites() {
        benchmarkPrimitiveWrites(FramesInTenSecondHighRefreshAnimation)
    }

    @Test
    fun benchmarkManyBoxedFloatValueWrites() {
        benchmarkBoxedWrites(ManyWritesCount)
    }

    @Test
    fun benchmarkManyPrimitiveFloatValueWrites() {
        benchmarkPrimitiveWrites(ManyWritesCount)
    }

    private fun benchmarkPrimitiveWrites(count: Int) {
        benchmarkRule.measureRepeated {
            var value = 0f
            while (value < count) {
                primitiveFloatState.floatValue = value++
            }
        }
    }

    private fun benchmarkBoxedWrites(count: Int) {
        benchmarkRule.measureRepeated {
            var value = 0f
            while (value < count) {
                autoboxingFloatState.value = value++
            }
        }
    }
}