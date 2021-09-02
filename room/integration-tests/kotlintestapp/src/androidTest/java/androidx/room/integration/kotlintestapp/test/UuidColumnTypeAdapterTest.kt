/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.reflect.KClass

@SmallTest
@RunWith(AndroidJUnit4::class)
class UuidColumnTypeAdapterTest {

    @Test
    fun byte_noSuppliedConverter_insertWithQuery() = runTest(NoConverterDatabase::class) { dao, _ ->
        // database has no converters supplied
        val uuid = UUID.randomUUID()

        dao.insertWithQuery(uuid)

        val retrieved = dao.getEntity(uuid).id
        assertThat(retrieved).isEqualTo(uuid)
    }

    @Test
    fun byte_noSuppliedConverter_insertEntity() = runTest(NoConverterDatabase::class) { dao, _ ->
        // database has no converters supplied
        val uuid = UUID.randomUUID()

        dao.insert(UUIDEntity(uuid))

        val retrieved = dao.getEntity(uuid).id
        assertThat(retrieved).isEqualTo(uuid)
    }

    @Test
    fun byte_suppliedFromByteConverter_insertWithQuery() =
        runTest(ToUUIDConverterDatabase::class) { dao, _ ->
            // database has a byte[] to UUID converter supplied
            val uuid = UUID.randomUUID()

            dao.insertWithQuery(uuid)

            // the supplied converter retrieves the bytes but returns a UUID built from
            // (uuid.mostSignificantBits, Long.MIN_VALUE)
            val retrieved = dao.getEntity(uuid).id

            // ensure it used the one way converter
            val expected = UUID(uuid.mostSignificantBits, Long.MIN_VALUE)
            assertThat(retrieved).isEqualTo(expected)
        }

    @Test
    fun byte_suppliedFromByteConverter_insertEntity() =
        runTest(ToUUIDConverterDatabase::class) { dao, _ ->
            // database has a byte[] to UUID converter supplied
            val uuid = UUID.randomUUID()

            dao.insert(UUIDEntity(uuid))

            // the supplied converter retrieves the bytes but returns a UUID built from
            // (uuid.mostSignificantBits, Long.MIN_VALUE)
            val retrieved = dao.getEntity(uuid).id

            // ensure it used the one way converter
            val expected = UUID(uuid.mostSignificantBits, Long.MIN_VALUE)
            assertThat(retrieved).isEqualTo(expected)
        }

    @Test
    fun byte_suppliedTwoWayConverter_insertWithQuery() =
        runTest(TwoWayConverterDatabase::class) { dao, _ ->
            // database has UUID <--> byte[] two way converter supplied
            val uuid = UUID.randomUUID()

            dao.insertWithQuery(uuid)

            // two way converter builds a byte array with
            // (uuid.leastSignificantBits, uuid.leastSignificantBits)
            // then converts it back into UUID with
            // (uuid.leastSignificantBits, Long.MAX_VALUE)
            val retrieved = dao.getEntity(uuid).id

            // check if the adapter used the two way converter
            val expected = UUID(uuid.leastSignificantBits, Long.MAX_VALUE)
            assertThat(retrieved).isEqualTo(expected)
        }

    @Test
    fun byte_suppliedTwoWayConverter_insertEntity() =
        runTest(TwoWayConverterDatabase::class) { dao, _ ->
            // database has UUID <--> byte[] two way converter supplied
            val uuid = UUID.randomUUID()

            dao.insert(UUIDEntity(uuid))

            // two way converter builds a byte array with
            // (uuid.leastSignificantBits, uuid.leastSignificantBits)
            // then converts it back into UUID with
            // (uuid.leastSignificantBits, Long.MAX_VALUE)
            val retrieved = dao.getEntity(uuid).id

            // check if the adapter used the two way converter
            val expected = UUID(uuid.leastSignificantBits, Long.MAX_VALUE)
            assertThat(retrieved).isEqualTo(expected)
        }

    @Test
    fun string_noSuppliedConverter_insertWithQuery() =
        runTest(NoConverterDatabase::class) { _, dao ->
            // database has no converters supplied
            val uuid = UUID.randomUUID()

            dao.insertWithQuery(uuid)

            // binds and reads as BLOB
            val retrieved = dao.getEntity(uuid).id
            assertThat(retrieved).isEqualTo(uuid)
        }

    @Test
    fun string_noSuppliedConverter_insertEntity() =
        runTest(NoConverterDatabase::class) { _, dao ->
            // database has no converters supplied
            val uuid = UUID.randomUUID()

            dao.insert(UUIDStringEntity(uuid))

            // binds and reads as BLOB
            val retrieved = dao.getEntity(uuid).id
            assertThat(retrieved).isEqualTo(uuid)
        }

    @Test
    fun string_suppliedTwoWayConverter_insertWithQuery() =
        runTest(TwoWayStringConverterDatabase::class) { _, dao ->
            val text = "88c6af75-8d2a-489c-85c9-92e5dd8a108c"
            val uuid = UUID.fromString(text)

            // supplied converter replaces first '8' with '5' before storing into db
            dao.insertWithQuery(uuid)

            // supplied converter retrieves stored String and replaces first '6' with '2' before
            // converting into UUID
            val retrieved = dao.getEntity(uuid).id

            val newText = text
                .replaceFirst('8', '5')
                .replaceFirst('6', '2')
            val expected = UUID.fromString(newText)

            assertThat(retrieved).isEqualTo(expected)
        }

    @Test
    fun string_suppliedTwoWayConverter_insertEntity() =
        runTest(TwoWayStringConverterDatabase::class) { _, dao ->
            val text = "88c6af75-8d2a-489c-85c9-92e5dd8a108c"
            val uuid = UUID.fromString(text)

            // supplied converter replaces first '8' with '5' before storing into db
            dao.insert(UUIDStringEntity(uuid))

            // supplied converter retrieves stored String and replaces first '6' with '2' before
            // converting into UUID
            val retrieved = dao.getEntity(uuid).id

            val newText = text
                .replaceFirst('8', '5')
                .replaceFirst('6', '2')
            val expected = UUID.fromString(newText)

            assertThat(retrieved).isEqualTo(expected)
        }

    private fun runTest(
        kClass: KClass<out ConverterDb>,
        block: (ByteDao, StringDao) -> Unit,
    ) {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            kClass.java,
        ).build()

        try {
            block(db.dao(), db.stringDao())
        } finally {
            db.close()
        }
    }

    @Entity
    data class UUIDEntity(
        @PrimaryKey
        val id: UUID,
    )

    @Entity
    data class UUIDStringEntity(
        @PrimaryKey
        @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
        val id: UUID,
    )

    @Dao
    interface ByteDao {
        @Insert
        fun insert(byteEntity: UUIDEntity)

        @Query("INSERT INTO UUIDEntity (id) VALUES (:uuid)")
        fun insertWithQuery(uuid: UUID): Long

        @Query("SELECT * FROM UUIDEntity WHERE id = :uuid")
        fun getEntity(uuid: UUID): UUIDEntity
    }

    @Dao
    interface StringDao {
        @Insert
        fun insert(byteEntity: UUIDStringEntity)

        @Query("INSERT INTO UUIDStringEntity (id) VALUES (:uuid)")
        fun insertWithQuery(uuid: UUID): Long

        @Query("SELECT * FROM UUIDStringEntity WHERE id = :uuid")
        fun getEntity(uuid: UUID): UUIDStringEntity
    }

    internal abstract class ConverterDb : RoomDatabase() {
        abstract fun dao(): ByteDao
        abstract fun stringDao(): StringDao
    }

    // Database with no UUID converters supplied
    @Database(
        entities = [UUIDEntity::class, UUIDStringEntity::class],
        version = 1,
        exportSchema = false
    )
    internal abstract class NoConverterDatabase : ConverterDb()

    // Database with a byte[] to UUID converter supplied
    @Database(
        entities = [UUIDEntity::class, UUIDStringEntity::class],
        version = 1,
        exportSchema = false
    )
    @TypeConverters(FromByteConverter::class)
    internal abstract class ToUUIDConverterDatabase : ConverterDb()

    // Database with two way converter of UUID <--> byte[] supplied
    @Database(
        entities = [UUIDEntity::class, UUIDStringEntity::class],
        version = 1,
        exportSchema = false
    )
    @TypeConverters(TwoWayConverter::class)
    internal abstract class TwoWayConverterDatabase : ConverterDb()

    // Database with two way converter of UUID <--> String supplied
    @Database(
        entities = [UUIDEntity::class, UUIDStringEntity::class],
        version = 1,
        exportSchema = false
    )
    @TypeConverters(TwoWayStringConverter::class)
    internal abstract class TwoWayStringConverterDatabase : ConverterDb()

    class FromByteConverter {
        @TypeConverter
        fun fromByte(bytes: ByteArray): UUID {
            val bb = ByteBuffer.wrap(bytes)
            return UUID(bb.long, Long.MIN_VALUE)
        }
    }

    class TwoWayConverter {
        @TypeConverter
        fun fromByte(bytes: ByteArray): UUID {
            val bb = ByteBuffer.wrap(bytes)
            return UUID(bb.long, Long.MAX_VALUE)
        }

        @TypeConverter
        fun toByte(uuid: UUID): ByteArray {
            val bb = ByteBuffer.wrap(ByteArray(16))
            bb.putLong(uuid.leastSignificantBits)
            bb.putLong(uuid.leastSignificantBits)
            return bb.array()
        }
    }

    class TwoWayStringConverter {
        @TypeConverter
        fun fromString(text: String): UUID {
            val newText = text.replaceFirst('6', '2')
            return UUID.fromString(newText)
        }

        @TypeConverter
        fun toString(uuid: UUID): String {
            val text = uuid.toString()
            return text.replaceFirst('8', '5')
        }
    }
}