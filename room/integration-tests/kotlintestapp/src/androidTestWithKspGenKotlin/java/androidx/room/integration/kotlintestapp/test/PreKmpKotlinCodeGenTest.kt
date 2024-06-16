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
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.DeleteTable
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.ProvidedAutoMigrationSpec
import androidx.room.ProvidedTypeConverter
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.testing.MigrationTestHelper
import androidx.room.util.useCursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * This test is a bit special because it uses an already generated and frozen in time Room processor
 * generated code (pre-KMP). It helps us validate *new* runtime keeps working with *old* gen code.
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class PreKmpKotlinCodeGenTest {

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            instrumentation = InstrumentationRegistry.getInstrumentation(),
            databaseClass = PreKmpDatabase::class.java,
            specs = listOf(PreKmpDatabase.MigrationSpec1To2())
        )

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var db: PreKmpDatabase

    @BeforeTest
    fun setup() {
        db =
            Room.inMemoryDatabaseBuilder(context, PreKmpDatabase::class.java)
                .addAutoMigrationSpec(PreKmpDatabase.MigrationSpec1To2())
                .addTypeConverter(PreKmpDatabase.TheConverter())
                .build()
    }

    @Test
    fun simple() {
        db.getTheDao()
            .insert(
                PreKmpDatabase.TheEntity(
                    id = 2,
                    text = "ok",
                    custom = PreKmpDatabase.CustomData(byteArrayOf())
                )
            )
        assertThat(db.getTheDao().query().size).isEqualTo(1)
    }

    @Test
    fun migrate() {
        val filename = "pre-kmp-db"

        helper.createDatabase(filename, 1).use { db ->
            db.execSQL("INSERT INTO TheEntity (id, text, custom) VALUES (1, 'ok', x'0500')")
        }

        helper.runMigrationsAndValidate(filename, 2, true).use { db ->
            db.query("SELECT 1 FROM TheEntity WHERE text = 'ok'").useCursor {
                assertThat(it.moveToNext()).isTrue()
                assertThat(it.getInt(0)).isEqualTo(1)
            }
        }
    }
}

/* Commented out to prevent code generation, but left for documentation.
@Database(
    entities = [PreKmpDatabase.TheEntity::class],
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = PreKmpDatabase.MigrationSpec1To2::class)
    ],
    version = 2
)
*/
@TypeConverters(PreKmpDatabase.TheConverter::class)
abstract class PreKmpDatabase : RoomDatabase() {

    abstract fun getTheDao(): TheDao

    @Entity
    data class TheEntity(
        @PrimaryKey val id: Long,
        val text: String,
        @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val custom: CustomData
    )

    class CustomData(val blob: ByteArray)

    @Dao
    interface TheDao {
        @Insert fun insert(it: TheEntity)

        @Query("SELECT * FROM TheEntity") fun query(): List<TheEntity>
    }

    @ProvidedTypeConverter
    class TheConverter {
        @TypeConverter fun toCustomData(bytes: ByteArray) = CustomData(bytes)

        @TypeConverter fun fromCustomData(data: CustomData) = data.blob
    }

    @ProvidedAutoMigrationSpec
    @DeleteTable(tableName = "DeletedEntity")
    class MigrationSpec1To2 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            db.query(
                    "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'room_master_table'"
                )
                .useCursor {
                    assertThat(it.moveToNext())
                    assertThat(it.getInt(0)).isEqualTo(1)
                }
        }
    }
}
