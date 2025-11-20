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

import androidx.kruth.assertThat
import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Relation
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.Transaction
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class RelationWithReservedKeywordTest {
    private lateinit var db: MyDatabase

    @Before
    fun initDb() {
        db =
            Room.inMemoryDatabaseBuilder<MyDatabase>(ApplicationProvider.getApplicationContext())
                .setDriver(AndroidSQLiteDriver())
                .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun loadRelation() {
        val category = Category(1, "cat1")
        db.getDao().insert(category)
        val topic = Topic(2, 1, "foo")
        db.getDao().insert(topic)

        val categoryWithTopics = db.getDao().loadAll()
        assertThat(categoryWithTopics).hasSize(1)
        assertThat(categoryWithTopics[0].category).isEqualTo(category)
        assertThat(categoryWithTopics[0].topics).containsExactly(topic)
    }

    @Entity(tableName = "categories")
    data class Category(@PrimaryKey(autoGenerate = true) val id: Long, val name: String)

    @Dao
    interface MyDao {
        @Transaction @Query("SELECT * FROM categories") fun loadAll(): List<CategoryWithTopics>

        @Insert fun insert(vararg categories: Category)

        @Insert fun insert(vararg topics: Topic)
    }

    @Database(entities = [Category::class, Topic::class], version = 1, exportSchema = false)
    abstract class MyDatabase : RoomDatabase() {
        abstract fun getDao(): MyDao
    }

    class CategoryWithTopics {
        @Embedded var category: Category? = null

        @Relation(parentColumn = "id", entityColumn = "category_id", entity = Topic::class)
        var topics: List<Topic> = emptyList()
    }

    @Entity(
        tableName = "topics",
        foreignKeys =
            [
                ForeignKey(
                    entity = Category::class,
                    parentColumns = ["id"],
                    childColumns = ["category_id"],
                    onDelete = ForeignKey.Companion.CASCADE,
                )
            ],
        indices = [Index("category_id")],
    )
    data class Topic(
        @PrimaryKey(autoGenerate = true) val id: Long,
        @ColumnInfo(name = "category_id") val categoryId: Long,
        val to: String,
    )
}
