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

import androidx.datastore.TestingSerializerConfig
import androidx.datastore.core.DataStoreImpl
import androidx.datastore.core.FileStorage
import androidx.datastore.core.TestingSerializer
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.rules.Timeout

@OptIn(ExperimentalCoroutinesApi::class)
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
    fun testHandledRead() = runTest {
        preSeedData(testFile, 1)

        val store = DataStoreImpl<Byte>(
            FileStorage(
                TestingSerializer(TestingSerializerConfig(failReadWithCorruptionException = true))
            ) { testFile },
            corruptionHandler = ReplaceFileCorruptionHandler<Byte> { 10 },
            scope = backgroundScope
        )

        assertThat(store.data.first()).isEqualTo(10)
    }

    @Test
    fun testHandledWrite() = runTest {
        preSeedData(testFile, 1)

        val store = DataStoreImpl<Byte>(
            FileStorage(
                TestingSerializer(
                    TestingSerializerConfig(
                        listOfFailReadWithCorruptionException = listOf(true, true)
                    )
                )
            ) { testFile },
            corruptionHandler = ReplaceFileCorruptionHandler<Byte> { 10 },
            scope = backgroundScope
        )

        assertThat(store.updateData { it.inc() }).isEqualTo(11)
    }

    @Test
    fun testHandlerCalledOnce() = runTest {
        preSeedData(testFile, 1)

        val store = DataStoreImpl<Byte>(
            FileStorage(
                TestingSerializer(
                    TestingSerializerConfig(
                        listOfFailReadWithCorruptionException = listOf(true, true)
                    )
                )
            ) { testFile },
            corruptionHandler = ReplaceFileCorruptionHandler<Byte> { 10 },
            scope = backgroundScope
        )

        val plus1 = async { store.updateData { it.inc() } }
        val minus2 = async { store.updateData { it.dec().dec() } }

        plus1.await()
        minus2.await()

        assertThat(store.data.first()).isEqualTo(9)
    }

    @Test
    fun testFailingWritePropagates() = runTest {

        preSeedData(testFile, 1)

        val store = DataStoreImpl<Byte>(
            FileStorage(
                TestingSerializer(
                    TestingSerializerConfig(
                        failReadWithCorruptionException = true,
                        failingWrite = true
                    )
                )
            ) { testFile },
            corruptionHandler = ReplaceFileCorruptionHandler<Byte> { 10 },
            scope = backgroundScope
        )

        assertThrows<IOException> { store.data.first() }

        // Confirm that the error is still thrown since data was never replaced:
        assertThrows<IOException> { store.data.first() }
    }

    private suspend fun preSeedData(file: File, byte: Byte) {
        runTest {
            DataStoreImpl(
                FileStorage(
                    TestingSerializer()
                ) { file },
                scope = backgroundScope
            ).updateData { byte }
        }
    }
}
