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
import androidx.kruth.assertThrows
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Delete
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.integration.kotlintestapp.test.TestDatabaseTest.UseDriver
import androidx.room3.withWriteTransaction
import androidx.sqlite.SQLiteException
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@SmallTest
@RunWith(Parameterized::class)
class ForeignKeyTest(private val useDriver: UseDriver) {

    private companion object {
        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = arrayOf(UseDriver.ANDROID, UseDriver.BUNDLED)
    }

    @Database(
        version = 1,
        entities = [A::class, B::class, C::class, D::class, E::class],
        exportSchema = false,
    )
    internal abstract class ForeignKeyDb : RoomDatabase() {
        abstract fun dao(): FkDao
    }

    @Dao
    internal interface FkDao {
        @Insert suspend fun insert(vararg a: A)

        @Insert suspend fun insert(vararg b: B)

        @Insert suspend fun insert(vararg c: C)

        @Insert suspend fun insert(vararg d: D)

        @Query("SELECT * FROM A WHERE id = :id") suspend fun loadA(id: Int): A?

        @Query("SELECT * FROM B WHERE id = :id") suspend fun loadB(id: Int): B?

        @Query("SELECT * FROM C WHERE id = :id") suspend fun loadC(id: Int): C?

        @Query("SELECT * FROM D WHERE id = :id") suspend fun loadD(id: Int): D?

        @Query("SELECT * FROM E WHERE id = :id") suspend fun loadE(id: Int): E?

        @Delete suspend fun delete(vararg a: A)

        @Delete suspend fun delete(vararg b: B)

        @Delete suspend fun delete(vararg c: C)

        @Query("UPDATE A SET name = :newName WHERE id = :id")
        suspend fun changeNameA(id: Int, newName: String)

        @Insert suspend fun insert(vararg e: E)
    }

    @Entity(
        indices = [Index(value = ["name"], unique = true), Index("name", "lastName", unique = true)]
    )
    internal data class A(
        @PrimaryKey(autoGenerate = true) var id: Int = 0,
        var name: String,
        var lastName: String? = null,
    )

    @Entity(
        foreignKeys =
            [ForeignKey(entity = A::class, parentColumns = ["name"], childColumns = ["aName"])],
        indices = [Index("aName")],
    )
    internal data class B(@PrimaryKey(autoGenerate = true) var id: Int = 0, var aName: String)

    @Entity(
        foreignKeys =
            [
                ForeignKey(
                    entity = A::class,
                    parentColumns = ["name"],
                    childColumns = ["aName"],
                    deferred = true,
                )
            ],
        indices = [Index("aName")],
    )
    internal data class C(@PrimaryKey(autoGenerate = true) var id: Int = 0, var aName: String)

    @Entity(
        foreignKeys =
            [
                ForeignKey(
                    entity = A::class,
                    parentColumns = ["name"],
                    childColumns = ["aName"],
                    onDelete = ForeignKey.CASCADE,
                    onUpdate = ForeignKey.CASCADE,
                )
            ],
        indices = [Index("aName")],
    )
    internal data class D(@PrimaryKey(autoGenerate = true) var id: Int = 0, var aName: String)

    @Entity(
        foreignKeys =
            [
                ForeignKey(
                    entity = A::class,
                    parentColumns = ["name", "lastName"],
                    childColumns = ["aName", "aLastName"],
                    onDelete = ForeignKey.SET_NULL,
                    onUpdate = ForeignKey.CASCADE,
                )
            ],
        indices = [Index("aName", "aLastName")],
    )
    internal data class E(
        @PrimaryKey(autoGenerate = true) var id: Int = 0,
        var aName: String? = null,
        var aLastName: String? = null,
    )

    private lateinit var db: ForeignKeyDb
    private lateinit var dao: FkDao

    @Before
    fun openDb() {
        db =
            Room.inMemoryDatabaseBuilder<ForeignKeyDb>(ApplicationProvider.getApplicationContext())
                .setDriver(
                    when (useDriver) {
                        UseDriver.ANDROID -> AndroidSQLiteDriver()
                        UseDriver.BUNDLED -> BundledSQLiteDriver()
                    }
                )
                .build()
        dao = db.dao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun simpleForeignKeyFailure() = runTest {
        assertThrowsForeignKeyError { dao.insert(B(aName = "foo")) }
    }

    @Test
    fun simpleForeignKeyDeferredFailure() = runTest {
        assertThrowsForeignKeyError { dao.insert(C(aName = "foo")) }
    }

    @Test
    fun immediateForeignKeyFailure() = runTest {
        assertThrowsForeignKeyError {
            db.withWriteTransaction {
                dao.insert(B(aName = "foo"))
                dao.insert(A(name = "foo"))
            }
        }
    }

    @Test
    fun deferredForeignKeySuccess() = runTest {
        db.withWriteTransaction {
            dao.insert(C(aName = "foo"))
            dao.insert(A(name = "foo"))
        }
        assertThat(dao.loadA(1)).isNotNull()
        assertThat(dao.loadC(1)).isNotNull()
    }

    @Test
    fun onDelete_noAction() = runTest {
        dao.insert(A(name = "a1"))
        val a = checkNotNull(dao.loadA(1))
        dao.insert(B(aName = "a1"))
        assertThrowsForeignKeyError { dao.delete(a) }
    }

    @Test
    fun onDelete_noAction_withTransaction() = runTest {
        dao.insert(A(name = "a1"))
        val a = checkNotNull(dao.loadA(1))
        dao.insert(B(aName = "a1"))
        val b = checkNotNull(dao.loadB(1))
        assertThrowsForeignKeyError {
            db.withWriteTransaction {
                dao.delete(a)
                dao.delete(b)
            }
        }
    }

    @Test
    fun onDelete_noAction_deferred() = runTest {
        dao.insert(A(name = "a1"))
        val a = checkNotNull(dao.loadA(1))
        dao.insert(C(aName = "a1"))
        assertThrowsForeignKeyError { dao.delete(a) }
    }

    @Test
    fun onDelete_noAction__deferredWithTransaction() = runTest {
        dao.insert(A(name = "a1"))
        val a = checkNotNull(dao.loadA(1))
        dao.insert(C(aName = "a1"))
        val c = checkNotNull(dao.loadC(1))
        db.withWriteTransaction {
            dao.delete(a)
            dao.delete(c)
        }
    }

    @Test
    fun onDelete_cascade() = runTest {
        dao.insert(A(name = "a1"))
        val a = checkNotNull(dao.loadA(1))
        dao.insert(D(aName = "a1"))
        val d = dao.loadD(1)
        assertThat(d).isNotNull()
        dao.delete(a)
        assertThat(dao.loadD(1)).isNull()
    }

    @Test
    fun onUpdate_cascade() = runTest {
        dao.insert(A(name = "a1"))
        dao.insert(D(aName = "a1"))
        val d = dao.loadD(1)
        assertThat(d).isNotNull()
        dao.changeNameA(1, "bla")
        assertThat(dao.loadD(1)!!.aName).isEqualTo("bla")
        assertThat(dao.loadA(1)!!.name).isEqualTo("bla")
    }

    @Test
    fun multipleReferences() = runTest {
        dao.insert(A(name = "a1", lastName = "a2"))
        val a = dao.loadA(1)
        assertThat(a).isNotNull()
        assertThrowsForeignKeyError { dao.insert(E(aName = "a1", aLastName = "dsa")) }
    }

    @Test
    fun onDelete_setNull_multipleReferences() = runTest {
        dao.insert(A(name = "a1", lastName = "a2"))
        val a = checkNotNull(dao.loadA(1))
        dao.insert(E(aName = "a1", aLastName = "a2"))
        assertThat(dao.loadE(1)).isNotNull()
        dao.delete(a)
        val e = checkNotNull(dao.loadE(1))
        assertThat(e.aName).isNull()
        assertThat(e.aLastName).isNull()
    }

    @Test
    fun onUpdate_cascade_multipleReferences() = runTest {
        dao.insert(A(name = "a1", lastName = "a2"))
        dao.insert(E(aName = "a1", aLastName = "a2"))
        assertThat(dao.loadE(1)).isNotNull()
        dao.changeNameA(1, "foo")
        assertThat(dao.loadE(1)).isNotNull()
        assertThat(dao.loadE(1)!!.aName).isEqualTo("foo")
        assertThat(dao.loadE(1)!!.aLastName).isEqualTo("a2")
    }

    private inline fun assertThrowsForeignKeyError(block: () -> Unit) =
        assertThrows<SQLiteException>(block).hasMessageThat().contains("FOREIGN KEY")
}
