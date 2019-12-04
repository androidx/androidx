/*
 * Copyright (C) 2019 The Android Open Source Project
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

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItems
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ConstructorInheritanceTest {

    data class Info(
        val code: String
    )

    abstract class Parent(
        @PrimaryKey
        val id: Long,
        @Embedded
        val info: Info?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Parent
            if (id != other.id) return false
            if (info != other.info) return false
            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + (info?.hashCode() ?: 0)
            return result
        }
    }

    @Entity
    class Child1(id: Long, info: Info?) : Parent(id, info)

    @Entity
    class Child2(id: Long, info: Info?) : Parent(id, info)

    abstract class ChildGroup(
        @Embedded
        val child1: Child1,
        @Relation(entityColumn = "code", parentColumn = "code")
        val children2: List<Child2>
    )

    class ChildGroup1(child1: Child1, children2: List<Child2>) : ChildGroup(child1, children2)

    class ChildGroup2(child1: Child1, children2: List<Child2>) : ChildGroup(child1, children2)

    @Dao
    interface EmbeddedDao {
        @Insert
        fun insert(child1: Child1)

        @Insert
        fun insert(child2: Child2)

        @Suppress("unused")
        @Query("SELECT * FROM Child1 WHERE id = :id")
        fun loadById1(id: Long): Child1

        @Query("SELECT * FROM Child2 WHERE id = :id")
        fun loadById2(id: Long): Child2

        @Suppress("unused")
        @Transaction
        @Query("SELECT * FROM Child1 WHERE id = :id")
        fun loadGroupById1(id: Long): ChildGroup1

        @Transaction
        @Query("SELECT * FROM Child1 WHERE id = :id")
        fun loadGroupById2(id: Long): ChildGroup2
    }

    @Database(version = 1, exportSchema = false, entities = [Child1::class, Child2::class])
    abstract class EmbeddedDatabase : RoomDatabase() {
        abstract fun dao(): EmbeddedDao
    }

    @Test
    fun embeddedFieldInParent() {
        val db = openDatabase()
        val dao = db.dao()
        dao.insert(Child2(1, Info("123")))
        val child = dao.loadById2(1)
        assertThat(child.id, `is`(1L))
        assertThat(child.info!!.code, `is`(equalTo("123")))
    }

    @Test
    fun relationFieldInParent() {
        val db = openDatabase()
        val dao = db.dao()
        dao.insert(Child1(1, Info("123")))
        dao.insert(Child2(2, Info("123")))
        dao.insert(Child2(3, Info("123")))
        val childGroup = dao.loadGroupById2(1)
        assertThat(childGroup.child1.id, `is`(1L))
        assertThat(childGroup.children2, hasSize(2))
        assertThat(childGroup.children2, hasItems(
            Child2(2, Info("123")),
            Child2(3, Info("123"))))
    }

    private fun openDatabase(): EmbeddedDatabase {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return Room.inMemoryDatabaseBuilder(context, EmbeddedDatabase::class.java).build()
    }
}
