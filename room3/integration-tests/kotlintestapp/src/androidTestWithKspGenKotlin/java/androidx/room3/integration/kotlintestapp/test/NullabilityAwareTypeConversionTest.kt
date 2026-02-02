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

package androidx.room3.integration.kotlintestapp.test

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
import androidx.room3.useReaderConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class NullabilityAwareTypeConversionTest {
    lateinit var dao: UserDao
    private val nullableConvertors = NullableTypeConverters()

    @Before
    fun init() {
        dao =
            Room.inMemoryDatabaseBuilder<NullAwareConverterDatabase>(
                    ApplicationProvider.getApplicationContext()
                )
                .setDriver(AndroidSQLiteDriver())
                .addTypeConverter(nullableConvertors)
                .build()
                .userDao
    }

    private fun assertNullableConverterIsNotUsed() {
        assertWithMessage(
                "should've not used nullable conversion since it is not available in this scope"
            )
            .that(nullableConvertors.toStringInvocations)
            .isEmpty()
        assertWithMessage(
                "should've not used nullable conversion since it is not available in this scope"
            )
            .that(nullableConvertors.fromStringInvocations)
            .isEmpty()
    }

    @Test
    fun insert() {
        val user =
            User(id = 1, nonNullCountry = Country.FRANCE, nullableCountry = Country.UNITED_KINGDOM)
        dao.insert(user)
        assertThat(dao.getRawData()).isEqualTo("1-FR-UK")
        assertNullableConverterIsNotUsed()
    }

    @Test
    fun setNonNullColumn() {
        val user = User(id = 1, nonNullCountry = Country.FRANCE, nullableCountry = null)
        dao.insert(user)
        assertThat(dao.getRawData()).isEqualTo("1-FR-null")
        dao.setNonNullCountry(id = 1, nonNullCountry = Country.UNITED_KINGDOM)
        assertThat(dao.getRawData()).isEqualTo("1-UK-null")
        assertNullableConverterIsNotUsed()
    }

    @Test
    fun setNullableColumn() {
        val user =
            User(id = 1, nonNullCountry = Country.FRANCE, nullableCountry = Country.UNITED_KINGDOM)
        dao.insert(user)
        dao.setNullableCountry(id = 1, nullableCountry = null)
        assertThat(dao.getRawData()).isEqualTo("1-FR-null")
        dao.setNullableCountry(id = 1, nullableCountry = Country.UNITED_KINGDOM)
        assertThat(dao.getRawData()).isEqualTo("1-FR-UK")
        assertNullableConverterIsNotUsed()
    }

    @Test
    fun load() {
        val user1 =
            User(id = 1, nonNullCountry = Country.FRANCE, nullableCountry = Country.UNITED_KINGDOM)
        dao.insert(user1)
        assertThat(dao.getById(1)).isEqualTo(user1)
        val user2 = User(id = 2, nonNullCountry = Country.UNITED_KINGDOM, nullableCountry = null)
        dao.insert(user2)
        assertThat(dao.getById(2)).isEqualTo(user2)
        assertNullableConverterIsNotUsed()
    }

    @Test
    fun useNullableConverter() {
        val user =
            User(id = 1, nonNullCountry = Country.FRANCE, nullableCountry = Country.UNITED_KINGDOM)
        dao.insert(user)
        dao.setNullableCountryWithNullableTypeConverter(id = 1, nullableCountry = null)
        assertThat(dao.getRawData()).isEqualTo("1-FR-null")
        assertThat(nullableConvertors.toStringInvocations).containsExactly(null)
    }

    @Test
    fun loadNonNullColumn() {
        val user = User(id = 1, nonNullCountry = Country.FRANCE, nullableCountry = null)
        dao.insert(user)
        val country = dao.getNonNullCountry(id = 1)
        assertThat(country).isEqualTo(Country.FRANCE)
        assertNullableConverterIsNotUsed()
    }

    @Test
    fun loadNullableColumn() {
        val user = User(id = 1, nonNullCountry = Country.FRANCE, nullableCountry = null)
        dao.insert(user)
        val country = dao.getNullableCountry(id = 1)
        assertThat(country).isNull()
        assertNullableConverterIsNotUsed()
    }

    @Test
    fun loadNonNullColumn_withNullableConverter() {
        val user = User(id = 1, nonNullCountry = Country.FRANCE, nullableCountry = null)
        dao.insert(user)
        val country = dao.getNonNullCountryWithNullableTypeConverter(id = 1)
        assertThat(country).isEqualTo(Country.FRANCE)
        // return value is non-null so it is better to use non-null converter and assume
        // column is non-null, instead of using the nullable converter
        assertNullableConverterIsNotUsed()
    }

    @Test
    fun loadNonNullColumn_asNullable_withNullableConverter() {
        val user = User(id = 1, nonNullCountry = Country.FRANCE, nullableCountry = null)
        dao.insert(user)
        val country = dao.getNonNullCountryAsNullableWithNullableTypeConverter(id = 1)
        assertThat(country).isEqualTo(Country.FRANCE)
        // return value is nullable so we are using the nullable converter because room does not
        // know that the column is non-null.
        // if one day Room understands it and this test fails, feel free to update it.
        // We still want this test because right now Room does not know column is non-null hence
        // it should prefer the nullable converter.
        assertThat(nullableConvertors.fromStringInvocations).containsExactly("FR")
    }

    @Test
    fun loadNullableColumn_withNullableConverter() {
        val user = User(id = 1, nonNullCountry = Country.FRANCE, nullableCountry = null)
        dao.insert(user)
        val country = dao.getNullableCountryWithNullableTypeConverter(id = 1)
        assertThat(country).isNull()
        assertThat(nullableConvertors.fromStringInvocations).containsExactly(null)
    }

    @Database(version = 1, entities = [User::class], exportSchema = false)
    @TypeConverters(NonNullTypeConverters::class)
    abstract class NullAwareConverterDatabase : RoomDatabase() {
        abstract val userDao: UserDao
    }

    @Dao
    abstract class UserDao(private val db: RoomDatabase) {

        @Insert abstract fun insert(user: User): Long

        @Query("UPDATE user SET nonNullCountry = :nonNullCountry WHERE id = :id")
        abstract fun setNonNullCountry(id: Long, nonNullCountry: Country)

        @Query("UPDATE user SET nullableCountry = :nullableCountry WHERE id = :id")
        abstract fun setNullableCountry(id: Long, nullableCountry: Country?)

        @Query("SELECT * FROM user WHERE id = :id") abstract fun getById(id: Long): User?

        @Query("UPDATE user SET nullableCountry = :nullableCountry WHERE id = :id")
        @TypeConverters(NullableTypeConverters::class)
        abstract fun setNullableCountryWithNullableTypeConverter(
            id: Long,
            nullableCountry: Country?,
        )

        @Query("SELECT nonNullCountry FROM user WHERE id = :id")
        abstract fun getNonNullCountry(id: Long): Country

        @Query("SELECT nullableCountry FROM user WHERE id = :id")
        abstract fun getNullableCountry(id: Long): Country?

        @Query("SELECT nullableCountry FROM user WHERE id = :id")
        @TypeConverters(NullableTypeConverters::class)
        abstract fun getNullableCountryWithNullableTypeConverter(id: Long): Country?

        @Query("SELECT nonNullCountry FROM user WHERE id = :id")
        @TypeConverters(NullableTypeConverters::class)
        abstract fun getNonNullCountryWithNullableTypeConverter(id: Long): Country

        @Query("SELECT nonNullCountry FROM user WHERE id = :id")
        @TypeConverters(NullableTypeConverters::class)
        abstract fun getNonNullCountryAsNullableWithNullableTypeConverter(id: Long): Country?

        /**
         * Return raw data in the database so that we can assert what is in the database without
         * room's converters
         */
        fun getRawData(): String = runBlocking {
            buildString {
                db.useReaderConnection { connection ->
                    connection.usePrepared("SELECT * FROM user") {
                        if (it.step()) {
                            append(it.getInt(0))
                            append("-")
                            if (it.isNull(1)) {
                                append("null")
                            } else {
                                append(it.getText(1))
                            }
                            append("-")
                            if (it.isNull(2)) {
                                append("null")
                            } else {
                                append(it.getText(2))
                            }
                        }
                    }
                }
            }
        }
    }

    @Entity(tableName = "user")
    data class User(
        @PrimaryKey val id: Long,
        val nonNullCountry: Country,
        val nullableCountry: Country?,
    )

    enum class Country(val countryCode: String) {
        UNITED_KINGDOM("UK"),
        FRANCE("FR"),
    }

    object NonNullTypeConverters {
        @TypeConverter
        fun toString(country: Country): String {
            return country.countryCode
        }

        @TypeConverter
        fun toCountry(string: String): Country {
            return Country.entries.find { it.countryCode == string }
                ?: throw IllegalArgumentException("Country code '$string' not found")
        }
    }

    @ProvidedTypeConverter
    class NullableTypeConverters {
        val toStringInvocations = mutableListOf<Country?>()
        val fromStringInvocations = mutableListOf<String?>()

        @TypeConverter
        fun toString(country: Country?): String? {
            toStringInvocations.add(country)
            return country?.countryCode
        }

        @TypeConverter
        fun toCountry(string: String?): Country? {
            fromStringInvocations.add(string)
            if (string == null) {
                return null
            }
            return Country.entries.find { it.countryCode == string }
                ?: throw IllegalArgumentException("Country code '$string' not found")
        }
    }
}
