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

import androidx.kruth.assertThat
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Ignore
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SingleProcessDataStoreStressTest {
    @get:Rule val tempFolder = TemporaryFolder()

    @get:Rule val timeout = Timeout(4, TimeUnit.MINUTES)

    // using real dispatchers here to test parallelism
    private val testScope = CoroutineScope(Job() + Dispatchers.IO)

    @After
    fun cancelTestScope() {
        testScope.cancel()
    }

    @Ignore // b/368361169
    @Test
    fun testManyConcurrentReadsAndWrites() =
        runBlocking<Unit> {
            val file = tempFolder.newFile()
            file.delete()

            val dataStore =
                DataStoreFactory.create(
                    serializer = LongSerializer(failWrites = false, failReads = false),
                    scope = testScope
                ) {
                    file
                }
            assertThat(dataStore.data.first()).isEqualTo(0)

            val readers =
                (0 until READER_COUNT).map {
                    testScope.async {
                        dataStore.data
                            .takeWhile { it < FINAL_TEST_VALUE }
                            .assertIncreasingAfterFirstRead()
                    }
                }
            val writers =
                (0 until WRITER_COUNT).map {
                    testScope.async {
                        repeat(UPDATES_PER_WRITER) { dataStore.updateData { it.inc() } }
                    }
                }

            // There's no reason this should take more than a few seconds once writers complete and
            // there's no reason writers won't complete.
            withTimeout(10.seconds) { (writers + readers).awaitAll() }
        }

    @Test
    fun testManyConcurrentReadsAndWrites_withIntermittentWriteFailures() =
        runBlocking<Unit> {
            val file = tempFolder.newFile()
            file.delete()

            val serializer = LongSerializer(failWrites = false, failReads = false)

            val dataStore =
                DataStoreFactory.create(serializer = serializer, scope = testScope) { file }

            val readers =
                (0 until READER_COUNT).map {
                    testScope.async {
                        try {
                            dataStore.data
                                .takeWhile { it < FINAL_TEST_VALUE }
                                .reduce { accumulator, value ->
                                    // we don't use `assertIncreasingAfterFirstRead` here because
                                    // failed writes
                                    // might increment the shared counter and trigger more reads
                                    // than necessary.
                                    // Hence, we only assert for ">=" here.
                                    assertThat(value).isAtLeast(accumulator)
                                    value
                                }
                        } catch (_: NoSuchElementException) {
                            // the reduce on dataStore.data could start after dataStore is in Final
                            // state
                            // thus no longer emitting elements.
                        }
                    }
                }
            val writers =
                (0 until WRITER_COUNT).map {
                    testScope.async {
                        repeat(UPDATES_PER_WRITER) {
                            var success = false
                            while (!success) {
                                try {
                                    dataStore.updateData { it.inc() }
                                    success = true
                                } catch (_: IOException) {}
                            }
                        }
                    }
                }

            serializer.failWrites = true

            repeat(10) {
                delay(10)
                serializer.failWrites = !serializer.failWrites
            }

            serializer.failWrites = false

            // There's no reason this should take more than a few seconds once writers complete and
            // there's no reason writers won't complete.
            withTimeout(10.seconds) { (writers + readers).awaitAll() }
        }

    @Test
    fun testManyConcurrentReadsAndWrites_withBeginningReadFailures() =
        runBlocking<Unit> {
            val file = tempFolder.newFile()
            file.delete()

            val serializer = LongSerializer(failWrites = false, failReads = true)

            val dataStore =
                DataStoreFactory.create(serializer = serializer, scope = testScope) { file }

            val readers =
                (0 until READER_COUNT).map {
                    testScope.async {
                        var retry = true
                        while (retry) {
                            // we retry because the test itself is creating read failures on
                            // purpose.
                            retry = false
                            try {
                                dataStore.data
                                    .takeWhile { it < FINAL_TEST_VALUE }
                                    .assertIncreasingAfterFirstRead()
                            } catch (_: IOException) {
                                // reader is configured to throw IO exceptions in the test, hence it
                                // is
                                // ok to get them here. It means we need to go back to reading until
                                // we
                                // reach to the final value.
                                retry = true
                            }
                        }
                    }
                }
            val writers =
                (0 until WRITER_COUNT).map {
                    testScope.async {
                        repeat(UPDATES_PER_WRITER) {
                            var success = false
                            while (!success) {
                                try {
                                    dataStore.updateData { it.inc() }
                                    success = true
                                } catch (_: IOException) {}
                            }
                        }
                    }
                }

            // Read failures for first 100 ms
            delay(100)
            serializer.failReads = false

            // There's no reason this should take more than a few seconds once writers complete and
            // there's no reason writers won't complete.
            withTimeout(10.seconds) { (writers + readers).awaitAll() }
        }

    private suspend fun Flow<Long>.assertIncreasingAfterFirstRead() {
        // at a very rare race condition, we might read the new value with old
        // version during initialization due to reads without locks. So here,
        // we assert that it is increasing except for the first 2 reads which can be the same value
        var prev: Long = -1
        this.collectIndexed { index, value ->
            if (index <= 1) {
                assertThat(value).isAtLeast(prev)
            } else {
                assertThat(value).isGreaterThan(prev)
            }
            prev = value
        }
    }

    private class LongSerializer(
        @Volatile var failWrites: Boolean,
        @Volatile var failReads: Boolean
    ) : Serializer<Long> {
        override val defaultValue = 0L

        override suspend fun readFrom(input: InputStream): Long {
            if (failReads) {
                throw IOException("failing read")
            }
            return DataInputStream(input).readLong()
        }

        override suspend fun writeTo(t: Long, output: OutputStream) {
            if (failWrites) {
                throw IOException("failing write")
            }
            DataOutputStream(output).writeLong(t)
        }
    }

    companion object {
        private const val READER_COUNT = 100
        private const val WRITER_COUNT = 25
        private const val FINAL_TEST_VALUE = 100
        private const val UPDATES_PER_WRITER = FINAL_TEST_VALUE / WRITER_COUNT

        init {
            check(UPDATES_PER_WRITER * WRITER_COUNT == FINAL_TEST_VALUE) {
                "inconsistent test setup"
            }
        }
    }
}
