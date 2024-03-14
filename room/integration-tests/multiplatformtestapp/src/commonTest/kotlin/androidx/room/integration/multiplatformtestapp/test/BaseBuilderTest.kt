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

package androidx.room.integration.multiplatformtestapp.test

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

abstract class BaseBuilderTest {
    abstract fun getRoomDatabaseBuilder(): RoomDatabase.Builder<SampleDatabase>

    @Test
    fun createOpenCallback() = runTest {
        var onCreateInvoked = 0
        var onOpenInvoked = 0
        val testCallback = object : RoomDatabase.Callback() {
            override fun onCreate(connection: SQLiteConnection) {
                onCreateInvoked++
            }

            override fun onOpen(connection: SQLiteConnection) {
                onOpenInvoked++
            }
        }

        val builder = getRoomDatabaseBuilder()
            .addCallback(testCallback)

        val db1 = builder.build()

        // No callback invoked, Room opens the database lazily
        assertThat(onCreateInvoked).isEqualTo(0)
        assertThat(onOpenInvoked).isEqualTo(0)

        db1.dao().insertItem(1)

        // Database is created and opened
        assertThat(onCreateInvoked).isEqualTo(1)
        assertThat(onOpenInvoked).isEqualTo(1)

        db1.close()

        val db2 = builder.build()

        db2.dao().insertItem(2)

        // Database was already created, it is now only opened
        assertThat(onCreateInvoked).isEqualTo(1)
        assertThat(onOpenInvoked).isEqualTo(2)

        db2.close()
    }

    @Test
    fun setCoroutineContextWithoutDispatcher() {
        assertThrows<IllegalArgumentException> {
            getRoomDatabaseBuilder().setQueryCoroutineContext(EmptyCoroutineContext)
        }.hasMessageThat()
            .contains("It is required that the coroutine context contain a dispatcher.")
    }
}
