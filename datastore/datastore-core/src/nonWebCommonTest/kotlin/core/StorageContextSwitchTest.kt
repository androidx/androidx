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
package core

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class StorageContextSwitchTest {
    private val datastoreCtx = TestElement1("datastore_key_1") + TestElement2("datastore_key_2")
    private val callerCtx = TestElement1("caller_key_1") + TestElement3("caller_key_3")
    private val testStorage = TestStorage()
    private val store =
        _root_ide_package_.androidx.datastore.core.DataStoreImpl(
            testStorage,
            scope = CoroutineScope(Dispatchers.IO + datastoreCtx),
        )

    @Test
    fun testContextSandwich() =
        runBlocking(callerCtx) {
            // trigger a read to run read assertions in storage
            assertEquals("Initial Value", store.data.first().value)

            val unused =
                store.updateData {
                    assertEquals(
                        TestElement1("caller_key_1"),
                        _root_ide_package_.kotlin.coroutines.coroutineContext[TestKey1],
                    )
                    assertEquals(
                        TestElement3("caller_key_3"),
                        _root_ide_package_.kotlin.coroutines.coroutineContext[TestKey3],
                    )
                    TestData("updated")
                }
        }
}

private class TestData(var value: String)

private class TestStorageConnection : androidx.datastore.core.StorageConnection<TestData> {
    private var data: TestData = TestData("Initial Value")

    override suspend fun <R> readScope(
        block: suspend androidx.datastore.core.ReadScope<TestData>.(locked: Boolean) -> R
    ): R {
        return block(
            object :
                androidx.datastore.core.ReadScope<TestData>, androidx.datastore.core.Closeable {
                override suspend fun readData(): TestData {
                    // Context is caller + datastore so we assert that we have the keys from the
                    // datastoreCtx and any key in the callerCtx that was not present in
                    // the datastoreCtx.
                    // Ensure the caller's keys DO NOT OVERRIDE the datastore keys
                    assertEquals(coroutineContext[TestKey1], TestElement1("datastore_key_1"))
                    assertEquals(coroutineContext[TestKey2], TestElement2("datastore_key_2"))

                    // Ensure the additional keys in the caller are available.
                    assertEquals(coroutineContext[TestKey3], TestElement3("caller_key_3"))
                    return data
                }

                override fun close() {}
            },
            true,
        )
    }

    override suspend fun writeScope(
        block: suspend androidx.datastore.core.WriteScope<TestData>.() -> Unit
    ) {
        block(
            object :
                androidx.datastore.core.WriteScope<TestData>,
                androidx.datastore.core.ReadScope<TestData> {
                override suspend fun readData(): TestData = data

                override suspend fun writeData(value: TestData) {
                    // Context is caller + datastore so we assert that we have the keys from the
                    // datastoreCtx and any key in the callerCtx that was not present in
                    // the datastoreCtx.
                    // Ensure the caller's keys DO NOT OVERRIDE the datastore keys.
                    assertEquals(coroutineContext[TestKey1], TestElement1("datastore_key_1"))
                    assertEquals(coroutineContext[TestKey2], TestElement2("datastore_key_2"))

                    // Ensure the additional keys in the caller are available.
                    assertEquals(coroutineContext[TestKey3], TestElement3("caller_key_3"))
                    data = value
                }

                override fun close() {}
            }
        )
    }

    override val coordinator: androidx.datastore.core.InterProcessCoordinator =
        object : androidx.datastore.core.InterProcessCoordinator {
            override val updateNotifications: Flow<Unit>
                get() = TODO("Not yet implemented")

            override suspend fun <T> lock(block: suspend () -> T): T {
                return block.invoke()
            }

            override suspend fun <T> tryLock(block: suspend (Boolean) -> T): T {
                return block.invoke(true)
            }

            override suspend fun getVersion(): Int = 1

            override suspend fun incrementAndGetVersion(): Int = getVersion() + 1
        }

    override fun close() {}
}

private class TestStorage : androidx.datastore.core.Storage<TestData> {
    override fun createConnection(): androidx.datastore.core.StorageConnection<TestData> {
        return TestStorageConnection()
    }
}

private object TestKey1 : CoroutineContext.Key<TestElement1>

private object TestKey2 : CoroutineContext.Key<TestElement2>

private object TestKey3 : CoroutineContext.Key<TestElement3>

private data class TestElement1(val value: String) : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> = TestKey1
}

private data class TestElement2(val value: String) : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> = TestKey2
}

private data class TestElement3(val value: String) : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> = TestKey3
}
