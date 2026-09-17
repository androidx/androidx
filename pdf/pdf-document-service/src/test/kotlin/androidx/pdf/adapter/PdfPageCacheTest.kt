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

package androidx.pdf.adapter

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PdfPageCacheTest {

    @Test
    fun clearAll_closesCachedPagesAndRemovesFromCache() {
        val cache = PdfPageCache()
        val page0 = FakePdfPage(pageNum = 0, height = 100, width = 100)
        val page1 = FakePdfPage(pageNum = 1, height = 100, width = 100)

        cache.getOrUpdate(0, useCache = true) { page0 }
        cache.getOrUpdate(1, useCache = true) { page1 }

        cache.clearAll()

        assertThat(page0.isClosed).isTrue()
        assertThat(page0.closeCount).isEqualTo(1)
        assertThat(page1.isClosed).isTrue()
        assertThat(page1.closeCount).isEqualTo(1)

        // Subsequent remove should return null so releasePage does not close them a second time
        assertThat(cache.remove(0)).isNull()
        assertThat(cache.remove(1)).isNull()
    }

    @Test
    fun getOrUpdate_afterClearAll_createsNewPage() {
        val cache = PdfPageCache()
        val firstPage = FakePdfPage(pageNum = 0, height = 100, width = 100)
        cache.getOrUpdate(0, useCache = true) { firstPage }

        cache.clearAll()

        val secondPage = FakePdfPage(pageNum = 0, height = 200, width = 200)
        val retrieved = cache.getOrUpdate(0, useCache = true) { secondPage }

        assertThat(retrieved).isSameInstanceAs(secondPage)
        assertThat(retrieved.isClosed).isFalse()
    }
}
