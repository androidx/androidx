/*
 * Copyright (C) 2017 The Android Open Source Project
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

import androidx.kruth.assertThat
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.test.assertNotNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PrimaryKeyTest {

    @Entity
    data class LongAutoIncPKeyEntity(
        @PrimaryKey(autoGenerate = true, algorithm = PrimaryKey.Algorithm.AUTOINCREMENT)
        val pKey: Long,
        val data: String,
    )

    @Entity
    data class LongRowIdPKeyEntity(
        @PrimaryKey(autoGenerate = true, algorithm = PrimaryKey.Algorithm.ROWID) val pKey: Long,
        val data: String,
    )

    @Entity
    data class IntAutoIncPKeyEntity(
        @PrimaryKey(autoGenerate = true) val pKey: Int,
        val data: String,
    )

    @Entity data class StringPKeyEntity(@PrimaryKey val pKey: String, val data: String)

    @Dao
    interface LongAutoIncPKeyDao {
        @Insert fun insert(vararg items: LongAutoIncPKeyEntity)

        @Insert fun insertAndGetId(item: LongAutoIncPKeyEntity): Long

        @Query("SELECT * FROM LongAutoIncPKeyEntity WHERE pKey = :key")
        fun find(key: Long): LongAutoIncPKeyEntity?
    }

    @Dao
    interface LongRowIdPKeyDao {
        @Insert fun insert(item: LongRowIdPKeyEntity)

        @Insert fun insertAndGetId(item: LongRowIdPKeyEntity): Long

        @Query("SELECT * FROM LongRowIdPKeyEntity WHERE pKey = :key")
        fun find(key: Long): LongRowIdPKeyEntity?
    }

    @Dao
    interface IntPKeyDao {
        @Insert fun insert(vararg items: IntAutoIncPKeyEntity)

        @Insert fun insertAndGetId(item: IntAutoIncPKeyEntity): Long

        @Insert fun insertAndGetIds(vararg item: IntAutoIncPKeyEntity): List<Long>

        @Query("SELECT * FROM IntAutoIncPKeyEntity WHERE pKey = :key")
        fun find(key: Long): IntAutoIncPKeyEntity?

        @Query("SELECT data FROM IntAutoIncPKeyEntity WHERE pKey IN (:ids)")
        fun findByIds(ids: List<Long>): List<String>
    }

    @Dao
    interface StringPKeyDao {
        @Insert fun insert(item: StringPKeyEntity)

        @Insert fun insertAndGetId(item: StringPKeyEntity): Long

        @Query("SELECT * FROM StringPKeyEntity WHERE pKey = :key")
        fun find(key: String): StringPKeyEntity?
    }

    @Database(
        entities =
            [
                LongAutoIncPKeyEntity::class,
                LongRowIdPKeyEntity::class,
                IntAutoIncPKeyEntity::class,
                StringPKeyEntity::class,
            ],
        version = 1,
        exportSchema = false,
    )
    abstract class PKeyTestDatabase : RoomDatabase() {
        abstract fun longAutoIncDao(): LongAutoIncPKeyDao

        abstract fun longRowIdDao(): LongRowIdPKeyDao

        abstract fun intDao(): IntPKeyDao

        abstract fun stringDao(): StringPKeyDao
    }

    private lateinit var db: PKeyTestDatabase

    @Before
    fun setup() {
        db =
            Room.inMemoryDatabaseBuilder<PKeyTestDatabase>()
                .setDriver(BundledSQLiteDriver())
                .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun longKey() {
        db.longAutoIncDao().insert(LongAutoIncPKeyEntity(2L, "foo"))
        val result = db.longAutoIncDao().find(2L)
        assertNotNull(result)
        assertThat(result.data).isEqualTo("foo")
    }

    @Test
    fun longKeyAndGetKey() {
        val key = db.longAutoIncDao().insertAndGetId(LongAutoIncPKeyEntity(2L, "foo"))
        val result = db.longAutoIncDao().find(key)
        assertNotNull(result)
        assertThat(result.pKey).isEqualTo(2L)
        assertThat(result.data).isEqualTo("foo")
    }

    @Test
    fun autoincrementLong() {
        db.longAutoIncDao().insert(LongAutoIncPKeyEntity(0L, "foo"))
        val result = db.longAutoIncDao().find(1L)
        assertNotNull(result)
        assertThat(result.data).isEqualTo("foo")
    }

    @Test
    fun autoincrementInt() {
        db.intDao().insert(IntAutoIncPKeyEntity(0, "foo"))
        val result = db.intDao().find(1)
        assertNotNull(result)
        assertThat(result.data).isEqualTo("foo")
    }

    @Test
    fun autoincrementIntAndGetKey() {
        val key = db.intDao().insertAndGetId(IntAutoIncPKeyEntity(0, "foo"))
        val result = db.intDao().find(key)
        assertNotNull(result)
        assertThat(result.data).isEqualTo("foo")
    }

    @Test
    fun autoincrementIntAndGetKeys() {
        val keys =
            db.intDao()
                .insertAndGetIds(
                    IntAutoIncPKeyEntity(0, "foo1"),
                    IntAutoIncPKeyEntity(0, "foo2"),
                    IntAutoIncPKeyEntity(0, "foo3"),
                )
        val result = db.intDao().findByIds(keys)
        assertThat(result).containsExactly("foo1", "foo2", "foo3")
    }

    @Test
    fun rowIdLong() {
        val key = db.longRowIdDao().insertAndGetId(LongRowIdPKeyEntity(0, "foo"))
        val result = db.longRowIdDao().find(key)
        assertNotNull(result)
        assertThat(result.data).isEqualTo("foo")
    }

    @Test
    fun stringKey() {
        db.stringDao().insert(StringPKeyEntity("k1", "foo"))
        val result = db.stringDao().find("k1")
        assertNotNull(result)
        assertThat(result.data).isEqualTo("foo")
    }
}
