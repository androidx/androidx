/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.datastore.core

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
class SingleProcessDataStoreStressTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @get:Rule
    val timeout = Timeout(4, TimeUnit.MINUTES)

    @Test
    fun testManyConcurrentReadsAndWrites() = runBlocking<Unit> {
        val myScope = CoroutineScope(
            Job() + Executors.newFixedThreadPool(4).asCoroutineDispatcher()
        )

        val file = tempFolder.newFile()
        file.delete()

        val dataStore = DataStoreFactory.create(
            serializer = LongSerializer(failWrites = false, failReads = false),
            scope = myScope
        ) { file }

        val readers = mutableListOf<Deferred<*>>()
        val writers = mutableListOf<Deferred<*>>()

        repeat(100) {
            readers += myScope.async {
                dataStore.data.takeWhile {
                    it != 50_000L
                }.reduce { acc, value ->
                    assertThat(acc).isLessThan(value)
                    value
                }
            }
        }

        repeat(1000) {
            writers += myScope.async {
                repeat(50) {
                    dataStore.updateData {
                        it.inc()
                    }
                }
            }
        }

        readers.awaitAll()
        writers.awaitAll()
    }

    @Test
    fun testManyConcurrentReadsAndWrites_withIntermittentWriteFailures() = runBlocking<Unit> {
        val myScope = CoroutineScope(
            Job() + Executors.newFixedThreadPool(4).asCoroutineDispatcher()
        )

        val file = tempFolder.newFile()
        file.delete()

        val serializer = LongSerializer(false, false)

        val dataStore = DataStoreFactory.create(
            serializer = serializer,
            scope = myScope
        ) { file }

        val readers = mutableListOf<Deferred<*>>()
        val writers = mutableListOf<Deferred<*>>()

        repeat(100) {
            readers += myScope.async {
                dataStore.data.takeWhile {
                    it != 10_000L
                }.reduce { acc, value ->
                    assertThat(acc).isLessThan(value)
                    value
                }
            }
        }

        repeat(1000) {
            writers += myScope.async {
                repeat(10) {
                    var success = false
                    while (!success) {
                        try {
                            dataStore.updateData { it.inc() }
                            success = true
                        } catch (expected: IOException) {
                        }
                    }
                }
            }
        }

        val intermittentWriteFailures = myScope.launch {
            while (true) {
                ensureActive()
                delay(10)
                serializer.failWrites = !serializer.failWrites
            }
        }

        readers.awaitAll()
        writers.awaitAll()

        intermittentWriteFailures.cancelAndJoin()
    }

    @Test
    fun testManyConcurrentReadsAndWrites_withBeginningReadFailures() = runBlocking<Unit> {
        val myScope = CoroutineScope(
            Job() + Executors.newFixedThreadPool(4).asCoroutineDispatcher()
        )

        val file = tempFolder.newFile()
        file.delete()

        val serializer = LongSerializer(failWrites = false, failReads = true)

        val dataStore = DataStoreFactory.create(
            serializer = serializer,
            scope = myScope
        ) { file }

        val readers = mutableListOf<Deferred<*>>()
        val writers = mutableListOf<Deferred<*>>()

        repeat(100) {
            readers += myScope.async {
                dataStore.data
                    .retry { true }
                    .takeWhile {
                        it != 1_000L
                    }.reduce { acc, value ->
                        assertThat(acc).isLessThan(value)
                        value
                    }
            }
        }

        repeat(100) {
            writers += myScope.async {
                repeat(10) {
                    var success = false
                    while (!success) {
                        try {
                            dataStore.updateData { it.inc() }
                            success = true
                        } catch (expected: IOException) {
                        }
                    }
                }
            }
        }

        // Read failures for first 100 ms
        delay(100)
        serializer.failReads = false

        readers.awaitAll()
        writers.awaitAll()
    }

    private class LongSerializer(
        @Volatile var failWrites: Boolean,
        @Volatile var failReads: Boolean
    ) :
        Serializer<Long> {
        override val defaultValue = 0L

        override fun readFrom(input: InputStream): Long {
            if (failReads) {
                throw IOException("failing read")
            }
            return DataInputStream(input).readLong()
        }

        override fun writeTo(t: Long, output: OutputStream) {
            if (failWrites) {
                throw IOException("failing write")
            }
            DataOutputStream(output).writeLong(t)
        }
    }
}