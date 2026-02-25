/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.annotation.registry

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class StablePdfAnnotationHandleRegistryTest {
    private lateinit var registry: StablePdfAnnotationHandleRegistry

    private val pageNum = 0

    @Before
    fun setup() {
        registry = StablePdfAnnotationHandleRegistry()
    }

    @Test
    fun getHandleId_newSource_generatesNewHandle() {
        val sourceId = "source_123"

        val handle = registry.getHandleId(pageNum, sourceId)

        assertThat(handle).isNotEmpty()
        assertThat(handle).isNotEqualTo(sourceId)
    }

    @Test
    fun getHandleId_existingSource_returnsSameHandle() {
        val sourceId = "source_123"

        val handle1 = registry.getHandleId(pageNum, sourceId)

        val handle2 = registry.getHandleId(pageNum, sourceId)

        assertThat(handle1).isEqualTo(handle2)
    }

    @Test
    fun getHandleId_differentSources_returnsDifferentHandles() {
        val sourceA = "source_A"
        val sourceB = "source_B"

        val handleA = registry.getHandleId(pageNum, sourceA)
        val handleB = registry.getHandleId(pageNum, sourceB)

        assertThat(handleA).isNotEqualTo(handleB)
    }

    @Test
    fun getSourceId_existingHandle_returnsCorrectSource() {
        val sourceId = "source_123"
        val handle = registry.getHandleId(pageNum, sourceId)

        val retrievedSource = registry.getSourceId(handle)

        assertThat(retrievedSource).isEqualTo(sourceId)
    }

    @Test
    fun getSourceId_unknownHandle_returnsNull() {
        val result = registry.getSourceId("unknown_handle_999")
        assertThat(result).isNull()
    }

    @Test
    fun clear_removesAllMappings() {
        val sourceId = "source_123"
        val handle = registry.getHandleId(pageNum, sourceId)

        registry.clear()

        assertThat(registry.getSourceId(handle)).isNull()

        val newHandle = registry.getHandleId(pageNum, sourceId)
        assertThat(newHandle).isNotEqualTo(handle)
    }

    @Test
    fun getHandleId_concurrentAccess_sameSource_shouldReturnConsistentHandle() = runBlocking {
        val sourceId = "concurrent_source"
        val threadCount = 100

        val deferredHandles =
            (1..threadCount).map {
                async(Dispatchers.IO) { registry.getHandleId(pageNum, sourceId) }
            }

        val results = deferredHandles.awaitAll()

        val firstHandle = results.first()
        results.forEach { handle -> assertThat(handle).isEqualTo(firstHandle) }
    }

    @Test
    fun getHandleId_concurrentAccess_differentSources_shouldNotCrash() = runBlocking {
        val threadCount = 100

        val deferredHandles =
            (1..threadCount).map { i ->
                async(Dispatchers.IO) {
                    val sourceId = "source_$i"
                    registry.getHandleId(pageNum, sourceId)
                }
            }

        val results = deferredHandles.awaitAll()

        assertThat(results).hasSize(threadCount)
        assertThat(results.toSet()).hasSize(threadCount)
    }

    @Test
    fun getHandleId_sameSourceDifferentPages_returnsDifferentHandles() {
        val sourceId = "common_source_id"
        val page1 = 0
        val page2 = 1

        val handle1 = registry.getHandleId(page1, sourceId)
        val handle2 = registry.getHandleId(page2, sourceId)

        assertThat(handle1).isNotEqualTo(handle2)

        assertThat(registry.getSourceId(handle1)).isEqualTo(sourceId)
        assertThat(registry.getSourceId(handle2)).isEqualTo(sourceId)
    }
}
