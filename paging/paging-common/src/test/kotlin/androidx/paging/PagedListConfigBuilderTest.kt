/*
 * Copyright 2018 The Android Open Source Project
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

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PagedListConfigBuilderTest {
    @Test
    fun defaults() {
        @Suppress("DEPRECATION")
        val config = PagedList.Config.Builder()
            .setPageSize(10)
            .build()
        Assert.assertEquals(10, config.pageSize)
        Assert.assertEquals(30, config.initialLoadSizeHint)
        Assert.assertEquals(true, config.enablePlaceholders)
        Assert.assertEquals(10, config.prefetchDistance)
        @Suppress("DEPRECATION")
        Assert.assertEquals(PagedList.Config.MAX_SIZE_UNBOUNDED, config.maxSize)
    }

    @Test(expected = IllegalArgumentException::class)
    fun maxSizeTooSmall() {
        @Suppress("DEPRECATION")
        PagedList.Config.Builder()
            .setPageSize(20)
            .setPrefetchDistance(15)
            .setMaxSize(49)
            .build()
    }

    @Test
    fun maxSizeAccepted() {
        @Suppress("DEPRECATION")
        PagedList.Config.Builder()
            .setPageSize(20)
            .setPrefetchDistance(15)
            .setMaxSize(50)
            .build()
    }
}
