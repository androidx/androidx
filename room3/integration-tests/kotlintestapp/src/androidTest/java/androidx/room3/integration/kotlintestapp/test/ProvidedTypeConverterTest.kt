/*
 * Copyright 2018 The Android Open Source Project
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
import androidx.kruth.assertWithMessage
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.ProvidedTypeConverter
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.TypeConverter
import androidx.room3.TypeConverters
import androidx.room3.integration.kotlintestapp.TestDatabase
import androidx.room3.integration.kotlintestapp.dao.PetDao
import androidx.room3.integration.kotlintestapp.dao.RobotsDao
import androidx.room3.integration.kotlintestapp.vo.Hivemind
import androidx.room3.integration.kotlintestapp.vo.Pet
import androidx.room3.integration.kotlintestapp.vo.PetUser
import androidx.room3.integration.kotlintestapp.vo.PetWithUser
import androidx.room3.integration.kotlintestapp.vo.Robot
import androidx.room3.integration.kotlintestapp.vo.Toy
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import java.nio.ByteBuffer
import java.util.Date
import java.util.Objects
import java.util.UUID
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ProvidedTypeConverterTest {
    @Test
    fun testProvidedTypeConverter() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val db =
            Room.inMemoryDatabaseBuilder<TestDatabaseWithConverterOne>(context)
                .addTypeConverter(UUIDConverter())
                .addTypeConverter(TimeStampConverter())
                .setDriver(AndroidSQLiteDriver())
                .build()
        val pet: Pet = TestUtil.createPet(3)
        pet.name = "pet"
        db.petDao().insertOrReplace(pet)

        val robot = Robot(UUID.randomUUID(), UUID.randomUUID())
        db.robotsDao().putRobot(robot)
        db.close()
    }

    @Test
    fun testSharedProvidedTypeConverter() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val db =
            Room.inMemoryDatabaseBuilder<TestDatabaseWithConverterTwo>(context)
                .addTypeConverter(UUIDConverter())
                .addTypeConverter(TimeStampConverter())
                .setDriver(AndroidSQLiteDriver())
                .build()
        val pet: Pet = TestUtil.createPet(3)
        db.petRobotDao().putPet(pet)

        val robot = Robot(UUID.randomUUID(), UUID.randomUUID())
        db.petRobotDao().putRobot(robot)
        db.close()
    }

    @Test
    fun testMissingProvidedTypeConverterInstance() {
        val context: Context = ApplicationProvider.getApplicationContext()
        try {
            val db =
                Room.inMemoryDatabaseBuilder<TestDatabaseWithConverterOne>(context)
                    .setDriver(AndroidSQLiteDriver())
                    .build()
            val pet: Pet = TestUtil.createPet(3)
            pet.name = "pet"
            db.petDao().insertOrReplace(pet)
            assertWithMessage("Should have thrown an IllegalArgumentException").fail()
        } catch (throwable: Throwable) {
            assertThat(throwable).isInstanceOf<IllegalArgumentException>()
        }
    }

    @Test
    fun testMissingProvidedTypeConverterAnnotation() {
        val context: Context = ApplicationProvider.getApplicationContext()
        try {
            val db: TestDatabase =
                Room.inMemoryDatabaseBuilder<TestDatabase>(context)
                    .addTypeConverter(TimeStampConverter())
                    .setDriver(AndroidSQLiteDriver())
                    .build()
            val pet: Pet = TestUtil.createPet(3)
            pet.name = "pet"
            db.petDao().insertOrReplace(pet)
            assertWithMessage("Should have thrown an IllegalArgumentException").fail()
        } catch (throwable: Throwable) {
            assertThat(throwable).isInstanceOf<IllegalArgumentException>()
        }
    }

    @Test
    fun differentSerializerForTheSameClassInDifferentDatabases() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val db1 =
            Room.inMemoryDatabaseBuilder<ProvidedTypeConverterNameLastNameDb>(context)
                .addTypeConverter(NameLastNameSerializer())
                .setDriver(AndroidSQLiteDriver())
                .build()
        val db2 =
            Room.inMemoryDatabaseBuilder<ProvidedTypeConverterLastNameNameDb>(context)
                .addTypeConverter(LastNameNameSerializer())
                .setDriver(AndroidSQLiteDriver())
                .build()
        val entity1 = ProvidedTypeConverterEntity(1, Username("foo1", "bar1"))
        val entity2 = ProvidedTypeConverterEntity(2, Username("foo2", "bar2"))
        db1.dao().insert(entity1)
        db2.dao().insert(entity2)
        assertThat(db1.dao()[1]).isEqualTo(entity1)
        assertThat(db2.dao()[2]).isEqualTo(entity2)
        assertThat(db1.dao().getRawUsername(1)).isEqualTo("foo1-bar1")
        assertThat(db2.dao().getRawUsername(2)).isEqualTo("bar2-foo2")
        db1.close()
        db2.close()
    }

    @Database(
        entities = [Pet::class, Toy::class, PetUser::class, Robot::class, Hivemind::class],
        views = [PetWithUser::class],
        version = 1,
        exportSchema = false,
    )
    @TypeConverters(TimeStampConverter::class, UUIDConverter::class)
    internal abstract class TestDatabaseWithConverterOne : RoomDatabase() {
        abstract fun petDao(): PetDao

        abstract fun robotsDao(): RobotsDao

        abstract fun petRobotDao(): PetRobotDao
    }

    @Database(entities = [Pet::class, Robot::class], version = 1, exportSchema = false)
    @TypeConverters(TimeStampConverter::class, UUIDConverter::class)
    internal abstract class TestDatabaseWithConverterTwo : RoomDatabase() {
        abstract fun petRobotDao(): PetRobotDao
    }

    /**
     * A DAOs that uses two type converters that are shared with other DAOs. Used to validate the
     * runtime validation of provided type converters.
     */
    @Dao
    interface PetRobotDao {
        @Insert fun putPet(pet: Pet)

        @Insert fun putRobot(robot: Robot)
    }

    @Database(entities = [ProvidedTypeConverterEntity::class], version = 1, exportSchema = false)
    @TypeConverters(NameLastNameSerializer::class)
    internal abstract class ProvidedTypeConverterNameLastNameDb : ProvidedTypeConverterEntityDb()

    @Database(entities = [ProvidedTypeConverterEntity::class], version = 1, exportSchema = false)
    @TypeConverters(LastNameNameSerializer::class)
    internal abstract class ProvidedTypeConverterLastNameNameDb : ProvidedTypeConverterEntityDb()

    internal abstract class ProvidedTypeConverterEntityDb : RoomDatabase() {
        abstract fun dao(): ProvidedTypeConverterEntity.Dao
    }

    @ProvidedTypeConverter
    class TimeStampConverter {
        @TypeConverter
        fun fromTimestamp(value: Long?): Date? {
            return if (value == null) null else Date(value)
        }

        @TypeConverter
        fun dateToTimestamp(date: Date?): Long? {
            return date?.time
        }
    }

    @ProvidedTypeConverter
    class UUIDConverter {
        @TypeConverter
        fun asUuid(bytes: ByteArray): UUID {
            val bb = ByteBuffer.wrap(bytes)
            val firstLong = bb.long
            val secondLong = bb.long
            return UUID(firstLong, secondLong)
        }

        @TypeConverter
        fun asBytes(uuid: UUID): ByteArray {
            val bb = ByteBuffer.wrap(ByteArray(16))
            bb.putLong(uuid.mostSignificantBits)
            bb.putLong(uuid.leastSignificantBits)
            return bb.array()
        }
    }

    @Entity
    class ProvidedTypeConverterEntity(@field:PrimaryKey val mId: Int, val mUserName: Username) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val that = other as ProvidedTypeConverterEntity
            return mId == that.mId && mUserName == that.mUserName
        }

        override fun hashCode(): Int {
            return Objects.hash(mId, mUserName)
        }

        @androidx.room3.Dao
        interface Dao {
            @Insert fun insert(entity: ProvidedTypeConverterEntity)

            @Query("SELECT mUsername FROM ProvidedTypeConverterEntity WHERE mId = :id")
            fun getRawUsername(id: Int): String?

            @Query("SELECT * FROM ProvidedTypeConverterEntity WHERE mId = :id")
            operator fun get(id: Int): ProvidedTypeConverterEntity?
        }
    }

    /** Class that is serialized differently based on database */
    class Username(val name: String, val lastName: String) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val username = other as Username
            return name == username.name && lastName == username.lastName
        }

        override fun hashCode(): Int {
            return Objects.hash(name, lastName)
        }
    }

    @ProvidedTypeConverter
    inner class NameLastNameSerializer {
        @TypeConverter
        fun fromString(input: String): Username {
            val sections = input.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return Username(sections[0], sections[1])
        }

        @TypeConverter
        fun toString(input: Username): String {
            return input.name + "-" + input.lastName
        }
    }

    @ProvidedTypeConverter
    inner class LastNameNameSerializer {
        @TypeConverter
        fun fromString(input: String): Username {
            val sections = input.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return Username(sections[1], sections[0])
        }

        @TypeConverter
        fun toString(input: Username): String {
            return input.lastName + "-" + input.name
        }
    }
}
