/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class UnsignedIntegersTest {

    @Entity
    data class UnsignedEntity(
        @PrimaryKey val id: Long,
        val uByte: UByte,
        val uShort: UShort,
        val uInt: UInt,
        val uLong: ULong,
        val nullableUByte: UByte?,
        val nullableUShort: UShort?,
        val nullableUInt: UInt?,
        val nullableULong: ULong?,
    )

    @Entity data class UnsignedPkEntity(@PrimaryKey val id: UInt, val value: String)

    @Dao
    interface UnsignedDao {
        @Insert fun insert(entity: UnsignedEntity)

        @Query("SELECT * FROM UnsignedEntity WHERE id = :id") fun getById(id: Long): UnsignedEntity?

        @Query("SELECT * FROM UnsignedEntity WHERE uByte = :value")
        fun getByUByte(value: UByte): List<UnsignedEntity>

        @Query("SELECT * FROM UnsignedEntity WHERE uShort = :value")
        fun getByUShort(value: UShort): List<UnsignedEntity>

        @Query("SELECT * FROM UnsignedEntity WHERE uInt = :value")
        fun getByUInt(value: UInt): List<UnsignedEntity>

        @Query("SELECT * FROM UnsignedEntity WHERE uLong = :value")
        fun getByULong(value: ULong): List<UnsignedEntity>

        @Query("SELECT * FROM UnsignedEntity WHERE uInt IN (:values)")
        fun getByUInts(values: List<UInt>): List<UnsignedEntity>

        @Insert fun insertPk(entity: UnsignedPkEntity)

        @Query("SELECT * FROM UnsignedPkEntity WHERE id = :id")
        fun getPkById(id: UInt): UnsignedPkEntity?
    }

    @Database(
        entities = [UnsignedEntity::class, UnsignedPkEntity::class],
        version = 1,
        exportSchema = false,
    )
    abstract class UnsignedDatabase : RoomDatabase() {
        abstract fun getDao(): UnsignedDao
    }

    private lateinit var db: UnsignedDatabase
    private lateinit var dao: UnsignedDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room.inMemoryDatabaseBuilder<UnsignedDatabase>(context)
                .setDriver(BundledSQLiteDriver())
                .build()
        dao = db.getDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRead() {
        val entity =
            UnsignedEntity(
                id = 1,
                uByte = 1u,
                uShort = 2u,
                uInt = 3u,
                uLong = 4u,
                nullableUByte = 5u,
                nullableUShort = 6u,
                nullableUInt = 7u,
                nullableULong = 8u,
            )
        dao.insert(entity)
        assertThat(dao.getById(1)).isEqualTo(entity)
    }

    @Test
    fun insertAndReadMaxValues() {
        val entity =
            UnsignedEntity(
                id = 1,
                uByte = UByte.MAX_VALUE,
                uShort = UShort.MAX_VALUE,
                uInt = UInt.MAX_VALUE,
                uLong = ULong.MAX_VALUE,
                nullableUByte = UByte.MAX_VALUE,
                nullableUShort = UShort.MAX_VALUE,
                nullableUInt = UInt.MAX_VALUE,
                nullableULong = ULong.MAX_VALUE,
            )
        dao.insert(entity)
        assertThat(dao.getById(1)).isEqualTo(entity)
    }

    @Test
    fun queryByUnsigned() {
        val entity1 = UnsignedEntity(1, 10u, 100u, 1000u, 10000u, null, null, null, null)
        val entity2 = UnsignedEntity(2, 20u, 200u, 2000u, 20000u, null, null, null, null)
        dao.insert(entity1)
        dao.insert(entity2)

        assertThat(dao.getByUByte(10u)).containsExactly(entity1)
        assertThat(dao.getByUShort(200u)).containsExactly(entity2)
        assertThat(dao.getByUInt(1000u)).containsExactly(entity1)
        assertThat(dao.getByULong(20000u)).containsExactly(entity2)
    }

    @Test
    fun queryByUnsignedCollection() {
        val entity1 = UnsignedEntity(1, 1u, 1u, 1u, 1u, null, null, null, null)
        val entity2 = UnsignedEntity(2, 2u, 2u, 2u, 2u, null, null, null, null)
        val entity3 = UnsignedEntity(3, 3u, 3u, 3u, 3u, null, null, null, null)
        dao.insert(entity1)
        dao.insert(entity2)
        dao.insert(entity3)

        assertThat(dao.getByUInts(listOf(1u, 3u))).containsExactly(entity1, entity3)
    }

    @Test
    fun unsignedPrimaryKey() {
        val entity = UnsignedPkEntity(id = UInt.MAX_VALUE, value = "foo")
        dao.insertPk(entity)
        assertThat(dao.getPkById(UInt.MAX_VALUE)).isEqualTo(entity)
    }
}
