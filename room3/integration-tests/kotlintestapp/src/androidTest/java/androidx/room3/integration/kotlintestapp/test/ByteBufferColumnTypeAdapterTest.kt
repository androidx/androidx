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

import android.content.Context
import androidx.kruth.assertThat
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import java.nio.ByteBuffer
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ByteBufferColumnTypeAdapterTest {

    @Test
    fun testByteBuffer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db =
            Room.inMemoryDatabaseBuilder<ByteBufferColumnTypeAdapterDatabase>(context)
                .setDriver(AndroidSQLiteDriver())
                .build()

        db.dao().insert(ByteBufferEntity("Key1", ByteBuffer.wrap(byteArrayOf(1, 2, 3))))
        assertThat(db.dao().getItem("Key1")!!.buffer!!.array()).isEqualTo(byteArrayOf(1, 2, 3))
        db.close()
    }

    @Test
    fun testNullByteBuffer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db =
            Room.inMemoryDatabaseBuilder<ByteBufferColumnTypeAdapterDatabase>(context)
                .setDriver(AndroidSQLiteDriver())
                .build()

        db.dao().insert(ByteBufferEntity("Key1", null))
        assertThat(db.dao().getItem("Key1")!!.buffer).isEqualTo(null)
        db.close()
    }

    @Entity class ByteBufferEntity(@PrimaryKey val id: String, val buffer: ByteBuffer?)

    @Dao
    interface ByteBufferFooDao {
        @Query("select * from ByteBufferEntity where id = :id")
        fun getItem(id: String?): ByteBufferEntity?

        @Insert fun insert(item: ByteBufferEntity)
    }

    @Database(version = 1, entities = [ByteBufferEntity::class], exportSchema = false)
    abstract class ByteBufferColumnTypeAdapterDatabase : RoomDatabase() {
        abstract fun dao(): ByteBufferFooDao
    }
}
