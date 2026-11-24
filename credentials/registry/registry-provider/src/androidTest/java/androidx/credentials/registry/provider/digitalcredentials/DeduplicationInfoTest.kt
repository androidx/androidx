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

package androidx.credentials.registry.provider.digitalcredentials

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class DeduplicationInfoTest {
    @Test
    fun construction_defaultRank_success() {
        val info = DeduplicationInfo(id = "dedup-id")

        assertThat(info.id).isEqualTo("dedup-id")
        assertThat(info.rank).isEqualTo(0)
    }

    @Test
    fun construction_withRank_success() {
        val info = DeduplicationInfo(id = "dedup-id", rank = 5)

        assertThat(info.id).isEqualTo("dedup-id")
        assertThat(info.rank).isEqualTo(5)
    }

    @Test
    fun construction_emptyId_throws() {
        assertThrows(IllegalArgumentException::class.java) { DeduplicationInfo(id = "") }
    }

    @Test
    fun construction_negativeRank_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            DeduplicationInfo(id = "dedup-id", rank = -1)
        }
    }

    @Test
    fun equalsAndHashCode() {
        val info1 = DeduplicationInfo(id = "dedup-id", rank = 3)
        val info2 = DeduplicationInfo(id = "dedup-id", rank = 3)
        val infoDifferentId = DeduplicationInfo(id = "other-id", rank = 3)
        val infoDifferentRank = DeduplicationInfo(id = "dedup-id", rank = 4)

        assertThat(info1).isEqualTo(info2)
        assertThat(info1.hashCode()).isEqualTo(info2.hashCode())
        assertThat(info1).isNotEqualTo(infoDifferentId)
        assertThat(info1).isNotEqualTo(infoDifferentRank)
    }
}
