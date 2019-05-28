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

import androidx.test.filters.SmallTest
import junit.framework.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class ClocksTest {
    @Test
    fun allSimilarCoresSameFreqs() {
        assertTrue(Clocks.isCpuLocked(
            listOf(
                Clocks.CoreDir(true, listOf(1, 2, 3), 2),
                Clocks.CoreDir(true, listOf(1, 2, 3), 2),
                Clocks.CoreDir(true, listOf(1, 2, 3), 2),
                Clocks.CoreDir(true, listOf(1, 2, 3), 2)
            )
        ))
    }

    @Test
    fun differentMaxFrequencies() {
        assertFalse(Clocks.isCpuLocked(
            listOf(
                Clocks.CoreDir(true, listOf(1, 2, 3), 2),
                Clocks.CoreDir(true, listOf(1, 2, 3), 2),
                Clocks.CoreDir(true, listOf(1, 2), 2),
                Clocks.CoreDir(true, listOf(1, 2), 2)
            )
        ))
    }

    @Test
    fun differentCurrentMinFrequencies() {
        assertFalse(Clocks.isCpuLocked(
            listOf(
                Clocks.CoreDir(true, listOf(1, 2, 3), 3),
                Clocks.CoreDir(true, listOf(1, 2, 3), 3),
                Clocks.CoreDir(true, listOf(1, 2, 3), 3),
                Clocks.CoreDir(true, listOf(1, 2, 3), 2)
            )
        ))
    }

    @Test
    fun currentMinEqualsMinAvailable() {
        assertFalse(Clocks.isCpuLocked(
            listOf(
                Clocks.CoreDir(true, listOf(1, 2, 3), 1),
                Clocks.CoreDir(true, listOf(1, 2, 3), 1),
                Clocks.CoreDir(true, listOf(1, 2, 3), 1),
                Clocks.CoreDir(true, listOf(1, 2, 3), 1)
            )
        ))
    }
}
