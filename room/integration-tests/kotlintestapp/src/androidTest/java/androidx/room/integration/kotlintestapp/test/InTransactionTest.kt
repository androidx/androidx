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

package androidx.room.integration.kotlintestapp.test

import androidx.kruth.assertThat
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.integration.kotlintestapp.TestDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import org.junit.Test

@SmallTest
class InTransactionTest {

    @Test
    fun inTransaction_alreadyClosed() {
        var onOpenCalled = 0
        val database =
            Room.inMemoryDatabaseBuilder<TestDatabase>(ApplicationProvider.getApplicationContext())
                .addCallback(
                    object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            onOpenCalled++
                        }
                    }
                )
                .build()
        assertThat(onOpenCalled).isEqualTo(0)
        database.close()
        assertThat(database.inTransaction()).isFalse()
        assertThat(onOpenCalled).isEqualTo(0)
    }
}
