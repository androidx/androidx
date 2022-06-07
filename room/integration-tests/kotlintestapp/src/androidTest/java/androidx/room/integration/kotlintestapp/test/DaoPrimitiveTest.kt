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

package androidx.room.integration.kotlintestapp.test

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class DaoPrimitiveTest {

    @Entity(tableName = "longFoo")
    data class LongFoo(@PrimaryKey val id: Long, val description: String)

    @Entity(tableName = "stringFoo")
    data class StringFoo(@PrimaryKey val id: String, val description: String)

    @Entity(tableName = "byteArrayFoo")
    data class ByteArrayFoo(@PrimaryKey val id: ByteArray, val description: String)

    /* Interface with generics only */
    interface BaseDao<Key, Value> {

        fun getItem(id: Key): Value?

        fun delete(id: Key)

        fun getFirstItemId(): Key
    }

    /* Interface with non-generics and generics */
    interface LongBaseDao<Long, Value> {

        fun getItem(id: Long): Value?

        fun delete(id: Long)

        @Query("select id from longFoo limit 1")
        fun getFirstItemId(): Long
    }

    /* Interface with non-generics and generics */
    interface StringBaseDao<String, Value> {

        fun getItem(id: String): Value?

        fun delete(id: String)

        @Query("select id from stringFoo limit 1")
        fun getFirstItemId(): String
    }

    interface ByteArrayBaseDao<ByteArray, Value> {
        @Query("select id from byteArrayFoo limit 1")
        fun getByteArray(): Array<ByteArray>
    }

    // Foo interfaces that are using the base

    @Dao
    interface LongFooDao : BaseDao<Long, LongFoo> {

        @Query("select * from longFoo where id=:id")
        override fun getItem(id: Long): LongFoo?

        @Query("delete from longFoo where id=:id")
        override fun delete(id: Long)

        @Insert
        fun insert(item: LongFoo)

        @Query("select id from longFoo limit 1")
        override fun getFirstItemId(): Long
    }

    @Dao
    interface StringFooDao : BaseDao<String, StringFoo> {

        @Query("select * from stringFoo where id=:id")
        override fun getItem(id: String): StringFoo?

        @Query("delete from stringFoo where id=:id")
        override fun delete(id: String)

        @Insert
        fun insert(item: StringFoo)

        @Query("select id from stringFoo limit 1")
        override fun getFirstItemId(): String
    }

    @Dao
    interface ByteArrayFooDao : ByteArrayBaseDao<ByteArray, String> {

        @Insert
        fun insert(item: ByteArrayFoo)

        @Query("select id from byteArrayFoo limit 1")
        override fun getByteArray(): Array<ByteArray>
    }

    @Database(
        version = 1,
        entities = [
            LongFoo::class,
            StringFoo::class,
            ByteArrayFoo::class
        ],
        exportSchema = false
    )
    abstract class TestDatabase : RoomDatabase() {
        abstract fun longFooDao(): LongFooDao
        abstract fun stringFooDao(): StringFooDao
        abstract fun byteArrayFooDao(): ByteArrayFooDao
    }

    @Test
    fun testLongFooDao() {
        val db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation()
                .getTargetContext(),
            TestDatabase::class.java
        ).build()

        val foo = LongFoo(1, "Elif")
        db.longFooDao().insert(foo)
        assertThat(db.longFooDao().getFirstItemId()).isEqualTo(1)
        assertThat(db.longFooDao().getItem(1)).isEqualTo(foo)
        db.longFooDao().delete(1)
        assertThat(db.longFooDao().getItem(1)).isNull()
    }

    @Test
    fun testStringFooDao() {
        val db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation()
                .getTargetContext(),
            TestDatabase::class.java
        ).build()

        val foo = StringFoo("Key", "Elif")
        db.stringFooDao().insert(foo)
        assertThat(db.stringFooDao().getFirstItemId()).isEqualTo("Key")
        assertThat(db.stringFooDao().getItem("Key")).isEqualTo(foo)
        db.stringFooDao().delete("Key")
        assertThat(db.stringFooDao().getItem("Key")).isNull()
    }

    @Test
    fun testByteArrayFooDao() {
        val db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation()
                .getTargetContext(),
            TestDatabase::class.java
        ).build()
        val foo = ByteArrayFoo(ByteArray(16), "Elif")
        db.byteArrayFooDao().insert(foo)
        assertThat(db.byteArrayFooDao().getByteArray()).isEqualTo(arrayOf(ByteArray(16)))
    }
}