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

package androidx.paging

import androidx.paging.PagingConfig.Companion.MAX_SIZE_UNBOUNDED
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertFailsWith

@RunWith(JUnit4::class)
class PagingConfigTest {
    @Test
    fun defaults() {
        val config = PagingConfig(10)
        assertEquals(10, config.pageSize)
        assertEquals(30, config.initialLoadSize)
        assertEquals(true, config.enablePlaceholders)
        assertEquals(10, config.prefetchDistance)
        assertEquals(MAX_SIZE_UNBOUNDED, config.maxSize)
    }

    @Test
    fun requirePlaceholdersOrPrefetch() {
        assertFailsWith<IllegalArgumentException> {
            PagingConfig(
                pageSize = 10,
                enablePlaceholders = false,
                prefetchDistance = 0
            )
        }
    }

    @Test
    fun prefetchWindowMustFitInMaxSize() {
        assertFailsWith<IllegalArgumentException> {
            PagingConfig(
                pageSize = 3,
                prefetchDistance = 4,
                maxSize = 10
            )
        }
    }
}
