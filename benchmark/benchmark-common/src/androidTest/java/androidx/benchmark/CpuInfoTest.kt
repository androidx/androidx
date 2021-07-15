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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class CpuInfoTest {
    @Test
    fun allSimilarCoresSameFreqs() {
        assertTrue(
            CpuInfo.isCpuLocked(
                listOf(
                    CpuInfo.CoreDir("", true, listOf(1, 2, 3), 2, 1000),
                    CpuInfo.CoreDir("", true, listOf(1, 2, 3), 2, 1000),
                    CpuInfo.CoreDir("", true, listOf(1, 2, 3), 2, 1000),
                    CpuInfo.CoreDir("", true, listOf(1, 2, 3), 2, 1000)
                )
            )
        )
    }

    @Test
    fun differentCurrentMinFrequencies_similarCoresLockedDifferently() {
        assertFalse(
            CpuInfo.isCpuLocked(
                listOf(
                    CpuInfo.CoreDir("", true, listOf(1, 2, 3), 3, 1000),
                    CpuInfo.CoreDir("", true, listOf(1, 2, 3), 3, 1000),
                    CpuInfo.CoreDir("", true, listOf(1, 2, 3), 3, 1000),
                    CpuInfo.CoreDir("", true, listOf(1, 2, 3), 2, 1000)
                )
            )
        )
    }

    @Test
    fun differentCurrentMinFrequencies_differentCoresLockedSimilarly() {
        assertTrue(
            CpuInfo.isCpuLocked(
                listOf(
                    CpuInfo.CoreDir("", true, listOf(1, 2, 3), 2, 1000),
                    CpuInfo.CoreDir("", true, listOf(1, 2, 3), 2, 1000),
                    CpuInfo.CoreDir("", true, listOf(2, 3), 3, 1000),
                    CpuInfo.CoreDir("", true, listOf(2, 3), 3, 1000)
                )
            )
        )
    }

    @Test
    fun currentMinEqualsMinAvailable() {
        assertFalse(
            CpuInfo.isCpuLocked(
                listOf(
                    CpuInfo.CoreDir("", true, listOf(1, 2, 3), 1, 1000),
                    CpuInfo.CoreDir("", true, listOf(1, 2, 3), 1, 1000),
                    CpuInfo.CoreDir("", true, listOf(1, 2, 3), 1, 1000),
                    CpuInfo.CoreDir("", true, listOf(1, 2, 3), 1, 1000)
                )
            )
        )
    }
}
