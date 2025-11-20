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

import androidx.kruth.assertThat
import androidx.room3.ColumnInfo
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
import java.util.Locale
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Validates Android's built-in 'LOCALIZED' collator */
@SmallTest
@RunWith(AndroidJUnit4::class)
class CollationTest {

    private lateinit var db: CollateDb
    private lateinit var dao: CollateDao
    private lateinit var defaultLocale: Locale
    private val item1 = CollateEntity(1, "abı")
    private val item2 = CollateEntity(2, "abi")
    private val item3 = CollateEntity(3, "abj")
    private val item4 = CollateEntity(4, "abç")

    @Before
    fun init() {
        defaultLocale = Locale.getDefault()
    }

    private fun initDao(systemLocale: Locale) {
        Locale.setDefault(systemLocale)
        db =
            Room.inMemoryDatabaseBuilder<CollateDb>(ApplicationProvider.getApplicationContext())
                .setDriver(AndroidSQLiteDriver())
                .build()
        dao = db.dao()
        dao.insert(item1, item2, item3, item4)
    }

    @After
    fun closeDb() {
        db.close()
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun localized() {
        initDao(Locale("tr", "TR"))
        val result = dao.sortedByLocalized()
        assertThat(result).containsExactly(item4, item1, item2, item3).inOrder()
    }

    @Test
    fun localized_asUnicode() {
        initDao(Locale.getDefault())
        val result = dao.sortedByLocalizedAsUnicode()
        assertThat(result).containsExactly(item4, item2, item1, item3).inOrder()
    }

    @Test
    fun unicode_asLocalized() {
        initDao(Locale("tr", "TR"))
        val result = dao.sortedByUnicodeAsLocalized()
        assertThat(result).containsExactly(item4, item1, item2, item3).inOrder()
    }

    @Test
    fun unicode() {
        initDao(Locale.getDefault())
        val result = dao.sortedByUnicode()
        assertThat(result).containsExactly(item4, item2, item1, item3).inOrder()
    }

    @Entity
    data class CollateEntity(
        @PrimaryKey val id: Int,
        @ColumnInfo(collate = ColumnInfo.LOCALIZED) val localizedName: String,
        @ColumnInfo(collate = ColumnInfo.UNICODE) val unicodeName: String,
    ) {
        constructor(id: Int, name: String) : this(id, name, name)
    }

    @Dao
    interface CollateDao {
        @Query("SELECT * FROM CollateEntity ORDER BY localizedName ASC")
        fun sortedByLocalized(): List<CollateEntity>

        @Query("SELECT * FROM CollateEntity ORDER BY localizedName COLLATE UNICODE ASC")
        fun sortedByLocalizedAsUnicode(): List<CollateEntity>

        @Query("SELECT * FROM CollateEntity ORDER BY unicodeName ASC")
        fun sortedByUnicode(): List<CollateEntity>

        @Query("SELECT * FROM CollateEntity ORDER BY unicodeName COLLATE LOCALIZED ASC")
        fun sortedByUnicodeAsLocalized(): List<CollateEntity>

        @Insert fun insert(vararg entities: CollateEntity)
    }

    @Database(entities = [CollateEntity::class], version = 1, exportSchema = false)
    abstract class CollateDb : RoomDatabase() {
        abstract fun dao(): CollateDao
    }
}
