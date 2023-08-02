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

package androidx.room.integration.kotlintestapp.test

import androidx.kruth.assertThat
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@SmallTest
class NullableCollectionQueryParamTest {

    private val songOne = Song(1, "tag_1", 1)
    private val songTwo = Song(2, "tag_2", 2)

    lateinit var db: TestDatabase
    lateinit var dao: SongDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TestDatabase::class.java
        ).build()
        dao = db.getDao()

        dao.addSong(songOne)
        dao.addSong(songTwo)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun nullableList() {
        assertThat(dao.queryList(null))
            .containsExactly(songOne, songTwo)

        assertThat(dao.queryList(listOf("tag_1", "tag_2")))
            .containsExactly(songOne, songTwo)

        assertThat(dao.queryList(listOf("tag_2")))
            .containsExactly(songTwo)
    }

    @Test
    fun nullableArray() {
        assertThat(dao.queryArray(null))
            .containsExactly(songOne, songTwo)

        assertThat(dao.queryArray(arrayOf("tag_1", "tag_2")))
            .containsExactly(songOne, songTwo)

        assertThat(dao.queryArray(arrayOf("tag_2")))
            .containsExactly(songTwo)
    }

    @Test
    fun nullableVararg() {
        assertThat(dao.queryVarargs(null))
            .containsExactly(songOne, songTwo)

        assertThat(dao.queryVarargs("tag_1", "tag_2"))
            .containsExactly(songOne, songTwo)

        assertThat(dao.queryVarargs("tag_2"))
            .containsExactly(songTwo)
    }

    @Test
    fun nullablePrimitiveArray() {
        assertThat(dao.queryPrimitiveArray(null))
            .containsExactly(songOne, songTwo)

        assertThat(dao.queryPrimitiveArray(intArrayOf(1, 2)))
            .containsExactly(songOne, songTwo)

        assertThat(dao.queryPrimitiveArray(intArrayOf(2)))
            .containsExactly(songTwo)
    }

    @Entity
    data class Song(
        @PrimaryKey val id: Long,
        val tag: String,
        val num: Int
    )

    @Dao
    interface SongDao {

        @Insert
        fun addSong(song: Song)

        @Query(COALESCE_QUERY)
        fun queryList(inputTags: List<String>?): List<Song>

        @Query(COALESCE_QUERY)
        fun queryArray(inputTags: Array<String>?): List<Song>

        @Query(COALESCE_QUERY)
        fun queryVarargs(vararg inputTags: String?): List<Song>

        @Query("SELECT * FROM Song WHERE num IN (:inputNum) OR COALESCE(:inputNum, 0) = 0")
        fun queryPrimitiveArray(inputNum: IntArray?): List<Song>

        companion object {
            const val COALESCE_QUERY =
                "SELECT * FROM Song WHERE tag IN (:inputTags) OR COALESCE(:inputTags, 0) = 0"
        }
    }

    @Database(entities = [Song::class], version = 1, exportSchema = false)
    abstract class TestDatabase : RoomDatabase() {
        abstract fun getDao(): SongDao
    }
}
