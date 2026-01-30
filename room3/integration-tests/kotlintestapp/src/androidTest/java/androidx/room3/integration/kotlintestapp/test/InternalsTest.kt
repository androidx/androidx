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
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Test to make sure we can generate proper code when developer uses internals hence jvm names might
 * be different than what developer sees
 */
@SmallTest
class InternalsTest {
    @Database(version = 1, entities = [InternalEntity::class], exportSchema = false)
    internal abstract class InternalDb : RoomDatabase() {
        abstract fun getDao(): InternalDao
    }

    @Entity
    internal class InternalEntity(
        @PrimaryKey internal val id: Long,
        internal val internalField: String,
        val publicField: String,
    ) {
        // these are added to have setters
        internal var internalFieldProp: String = ""
        var publicFieldProp: String = ""

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is InternalEntity) return false
            if (id != other.id) return false
            if (internalField != other.internalField) return false
            if (publicField != other.publicField) return false
            if (internalFieldProp != other.internalFieldProp) return false
            if (publicFieldProp != other.publicFieldProp) return false
            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + internalField.hashCode()
            result = 31 * result + publicField.hashCode()
            result = 31 * result + internalFieldProp.hashCode()
            result = 31 * result + publicFieldProp.hashCode()
            return result
        }
    }

    @Dao
    internal abstract class InternalDao {
        @Insert abstract fun insert(entity: InternalEntity)

        @Query("SELECT * FROM InternalEntity WHERE publicField LIKE :field")
        abstract fun byPublicField(field: String): List<InternalEntity>

        @Query("SELECT * FROM InternalEntity WHERE internalField LIKE :field")
        abstract fun byInternalField(field: String): List<InternalEntity>

        @Query("SELECT * FROM InternalEntity WHERE publicFieldProp LIKE :field")
        abstract fun byPublicFieldProp(field: String): List<InternalEntity>

        @Query("SELECT * FROM InternalEntity WHERE internalFieldProp LIKE :field")
        abstract fun byInternalFieldProp(field: String): List<InternalEntity>
    }

    private lateinit var db: InternalDb

    @Before
    fun init() {
        db =
            Room.inMemoryDatabaseBuilder<InternalDb>(
                    InstrumentationRegistry.getInstrumentation().targetContext
                )
                .setDriver(AndroidSQLiteDriver())
                .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun test() {
        val entity =
            InternalEntity(id = 1, internalField = "if", publicField = "pf").also {
                it.internalFieldProp = "ifp"
                it.publicFieldProp = "pfp"
            }
        db.getDao().insert(entity)
        assertThat(db.getDao().byInternalField(field = "if")).containsExactly(entity)
        assertThat(db.getDao().byPublicField(field = "pf")).containsExactly(entity)
        assertThat(db.getDao().byInternalFieldProp(field = "ifp")).containsExactly(entity)
        assertThat(db.getDao().byPublicFieldProp(field = "pfp")).containsExactly(entity)
    }
}
