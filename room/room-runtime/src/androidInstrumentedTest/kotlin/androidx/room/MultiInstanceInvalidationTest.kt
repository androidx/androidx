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
import androidx.kruth.assertThat
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import org.junit.Before

class MultiInstanceInvalidationTest {
    @Entity
    data class SampleEntity(
        @PrimaryKey
        val pk: Int
    )

    @Database(
        entities = [SampleEntity::class],
        version = 1,
        exportSchema = false
    )
    abstract class SampleDatabase : RoomDatabase()

    private lateinit var autoCloseDb: SampleDatabase

    @Suppress("DEPRECATION") // For `getRunningServices()`
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    fun invalidateInAnotherInstanceAutoCloser() {
        val latch = CountDownLatch(1)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(
            ActivityManager::class.java
        )
        val autoCloseHelper = autoCloseDb.openHelper as AutoClosingRoomOpenHelper
        val autoCloser = autoCloseHelper.autoCloser
        autoCloseHelper.writableDatabase
        // Make sure the service is running.
        assertThat(manager.getRunningServices(100)).isNotEmpty()

        // Let Room call setAutoCloseCallback
        val trackerCallback = autoCloser.onAutoCloseCallback
        autoCloser.setAutoCloseCallback {
            trackerCallback?.run()
            // At this point in time InvalidationTracker's callback has run and unbind should have
            // been invoked.
            latch.countDown()
        }
        latch.await()

        // Make sure the service is no longer running.
        assertThat(manager.getRunningServices(100)).isEmpty()
        autoCloseDb.close()
    }

    @OptIn(ExperimentalRoomApi::class)
    @Before
    fun initDb() {
        val context: Context = ApplicationProvider.getApplicationContext()
        autoCloseDb = Room.databaseBuilder(
            context,
            SampleDatabase::class.java,
            "MyDb"
        )
            .enableMultiInstanceInvalidation()
            .setAutoCloseTimeout(200, TimeUnit.MILLISECONDS)
            .build()
    }
}
