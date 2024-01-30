/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.sqlite.db.framework

import android.content.Context
import android.os.StrictMode
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

@SmallTest
class FrameworkOpenHelperTest {
    private val dbName = "test.db"
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val openHelper = FrameworkSQLiteOpenHelper(
        context = context,
        name = dbName,
        callback = OpenHelperRecoveryTest.EmptyCallback(),
    )

    @Before
    fun setup() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun testFrameWorkSQLiteOpenHelper_cacheDatabase() {
        // Open DB, does I/O
        val firstRef = openHelper.writableDatabase
        assertThat(firstRef.isOpen).isTrue()

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .penaltyDeath()
                    .build()
            )

            // DB is already opened, should not do I/O
            val secondRef = openHelper.writableDatabase
            assertThat(secondRef.isOpen).isTrue()

            assertThat(firstRef).isEqualTo(secondRef)
        }
    }
}
