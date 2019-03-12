/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.InstrumentationRegistry
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class BitmapBenchmark {

    companion object {
        const val JETPACK = "images/jetpack.png"
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val context: Context = InstrumentationRegistry.getTargetContext()

    @Test
    fun bitmapGetPixelBenchmark() {
        val inputStream = context.assets.open(JETPACK)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val pixels = IntArray(100) { it }
        benchmarkRule.measure {
            pixels.map { bitmap.getPixel(it, 0) }
        }
        inputStream.close()
    }

    @Test
    fun bitmapGetPixelsBenchmark() {
        val inputStream = context.assets.open(JETPACK)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val pixels = IntArray(100) { it }
        benchmarkRule.measure {
            bitmap.getPixels(pixels, 0, 100, 0, 0, 100, 1)
        }
        inputStream.close()
    }
}
