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

package androidx.room3.integration.kotlintestapp.test

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.kruth.assertThat
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.ExperimentalRoomApi
import androidx.room3.Fts4
import androidx.room3.Insert
import androidx.room3.MultiInstanceInvalidationService
import androidx.room3.PrimaryKey
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.integration.kotlintestapp.IRemoteDatabaseService
import androidx.room3.integration.kotlintestapp.RemoteDatabaseService
import androidx.room3.integration.kotlintestapp.database.RemoteEntity
import androidx.room3.integration.kotlintestapp.database.RemoteSampleDatabase
import androidx.room3.withWriteTransaction
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.rule.ServiceTestRule
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.AssumptionViolatedException
import org.junit.Before
import org.junit.Rule

@OptIn(ExperimentalRoomApi::class)
class MultiInstanceInvalidationTest {
    @Entity data class SampleEntity(@PrimaryKey val pk: Int)

    @Entity data class AnotherSampleEntity(@PrimaryKey val pk: Int)

    @Entity @Fts4 data class FtsSampleEntity(val data: String)

    @Dao
    interface SampleDao {
        @Insert suspend fun insert(entity: SampleEntity)

        @Insert suspend fun insertFts(entity: FtsSampleEntity)
    }

    @Database(
        entities = [SampleEntity::class, AnotherSampleEntity::class, FtsSampleEntity::class],
        version = 1,
        exportSchema = false,
    )
    abstract class SampleDatabase : RoomDatabase() {
        abstract fun dao(): SampleDao
    }

    @get:Rule val serviceRule = ServiceTestRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        context.deleteDatabase("test.db")
    }

    @Test
    fun invalidateInAnotherInstanceFlow() = runTest {
        val databaseOne =
            Room.databaseBuilder<SampleDatabase>(context, "test.db")
                .setDriver(AndroidSQLiteDriver())
                .enableMultiInstanceInvalidation()
                .build()
        val databaseTwo =
            Room.databaseBuilder<SampleDatabase>(context, "test.db")
                .setDriver(AndroidSQLiteDriver())
                .enableMultiInstanceInvalidation()
                .build()

        val channel =
            databaseOne.invalidationTracker
                .createFlow("SampleEntity", "AnotherSampleEntity")
                .produceIn(this)

        // Initial invalidation, all tables
        assertThat(channel.receive()).containsExactly("SampleEntity", "AnotherSampleEntity")

        // Assert multi-instance invalidation service is running.
        awaitService(isRunning = true)
        // Insert in second instance
        databaseTwo.dao().insert(SampleEntity(1))

        // Invalidation by second instance
        assertThat(channel.receive()).containsExactly("SampleEntity")

        channel.cancel()
        databaseOne.close()
        databaseTwo.close()

        // Assert multi-instance invalidation service is not running.
        awaitService(isRunning = false)
    }

    @Test
    fun invalidateInAnotherInstanceFtsFlow() = runTest {
        val databaseOne =
            Room.databaseBuilder<SampleDatabase>(context, "test.db")
                .setDriver(AndroidSQLiteDriver())
                .enableMultiInstanceInvalidation()
                .build()
        val databaseTwo =
            Room.databaseBuilder<SampleDatabase>(context, "test.db")
                .setDriver(AndroidSQLiteDriver())
                .enableMultiInstanceInvalidation()
                .build()

        val channel = databaseOne.invalidationTracker.createFlow("FtsSampleEntity").produceIn(this)

        // Initial invalidation
        assertThat(channel.receive()).containsExactly("FtsSampleEntity")

        // Assert multi-instance invalidation service is running.
        awaitService(isRunning = true)
        // Insert in second instance
        databaseTwo.dao().insertFts(FtsSampleEntity("Tom"))

        // Invalidation by second instance
        assertThat(channel.receive()).containsExactly("FtsSampleEntity")

        channel.cancel()
        databaseOne.close()
        databaseTwo.close()

        // Assert multi-instance invalidation service is not running.
        awaitService(isRunning = false)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    fun invalidateFromAnotherProcess() = runTest {
        val database =
            Room.databaseBuilder<RemoteSampleDatabase>(context, "test.db")
                .setDriver(AndroidSQLiteDriver())
                .enableMultiInstanceInvalidation()
                .build()
        val service =
            IRemoteDatabaseService.Stub.asInterface(
                serviceRule.bindService(
                    RemoteDatabaseService.intentFor(context = context, databaseName = "test.db")
                )
            )

        val channel = database.dao().getEntityFlow(5).produceIn(this)

        // Initial invalidation
        assertThat(channel.receive()).isNull()

        // Assert multi-instance invalidation service is running.
        awaitService(isRunning = true)
        // Assert second process is indeed another process
        assertThat(Process.myPid()).isNotEqualTo(service.pid)
        // Insert in second process
        service.insertEntity(5)

        // Invalidation by second process
        assertThat(channel.receive()).isEqualTo(RemoteEntity(5))

        channel.cancel()
        database.close()
        serviceRule.unbindService()

        // Assert multi-instance invalidation service is not running.
        awaitService(isRunning = false)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    fun autoCloseDatabaseStopsService() = runTest {
        val autoCloseDb =
            Room.databaseBuilder<SampleDatabase>(context, "test.db")
                .setDriver(AndroidSQLiteDriver())
                .enableMultiInstanceInvalidation()
                .setAutoCloseTimeout(200, TimeUnit.MILLISECONDS)
                .build()

        // Force open the database causing the multi-instance invalidation service to start
        autoCloseDb.withWriteTransaction {
            // Assert multi-instance invalidation service is running.
            awaitService(isRunning = true)
        }

        // Await and assert multi-instance invalidation service is no longer running. As this
        // function awaits, the database will be auto-closed and the service should be stopped.
        awaitService(isRunning = false)

        autoCloseDb.close()

        // Assert multi-instance invalidation service is still not running.
        awaitService(isRunning = false)
    }

    @Suppress("DEPRECATION") // For getRunningServices()
    private suspend fun awaitService(isRunning: Boolean) {
        val manager = context.getSystemService(ActivityManager::class.java)
        val serviceComponentName =
            ComponentName(context, MultiInstanceInvalidationService::class.java)
        withContext(Dispatchers.Main) {
            withTimeoutOrNull(10.seconds) {
                while (true) {
                    val hasRunningService =
                        manager.getRunningServices(100).any { it.service == serviceComponentName }
                    if (hasRunningService == isRunning) {
                        return@withTimeoutOrNull
                    }
                    delay(200.milliseconds)
                }
            }
                ?: throw AssumptionViolatedException(
                    "Could not validate multi-instance service state. " +
                        "Expected for isRunning to be '$isRunning' but it was the opposite."
                )
        }
    }
}
