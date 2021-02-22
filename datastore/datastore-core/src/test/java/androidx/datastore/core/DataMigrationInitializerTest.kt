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

import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
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
class DataMigrationInitializerTest {
    @get:Rule
    val timeout = Timeout(10, TimeUnit.SECONDS)

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var serializer: TestingSerializer
    private lateinit var testFile: File

    @Before
    fun setUp() {
        serializer = TestingSerializer()
        testFile = tmp.newFile()
    }

    @Test
    fun testMigration() = runBlockingTest {
        val migrateTo100 = TestingDataMigration(migration = { 100 })

        val store = newDataStore(
            initTasksList = listOf(
                DataMigrationInitializer.getInitializer(
                    listOf(migrateTo100)
                )
            )
        )

        assertThat(store.data.first()).isEqualTo(100)
    }

    @Test
    fun testMultipleDataMigrationsExecuted() = runBlockingTest {
        val migratePlus2 = TestingDataMigration(migration = { it.inc().inc() })
        val migratePlus3 = TestingDataMigration(migration = { it.inc().inc().inc() })

        val store = newDataStore(
            initTasksList = listOf(
                DataMigrationInitializer.getInitializer(
                    listOf(migratePlus2, migratePlus3)
                )
            )
        )

        assertThat(store.data.first()).isEqualTo(5)
    }

    @Test
    fun testCleanupRunAfterMigration() = runBlockingTest {
        val continueMigration = CompletableDeferred<Byte>()
        val cleanUpFinished = CompletableDeferred<Unit>()

        val noOpMigration = TestingDataMigration(
            migration = { continueMigration.await() },
            cleanUpFunction = { cleanUpFinished.complete(Unit) }
        )

        val store = newDataStore(
            initTasksList = listOf(
                DataMigrationInitializer.getInitializer(listOf(noOpMigration))
            )
        )

        val getData = async { store.data.first() }

        continueMigration.complete(5)
        cleanUpFinished.await()

        assertThat(getData.await()).isEqualTo(5)
    }

    @Test
    fun testCleanupNotRunAfterFailedMigrate() = runBlockingTest {
        val continueMigration = CompletableDeferred<Byte>()
        val cleanUpFinished = CompletableDeferred<Unit>()

        val noOpMigration = TestingDataMigration(
            migration = { continueMigration.await() },
            cleanUpFunction = { cleanUpFinished.complete(Unit) }
        )

        val store = newDataStore(
            initTasksList = listOf(
                DataMigrationInitializer.getInitializer(listOf(noOpMigration))
            )
        )

        val getData = async { store.data.first() }

        continueMigration.completeExceptionally(IOException("Failed migration"))

        assertThrows<IOException> { getData.await() }

        assertThat(cleanUpFinished.isCompleted).isFalse()
    }

    @Test
    fun testCleanupNotRunAfterFailedUpdate() = runBlockingTest {
        val continueMigration = CompletableDeferred<Byte>()
        val cleanUpFinished = CompletableDeferred<Unit>()

        val noOpMigration = TestingDataMigration(
            migration = { continueMigration.await() },
            cleanUpFunction = { cleanUpFinished.complete(Unit) }
        )

        serializer.failingWrite = true
        val store = newDataStore(
            initTasksList = listOf(
                DataMigrationInitializer.getInitializer(listOf(noOpMigration))
            ),
            serializer = serializer
        )

        val getData = async { store.data.first() }

        continueMigration.complete(1)

        assertThrows<IOException> { getData.await() }

        assertThat(cleanUpFinished.isCompleted).isFalse()
    }

    @Test
    fun testCleanUpErrorPropagates() = runBlockingTest {
        val cleanUpFailingMigration = TestingDataMigration(
            cleanUpFunction = {
                throw IOException("Clean up failure")
            }
        )

        val store = newDataStore(
            initTasksList = listOf(
                DataMigrationInitializer.getInitializer(listOf(cleanUpFailingMigration))
            )
        )

        assertThrows<IOException> { store.data.first() }
    }

    @Test
    fun testShouldMigrateUsed() = runBlockingTest {
        val neverRunMigration = TestingDataMigration(shouldMigrate = false, migration = { 99 })

        val store = newDataStore(
            initTasksList = listOf(
                DataMigrationInitializer.getInitializer(listOf(neverRunMigration))
            )
        )

        assertThat(store.data.first()).isEqualTo(0)
    }

    private fun newDataStore(
        initTasksList: List<suspend (api: InitializerApi<Byte>) -> Unit> = listOf(),
        serializer: TestingSerializer = TestingSerializer()
    ): DataStore<Byte> {
        return SingleProcessDataStore(
            { testFile },
            serializer = serializer,
            scope = TestCoroutineScope(),
            initTasksList = initTasksList
        )
    }

    class TestingDataMigration(
        private val shouldMigrate: Boolean = true,
        private val migration: suspend (byte: Byte) -> Byte = { 0 },
        private val cleanUpFunction: suspend () -> Unit = { }
    ) : DataMigration<Byte> {
        override suspend fun shouldMigrate(currentData: Byte): Boolean = shouldMigrate

        override suspend fun migrate(currentData: Byte): Byte = migration(currentData)

        override suspend fun cleanUp() = cleanUpFunction()
    }
}