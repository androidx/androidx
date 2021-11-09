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

package androidx.datastore.core.handlers

import androidx.datastore.core.SingleProcessDataStore
import androidx.datastore.core.TestingSerializer
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.rules.Timeout
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

@kotlinx.coroutines.ExperimentalCoroutinesApi
@kotlinx.coroutines.InternalCoroutinesApi
@kotlinx.coroutines.ObsoleteCoroutinesApi
@kotlinx.coroutines.FlowPreview
class ReplaceFileCorruptionHandlerTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @get:Rule
    val timeout = Timeout(10, TimeUnit.SECONDS)

    private lateinit var testFile: File

    @Before
    fun setUp() {
        testFile = tmp.newFile()
    }

    @Test
    fun testHandledRead() = runBlockingTest {
        preSeedData(testFile, 1)

        val store = SingleProcessDataStore<Byte>(
            { testFile },
            TestingSerializer(failReadWithCorruptionException = true),
            corruptionHandler = ReplaceFileCorruptionHandler<Byte> { 10 },
            scope = TestCoroutineScope()
        )

        assertThat(store.data.first()).isEqualTo(10)
    }

    @Test
    fun testHandledWrite() = runBlockingTest {
        preSeedData(testFile, 1)

        val store = SingleProcessDataStore<Byte>(
            { testFile },
            TestingSerializer(failReadWithCorruptionException = true),
            corruptionHandler = ReplaceFileCorruptionHandler<Byte> { 10 },
            scope = TestCoroutineScope()
        )

        assertThat(store.updateData { it.inc() }).isEqualTo(11)
    }

    @Test
    fun testHandlerCalledOnce() = runBlockingTest {
        preSeedData(testFile, 1)

        val store = SingleProcessDataStore<Byte>(
            { testFile },
            TestingSerializer(failReadWithCorruptionException = true),
            corruptionHandler = ReplaceFileCorruptionHandler<Byte> { 10 },
            scope = TestCoroutineScope()
        )

        val plus1 = async { store.updateData { it.inc() } }
        val minus2 = async { store.updateData { it.dec().dec() } }

        plus1.await()
        minus2.await()

        assertThat(store.data.first()).isEqualTo(9)
    }

    @Test
    fun testFailingWritePropagates() = runBlockingTest {

        preSeedData(testFile, 1)

        val store = SingleProcessDataStore<Byte>(
            { testFile },
            TestingSerializer(failReadWithCorruptionException = true, failingWrite = true),
            corruptionHandler = ReplaceFileCorruptionHandler<Byte> { 10 },
            scope = TestCoroutineScope()
        )

        assertThrows<IOException> { store.data.first() }

        // Confirm that the error is still thrown since data was never replaced:
        assertThrows<IOException> { store.data.first() }
    }

    private suspend fun preSeedData(file: File, byte: Byte) {
        coroutineScope {
            SingleProcessDataStore(
                { file },
                TestingSerializer(),
                scope = this
            ).updateData { byte }
        }
    }
}