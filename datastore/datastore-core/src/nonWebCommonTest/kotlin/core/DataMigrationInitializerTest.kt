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

package androidx.datastore.core

import androidx.datastore.TestFile
import androidx.datastore.TestIO
import androidx.datastore.TestingSerializerConfig
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
abstract class DataMigrationInitializerTest<F : TestFile<F>, IOE : Throwable>(
    private val testIO: TestIO<F, IOE>
) {

    private lateinit var storage: Storage<Byte>
    private lateinit var testScope: TestScope
    private lateinit var dataStoreScope: TestScope
    protected lateinit var testFile: F

    @BeforeTest
    fun setUp() {
        testScope = TestScope(UnconfinedTestDispatcher())
        dataStoreScope = TestScope(UnconfinedTestDispatcher())
        testFile = testIO.newTempFile()
    }

    fun doTest(test: suspend TestScope.() -> Unit) {
        testScope.runTest(timeout = 10000.milliseconds) { test(testScope) }
    }

    @Test
    fun testMigration() = doTest {
        val migrateTo100 = TestingDataMigration(migration = { 100 })

        val store =
            newDataStore(
                initTasksList =
                    listOf(DataMigrationInitializer.getInitializer(listOf(migrateTo100)))
            )

        assertThat(store.data.first()).isEqualTo(100)
    }

    @Test
    fun testMultipleDataMigrationsExecuted() = doTest {
        val migratePlus2 = TestingDataMigration(migration = { it.inc().inc() })
        val migratePlus3 = TestingDataMigration(migration = { it.inc().inc().inc() })

        val store =
            newDataStore(
                initTasksList =
                    listOf(
                        DataMigrationInitializer.getInitializer(listOf(migratePlus2, migratePlus3))
                    )
            )

        assertThat(store.data.first()).isEqualTo(5)
    }

    @Test
    fun testCleanupRunAfterMigration() = doTest {
        val continueMigration = CompletableDeferred<Byte>()
        val cleanUpFinished = CompletableDeferred<Unit>()

        val noOpMigration =
            TestingDataMigration(
                migration = { continueMigration.await() },
                cleanUpFunction = { cleanUpFinished.complete(Unit) },
            )

        val store =
            newDataStore(
                initTasksList =
                    listOf(DataMigrationInitializer.getInitializer(listOf(noOpMigration)))
            )

        val getData = async { store.data.first() }

        continueMigration.complete(5)
        cleanUpFinished.await()

        assertThat(getData.await()).isEqualTo(5)
    }

    @Test
    fun testCleanupNotRunAfterFailedMigrate() = doTest {
        val continueMigration = CompletableDeferred<Byte>()
        val cleanUpFinished = CompletableDeferred<Unit>()

        val noOpMigration =
            TestingDataMigration(
                migration = { continueMigration.await() },
                cleanUpFunction = { cleanUpFinished.complete(Unit) },
            )

        val store =
            newDataStore(
                initTasksList =
                    listOf(DataMigrationInitializer.getInitializer(listOf(noOpMigration)))
            )

        val getData = async { assertThrows(testIO.ioExceptionClass()) { store.data.first() } }

        continueMigration.completeExceptionally(testIO.ioException("Failed migration"))

        getData.await()

        assertThat(cleanUpFinished.isCompleted).isFalse()
    }

    @Test
    fun testCleanupNotRunAfterFailedUpdate() = doTest {
        val continueMigration = CompletableDeferred<Byte>()
        val cleanUpFinished = CompletableDeferred<Unit>()

        val noOpMigration =
            TestingDataMigration(
                migration = { continueMigration.await() },
                cleanUpFunction = { cleanUpFinished.complete(Unit) },
            )

        val testFile = testIO.newTempFile()

        val storage =
            testIO.getStorage(
                TestingSerializerConfig(failingWrite = true),
                { createSingleProcessCoordinator(testFile.path()) },
            ) {
                testFile
            }
        val store =
            newDataStore(
                initTasksList =
                    listOf(DataMigrationInitializer.getInitializer(listOf(noOpMigration))),
                storage = storage,
            )

        val getData = async { assertThrows(testIO.ioExceptionClass()) { store.data.first() } }

        continueMigration.complete(1)

        getData.await()

        assertThat(cleanUpFinished.isCompleted).isFalse()
    }

    @Test
    fun testCleanUpErrorPropagates() = doTest {
        val cleanUpFailingMigration =
            TestingDataMigration(cleanUpFunction = { throw IOException("Clean up failure") })

        val store =
            newDataStore(
                initTasksList =
                    listOf(DataMigrationInitializer.getInitializer(listOf(cleanUpFailingMigration)))
            )

        assertThrows<IOException> { store.data.first() }
    }

    @Test
    fun testShouldMigrateUsed() = doTest {
        val neverRunMigration = TestingDataMigration(shouldMigrate = false, migration = { 99 })

        val store =
            newDataStore(
                initTasksList =
                    listOf(DataMigrationInitializer.getInitializer(listOf(neverRunMigration)))
            )

        assertThat(store.data.first()).isEqualTo(0)
    }

    private fun newDataStore(
        initTasksList: List<suspend (api: InitializerApi<Byte>) -> Unit> = listOf(),
        storage: Storage<Byte> =
            testIO.getStorage(
                TestingSerializerConfig(),
                { createSingleProcessCoordinator(testFile.path()) },
                { testFile },
            ),
    ): DataStore<Byte> {
        return DataStoreImpl(storage, scope = dataStoreScope, initTasksList = initTasksList)
    }

    class TestingDataMigration(
        private val shouldMigrate: Boolean = true,
        private val migration: suspend (byte: Byte) -> Byte = { 0 },
        private val cleanUpFunction: suspend () -> Unit = {},
    ) : DataMigration<Byte> {
        override suspend fun shouldMigrate(currentData: Byte): Boolean = shouldMigrate

        override suspend fun migrate(currentData: Byte): Byte = migration(currentData)

        override suspend fun cleanUp() = cleanUpFunction()
    }
}
