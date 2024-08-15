/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.room

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.arch.core.executor.testing.CountingTaskExecutorRule
import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.room.support.AutoClosingRoomOpenHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Ignore
import kotlin.test.Test
import org.junit.Rule

class MultiInstanceInvalidationTest {
    @Entity data class SampleEntity(@PrimaryKey val pk: Int)

    @Database(entities = [SampleEntity::class], version = 1, exportSchema = false)
    abstract class SampleDatabase : RoomDatabase()

    @get:Rule val countingTaskExecutorRule = CountingTaskExecutorRule()

    @Test
    @OptIn(ExperimentalRoomApi::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Ignore // Flaky Test b/359161892
    @Suppress("DEPRECATION") // For getRunningServices()
    fun invalidateInAnotherInstanceAutoCloser() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val autoCloseDb =
            Room.databaseBuilder(context, SampleDatabase::class.java, "MyDb")
                .enableMultiInstanceInvalidation()
                .setAutoCloseTimeout(200, TimeUnit.MILLISECONDS)
                .build()
        val manager = context.getSystemService(ActivityManager::class.java)
        val autoCloseHelper = autoCloseDb.openHelper as AutoClosingRoomOpenHelper

        // Force open the database causing the multi-instance invalidation service  to start
        autoCloseHelper.writableDatabase

        // Assert multi-instance invalidation service is running.
        assertThat(manager.getRunningServices(100)).isNotEmpty()

        // Remember the current auto close callback, it should be the one installed by the
        // invalidation tracker
        val trackerCallback = autoCloseHelper.autoCloser.autoCloseCallbackForTest!!

        // Set a new callback, intercepting when DB is auto-closed
        val latch = CountDownLatch(1)
        autoCloseHelper.autoCloser.setAutoCloseCallback {
            // Run the remember auto close callback
            trackerCallback.invoke()
            // At this point in time InvalidationTracker's callback has run and unbind should have
            // been invoked.
            latch.countDown()
        }

        assertWithMessage("Auto close callback latch await")
            .that(latch.await(2, TimeUnit.SECONDS))
            .isTrue()

        countingTaskExecutorRule.drainTasks(2, TimeUnit.SECONDS)
        assertWithMessage("Executor isIdle").that(countingTaskExecutorRule.isIdle).isTrue()

        // Assert multi-instance invalidation service is no longer running.
        assertThat(manager.getRunningServices(100)).isEmpty()

        autoCloseDb.close()
    }
}
