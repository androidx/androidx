/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.testutils.generateAllEnumerations
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class RelationBenchmark(private val parentSampleSize: Int, private val childSampleSize: Int) {

    @get:Rule val benchmarkRule = BenchmarkRule()

    val context = ApplicationProvider.getApplicationContext() as android.content.Context

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
    @Ignore // b/410015038
    fun largeRelationQuery() {
        val db =
            Room.databaseBuilder(context, TestDatabase::class.java, DB_NAME)
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .build()
        val dao = db.getUserDao()

        val users = List(parentSampleSize) { i -> User(i, "name$i") }
        val items = List(parentSampleSize * childSampleSize) { i -> Item(i, i / childSampleSize) }
        dao.insertUsers(users)
        dao.insertItems(items)

        benchmarkRule.measureRepeated {
            val result = dao.getUserWithItems()
            assertEquals(result.size, parentSampleSize)
            assertEquals(result.first().items.size, childSampleSize)
            assertEquals(result.last().items.size, childSampleSize)
        }

        db.close()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "parentSampleSize={0}, childSampleSize={1}")
        fun data() = generateAllEnumerations(listOf(100, 500, 1000), listOf(10))

        private const val DB_NAME = "relation-benchmark-test"
    }
}
