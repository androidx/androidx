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

package androidx.room.integration.kotlintestapp.test

import android.content.Context
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.androidx.room.integration.kotlintestapp.vo.Experiment
import androidx.room.androidx.room.integration.kotlintestapp.vo.Schrodinger
import androidx.room.androidx.room.integration.kotlintestapp.vo.SchrodingerConverter
import androidx.room.integration.kotlintestapp.vo.DateConverter
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import java.util.Date
import java.util.UUID
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ValueClassConverterWrapperTest {

    @JvmInline
    value class UserWithInt(val password: Int)

    @JvmInline
    value class UserWithString(val password: String)

    @JvmInline
    value class UserWithUUID(val password: UUID)

    @JvmInline
    value class UserWithByte(val password: Byte)

    @JvmInline
    value class UserWithDate(val password: Date)

    @JvmInline
    value class UserWithGeneric<T>(val password: T)

    enum class Season {
        WINTER, SUMMER, SPRING, FALL
    }

    @JvmInline
    value class UserWithEnum(val password: Season)

    @JvmInline
    value class UserWithStringInternal(internal val password: String)

    @JvmInline
    value class UserWithByteArray(val password: ByteArray)

    @JvmInline
    value class NullableValue(val data: Int?)

    @Entity
    @TypeConverters(DateConverter::class, SchrodingerConverter::class)
    class UserInfo(
        @PrimaryKey
        val pk: Int,
        val userIntPwd: UserWithInt,
        val userStringPwd: UserWithString,
        val userUUIDPwd: UserWithUUID,
        val userBytePwd: UserWithByte,
        val userEnumPwd: UserWithEnum,
        val userDatePwd: UserWithDate,
        val userStringInternalPwd: UserWithStringInternal,
        val userGenericPwd: UserWithGeneric<String>,
        val userByteArrayPwd: UserWithByteArray,
        val schrodingerUser: Schrodinger
    ) {
        override fun equals(other: Any?): Boolean {
            val otherEntity = other as UserInfo
            return pk == otherEntity.pk &&
                userIntPwd == otherEntity.userIntPwd &&
                userStringPwd == otherEntity.userStringPwd &&
                userBytePwd == otherEntity.userBytePwd &&
                userUUIDPwd == otherEntity.userUUIDPwd &&
                userEnumPwd == otherEntity.userEnumPwd &&
                userDatePwd == otherEntity.userDatePwd &&
                userStringInternalPwd == otherEntity.userStringInternalPwd &&
                userGenericPwd == otherEntity.userGenericPwd &&
                userByteArrayPwd.password.contentEquals(otherEntity.userByteArrayPwd.password) &&
                schrodingerUser.experiment.isCatAlive ==
                otherEntity.schrodingerUser.experiment.isCatAlive
        }

        override fun hashCode(): Int {
            return 1
        }
    }

    @Entity
    data class UserInfoNullable(
        @PrimaryKey
        val pk: Int,
        val nullableUserIntPwd: UserWithInt?,
        val nullableData: NullableValue,
        val doubleNullableData: NullableValue?
    )

    @Dao
    interface SampleDao {
        @Query("SELECT * FROM UserInfo")
        fun getEntity(): UserInfo

        @Query("SELECT * FROM UserInfoNullable")
        fun getNullableEntity(): UserInfoNullable

        @Insert
        fun insert(item: UserInfo)

        @Insert
        fun insertNullableEntity(item: UserInfoNullable)
    }

    @Database(
        entities = [UserInfo::class, UserInfoNullable::class],
        version = 1,
        exportSchema = false
    )
    abstract class ValueClassConverterWrapperDatabase : RoomDatabase() {
        abstract fun dao(): SampleDao
    }

    private lateinit var db: ValueClassConverterWrapperDatabase
    private val pk = 0
    private val intPwd = UserWithInt(123)
    private val stringPwd = UserWithString("open_sesame")
    private val uuidPwd = UserWithUUID(UUID.randomUUID())
    private val bytePwd = UserWithByte(Byte.MIN_VALUE)
    private val enumPwd = UserWithEnum(Season.SUMMER)
    private val datePwd = UserWithDate(Date(2023L))
    private val internalPwd = UserWithStringInternal("open_sesame")
    private val genericPwd = UserWithGeneric("open_sesame")
    private val byteArrayPwd = UserWithByteArray(byteArrayOf(Byte.MIN_VALUE))
    private val shrodingerPwd = Schrodinger(Experiment("the cat is alive!"))

    @Test
    fun readAndWriteValueClassToDatabase() {
        val customerInfo = UserInfo(
            pk = pk,
            userIntPwd = intPwd,
            userStringPwd = stringPwd,
            userUUIDPwd = uuidPwd,
            userBytePwd = bytePwd,
            userEnumPwd = enumPwd,
            userDatePwd = datePwd,
            userStringInternalPwd = internalPwd,
            userGenericPwd = genericPwd,
            userByteArrayPwd = byteArrayPwd,
            schrodingerUser = shrodingerPwd
        )

        db.dao().insert(customerInfo)

        val readEntity = db.dao().getEntity()

        assertThat(readEntity).isEqualTo(customerInfo)
    }

    @Test
    fun readAndWriteNullableValueClassToDatabase() {
        val data = UserInfoNullable(
            pk = 1,
            nullableUserIntPwd = null,
            nullableData = NullableValue(1),
            null
        )

        db.dao().insertNullableEntity(data)

        val readEntity = db.dao().getNullableEntity()

        assertThat(readEntity).isEqualTo(data)
    }

    @Test
    fun invalidWriteNullableValueClassToDatabase() {
        val data = UserInfoNullable(
            pk = 1,
            nullableUserIntPwd = null,
            nullableData = NullableValue(null),
            null
        )

        assertThrows<IllegalStateException> {
            db.dao().insertNullableEntity(data)
        }.hasMessageThat().isEqualTo(
            "Cannot bind NULLABLE value 'data' of inline class 'NullableValue' to " +
                "a NOT NULL column."
        )
    }

    @Before
    fun initDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            ValueClassConverterWrapperDatabase::class.java
        ).build()
    }

    @After
    fun teardown() {
        db.close()
    }
}
