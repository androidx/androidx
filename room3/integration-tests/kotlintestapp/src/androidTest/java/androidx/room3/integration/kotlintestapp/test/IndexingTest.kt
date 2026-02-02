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
import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class IndexingTest {

    @Entity(
        tableName = "foo_table",
        indices =
            [
                Index("field1", "field2"),
                Index(value = ["field2", "id"], unique = true),
                Index(value = ["field2"], unique = true, name = "customIndex"),
            ],
    )
    data class Entity1(
        @PrimaryKey val id: Int,
        val field1: String,
        val field2: String,
        @ColumnInfo(index = true, name = "my_field") val field3: String,
    )

    data class IndexInfo(
        val name: String,
        @ColumnInfo(name = "tbl_name") val tableName: String,
        val sql: String,
    )

    @Dao
    internal interface Entity1Dao {
        @Insert fun insert(item: Entity1)

        @Query("SELECT * FROM foo_table indexed by customIndex where field2 = :inp")
        fun indexedBy(inp: String): List<Entity1>
    }

    @Dao
    internal interface SqlMasterDao {
        @Query("SELECT name, tbl_name, sql FROM sqlite_master WHERE type = 'index'")
        fun loadIndices(): List<IndexInfo>
    }

    @Database(entities = [Entity1::class], version = 1, exportSchema = false)
    internal abstract class IndexingDb : RoomDatabase() {
        abstract fun sqlMasterDao(): SqlMasterDao

        abstract fun entity1Dao(): Entity1Dao
    }

    @Test
    fun verifyIndices() {
        val db =
            Room.inMemoryDatabaseBuilder<IndexingDb>(ApplicationProvider.getApplicationContext())
                .setDriver(AndroidSQLiteDriver())
                .build()
        val indices = db.sqlMasterDao().loadIndices()
        assertThat(indices).hasSize(4)
        for (info in indices) {
            assertThat(info.tableName).isEqualTo("foo_table")
        }
        assertThat(indices[0].sql)
            .isEqualTo(
                "CREATE INDEX `index_foo_table_field1_field2` ON `foo_table` (`field1`, `field2`)"
            )
        assertThat(indices[1].sql)
            .isEqualTo(
                "CREATE UNIQUE INDEX `index_foo_table_field2_id` ON `foo_table` (`field2`, `id`)"
            )
        assertThat(indices[2].sql)
            .isEqualTo("CREATE UNIQUE INDEX `customIndex` ON `foo_table` (`field2`)")
        assertThat(indices[3].sql)
            .isEqualTo("CREATE INDEX `index_foo_table_my_field` ON `foo_table` (`my_field`)")
        db.close()
    }

    @Test
    fun indexedByQuery() {
        val db =
            Room.inMemoryDatabaseBuilder<IndexingDb>(ApplicationProvider.getApplicationContext())
                .setDriver(AndroidSQLiteDriver())
                .build()
        db.entity1Dao().insert(Entity1(1, "a", "b", "c"))
        val result = db.entity1Dao().indexedBy("b")
        assertThat(result).hasSize(1)
        db.close()
    }
}
