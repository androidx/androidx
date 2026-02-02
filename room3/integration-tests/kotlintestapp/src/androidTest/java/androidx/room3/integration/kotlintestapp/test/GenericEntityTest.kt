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
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class GenericEntityTest {
    private lateinit var db: GenericDb
    private lateinit var dao: GenericDao

    @Before
    fun init() {
        db =
            Room.inMemoryDatabaseBuilder<GenericDb>(ApplicationProvider.getApplicationContext())
                .setDriver(AndroidSQLiteDriver())
                .build()
        dao = db.getDao()
    }

    @After
    fun close() {
        db.close()
    }

    @Test
    fun readWriteEntity() {
        val item = EntityItem("abc", "def")
        dao.insert(item)
        val received = dao.get("abc")
        assertThat(received).isEqualTo(item)
    }

    @Test
    fun readPojo() {
        val item = EntityItem("abc", "def")
        dao.insert(item)
        val received = dao.getPojo("abc")
        assertThat(received.id).isEqualTo("abc")
    }

    open class Item<P, F>(@PrimaryKey val id: P) {
        var field: F? = null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Item<*, *>) return false

            if (id != other.id) return false
            if (field != other.field) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id?.hashCode() ?: 0
            result = 31 * result + (field?.hashCode() ?: 0)
            return result
        }
    }

    class PoKoItem(id: String) : Item<String, Int>(id)

    @Entity
    class EntityItem(id: String, val name: String) : Item<String, Int>(id) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            if (!super.equals(other)) return false
            val that = other as EntityItem
            return name == that.name
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + name.hashCode()
            return result
        }
    }

    @Dao
    interface GenericDao {
        @Insert fun insert(vararg items: EntityItem)

        @Query("SELECT * FROM EntityItem WHERE id = :id") fun get(id: String): EntityItem

        @Query("SELECT id, field FROM EntityItem WHERE id = :id") fun getPojo(id: String): PoKoItem
    }

    @Database(version = 1, entities = [EntityItem::class], exportSchema = false)
    abstract class GenericDb : RoomDatabase() {
        abstract fun getDao(): GenericDao
    }
}
