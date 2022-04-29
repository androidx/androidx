/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.benchmark

import android.os.Build
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
class LargeQueryBenchmark(private val sampleSize: Int) {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val context = ApplicationProvider.getApplicationContext() as android.content.Context

    @Before
    fun setup() {
        for (postfix in arrayOf("", "-wal", "-shm")) {
            val dbFile = context.getDatabasePath(DB_NAME + postfix)
            if (dbFile.exists()) {
                Assert.assertTrue(dbFile.delete())
            }
        }
    }

    @Test
    fun testLargeQuerySingleParam() {
        val db = Room.databaseBuilder(context, TestDatabase::class.java, DB_NAME)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
        val dao = db.getUserDao()
        dao.insertUsers(generateUsers())

        val ids = List(sampleSize) { i -> i }

        benchmarkRule.measureRepeated {
            val result = dao.query(ids)
            Assert.assertEquals(result.size, sampleSize)
        }

        db.close()
    }

    @Test
    fun testLargeQueryMultipleParams() {
        val db = Room.databaseBuilder(context, TestDatabase::class.java, DB_NAME)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
        val dao = db.getUserDao()
        dao.insertUsers(generateUsers())

        var ids = List(sampleSize) { i -> i }
        val id4 = ids[ids.size - 1]
        val id3 = ids[ids.size - 2]
        ids = ids.subList(0, ids.size - 2)

        val ids1 = ids.subList(0, ids.size / 2)
        val ids2 = ids.subList(ids.size / 2, ids.size)

        benchmarkRule.measureRepeated {
            val result = dao.query(ids1, ids2, id3, id4)
            Assert.assertEquals(result.size, sampleSize)
        }

        db.close()
    }

    private fun generateUsers(): List<User> = List(sampleSize) { i -> User(i, "name $i") }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "sampleSize={0}")
        fun data() = listOf(100_000)

        private const val DB_NAME = "large-query-benchmark-test"
    }

    @Database(entities = [User::class], version = 1, exportSchema = false)
    abstract class TestDatabase : RoomDatabase() {
        abstract fun getUserDao(): UserDao
    }

    @Entity
    data class User(@PrimaryKey val id: Int, val name: String)

    @Dao
    interface UserDao {
        @Insert
        fun insertUsers(user: List<User>)

        @Query("SELECT * FROM User WHERE id IN (:ids)")
        fun query(ids: List<Int>): List<User>

        @Query("SELECT * FROM User WHERE id IN (:ids1) OR id IN (:ids2) OR id = :id3 OR id = :id4")
        fun query(ids1: List<Int>, ids2: List<Int>, id3: Int, id4: Int): List<User>
    }
}
