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

package androidx.room

import androidx.kruth.assertThat
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.sqlite.db.SupportSQLiteProgram
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(JUnit4::class)
class RoomSQLiteQueryTest {
    @Before
    fun clear() {
        RoomSQLiteQuery.queryPool.clear()
    }

    @Test
    fun acquireBasic() {
        val query = acquire("abc", 3)
        assertThat(query.sql).isEqualTo("abc")
        assertThat(query.argCount).isEqualTo(3)
        assertThat(query.blobBindings.size).isEqualTo(4)
        assertThat(query.longBindings.size).isEqualTo(4)
        assertThat(query.stringBindings.size).isEqualTo(4)
        assertThat(query.doubleBindings.size).isEqualTo(4)
    }

    @Test
    fun acquireSameSizeAgain() {
        val query = acquire("abc", 3)
        query.release()
        assertThat(acquire("blah", 3)).isSameInstanceAs(query)
    }

    @Test
    fun acquireSameSizeWithoutRelease() {
        val query = acquire("abc", 3)
        assertThat(
            acquire("fda", 3)
        ).isNotSameInstanceAs(query)
    }

    @Test
    fun bindings() {
        val query = acquire("abc", 6)
        val myBlob = ByteArray(3)
        val myLong = 3L
        val myDouble = 7.0
        val myString = "ss"
        query.bindBlob(1, myBlob)
        query.bindLong(2, myLong)
        query.bindNull(3)
        query.bindDouble(4, myDouble)
        query.bindString(5, myString)
        query.bindNull(6)
        val program: SupportSQLiteProgram = mock()
        query.bindTo(program)
        verify(program).bindBlob(1, myBlob)
        verify(program).bindLong(2, myLong)
        verify(program).bindNull(3)
        verify(program).bindDouble(4, myDouble)
        verify(program).bindString(5, myString)
        verify(program).bindNull(6)
    }

    @Test
    fun dontKeepSameSizeTwice() {
        val query1 = acquire("abc", 3)
        val query2 = acquire("zx", 3)
        val query3 = acquire("qw", 0)
        query1.release()
        query2.release()
        assertThat(RoomSQLiteQuery.queryPool.size).isEqualTo(1)
        query3.release()
        assertThat(RoomSQLiteQuery.queryPool.size).isEqualTo(2)
    }

    @Test
    fun returnExistingForSmallerSize() {
        val query = acquire("abc", 3)
        query.release()
        assertThat(acquire("dsa", 2)).isSameInstanceAs(query)
    }

    @Test
    fun returnNewForBigger() {
        val query = acquire("abc", 3)
        query.release()
        assertThat(
            acquire("dsa", 4)
        ).isNotSameInstanceAs(query)
    }

    @Test
    fun pruneCache() {
        for (i in 0 until RoomSQLiteQuery.POOL_LIMIT) {
            acquire("dsdsa", i).release()
        }
        pruneCacheTest()
    }

    @Test
    fun pruneCacheReverseInsertion() {
        val queries: MutableList<RoomSQLiteQuery> = ArrayList()
        for (i in RoomSQLiteQuery.POOL_LIMIT - 1 downTo 0) {
            queries.add(acquire("dsdsa", i))
        }
        for (query in queries) {
            query.release()
        }
        pruneCacheTest()
    }

    private fun pruneCacheTest() {
        assertThat(
            RoomSQLiteQuery.queryPool.size
        ).isEqualTo(RoomSQLiteQuery.POOL_LIMIT)
        acquire("dsadsa", RoomSQLiteQuery.POOL_LIMIT + 1).release()
        assertThat(
            RoomSQLiteQuery.queryPool.size
        ).isEqualTo(RoomSQLiteQuery.DESIRED_POOL_SIZE)
        val itr: Iterator<RoomSQLiteQuery> = RoomSQLiteQuery.queryPool.values.iterator()
        for (i in 0 until RoomSQLiteQuery.DESIRED_POOL_SIZE) {
            assertThat(itr.next().capacity).isEqualTo(i)
        }
    }
}
