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

@file:OptIn(DelicateTracingApi::class)

package androidx.lifecycle

import androidx.kruth.assertThat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.tracing.AbstractTraceSink
import androidx.tracing.DelicateTracingApi
import androidx.tracing.PooledTracePacketArray
import androidx.tracing.Tracer
import androidx.tracing.wire.TraceDriver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ViewModelTracerTest {

    private val sink = RecordingTraceSink()
    private val driver = TraceDriver(sink = sink)
    private val factory = viewModelFactory {
        initializer { TracedViewModel1() }
        initializer { TracedViewModel2() }
    }

    @BeforeTest
    fun setUp() {
        Tracer.resetGlobalTracer()
        Tracer.setGlobalTracer(driver.tracer)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
        Tracer.resetGlobalTracer()
    }

    @Test
    fun get_withDefaultKey_emitsCreateTraceWithMetadata() {
        val provider = ViewModelProvider.create(store = ViewModelStore(), factory = factory)

        provider[TracedViewModel1::class]
        driver.flush()

        val createEvents = sink.beginSectionEvents("ViewModelProvider#create")
        assertThat(createEvents).hasSize(1)
        val event = createEvents.single()
        assertThat(event.primaryCategory).isEqualTo("androidx.lifecycle.viewmodel")
        assertThat(event.metadata)
            .containsExactly(
                "key" to
                    "androidx.lifecycle.ViewModelProvider.DefaultKey:${TracedViewModel1::class.qualifiedName}",
                "class" to "TracedViewModel1",
                "factory" to "InitializerViewModelFactory",
            )
    }

    @Test
    fun get_withCustomKey_emitsCreateTraceWithCustomKey() {
        val provider = ViewModelProvider.create(store = ViewModelStore(), factory = factory)

        provider["custom_key", TracedViewModel1::class]
        driver.flush()

        val createEvents = sink.beginSectionEvents("ViewModelProvider#create")
        assertThat(createEvents).hasSize(1)
        val event = createEvents.single()
        assertThat(event.primaryCategory).isEqualTo("androidx.lifecycle.viewmodel")
        assertThat(event.metadata)
            .containsExactly(
                "key" to "custom_key",
                "class" to "TracedViewModel1",
                "factory" to "InitializerViewModelFactory",
            )
    }

    @Test
    fun get_whenCached_doesNotEmitSecondCreateTrace() {
        val provider = ViewModelProvider.create(store = ViewModelStore(), factory = factory)

        provider["cached_key", TracedViewModel1::class]
        provider["cached_key", TracedViewModel1::class]
        driver.flush()

        val createEvents = sink.beginSectionEvents("ViewModelProvider#create")
        assertThat(createEvents).hasSize(1)
    }

    @Test
    fun get_withSameKeyAndDifferentModelClass_emitsCreateTraceForBothModels() {
        val provider = ViewModelProvider.create(store = ViewModelStore(), factory = factory)

        provider["shared_key", TracedViewModel1::class]
        provider["shared_key", TracedViewModel2::class]
        driver.flush()

        val createEvents = sink.beginSectionEvents("ViewModelProvider#create")
        assertThat(createEvents).hasSize(2)
        assertThat(createEvents[0].metadata["class"]).isEqualTo("TracedViewModel1")
        assertThat(createEvents[1].metadata["class"]).isEqualTo("TracedViewModel2")
    }

    private class TracedViewModel1 : ViewModel()

    private class TracedViewModel2 : ViewModel()

    private data class RecordedTraceEvent(
        val type: Int,
        val primaryCategory: String,
        val name: String?,
        val metadata: Map<String, String>,
    )

    private class RecordingTraceSink : AbstractTraceSink() {
        val events = mutableListOf<RecordedTraceEvent>()

        fun beginSectionEvents(name: String): List<RecordedTraceEvent> = events.filter {
            it.type == TYPE_BEGIN && it.name == name
        }

        override fun enqueue(pooledPacketArray: PooledTracePacketArray) {
            pooledPacketArray.forEach { event ->
                val metadata = buildMap {
                    for (index in 0..event.lastMetadataEntryIndex) {
                        val entry = event.metadataEntries[index]
                        val entryName = entry.name
                        if (entryName != null) {
                            put(entryName, entry.stringValue)
                        }
                    }
                }
                events +=
                    RecordedTraceEvent(
                        type = event.type,
                        primaryCategory = event.primaryCategory,
                        name = event.name,
                        metadata = metadata,
                    )
            }
            pooledPacketArray.recycle()
        }

        override fun onDroppedTraceEvent() {}

        override fun flush() {}

        override fun close() {}

        private companion object {
            const val TYPE_BEGIN = 1
        }
    }
}
