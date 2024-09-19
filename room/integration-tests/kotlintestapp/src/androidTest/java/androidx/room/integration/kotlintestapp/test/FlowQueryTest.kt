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

package androidx.room.integration.kotlintestapp.test

import androidx.kruth.assertThat
import androidx.room.integration.kotlintestapp.vo.Book
import androidx.room.integration.kotlintestapp.vo.Playlist
import androidx.room.integration.kotlintestapp.vo.PlaylistSongXRef
import androidx.room.integration.kotlintestapp.vo.PlaylistWithSongs
import androidx.room.integration.kotlintestapp.vo.Song
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Ignore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class FlowQueryTest : TestDatabaseTest() {

    @After
    fun teardown() {
        // At the end of all tests, query executor should be idle.
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
        assertThat(countingTaskExecutorRule.isIdle).isTrue()
    }

    @Test
    fun collectBooks_takeOne() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        booksDao.getBooksFlow().take(1).collect {
            assertThat(it).isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
        }
    }

    @Test
    fun collectBooks_first() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val result = booksDao.getBooksFlow().first()
        assertThat(result).isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
    }

    @Test
    fun collectBooks_first_inTransaction() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        database.withTransaction {
            booksDao.insertBookSuspend(TestUtil.BOOK_2)
            val result = booksDao.getBooksFlow().first()
            assertThat(result).isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
        }
    }

    @Test
    fun collectBooks_async() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val latch = CountDownLatch(1)
        val job =
            launch(Dispatchers.IO) {
                booksDao.getBooksFlow().collect {
                    assertThat(it).isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
                    latch.countDown()
                }
            }

        latch.await()
        job.cancelAndJoin()
    }

    @Test
    fun receiveBooks_async_update() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val firstResultLatch = CountDownLatch(1)
        val secondResultLatch = CountDownLatch(1)
        val results = mutableListOf<List<Book>>()
        val job =
            launch(Dispatchers.IO) {
                booksDao.getBooksFlow().collect {
                    when (results.size) {
                        0 -> {
                            results.add(it)
                            firstResultLatch.countDown()
                        }
                        1 -> {
                            results.add(it)
                            secondResultLatch.countDown()
                        }
                        else -> fail("Should have only collected 2 results.")
                    }
                }
            }

        firstResultLatch.await()
        booksDao.insertBookSuspend(TestUtil.BOOK_3)

        secondResultLatch.await()
        assertThat(results.size).isEqualTo(2)
        assertThat(results[0]).isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
        assertThat(results[1]).isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2, TestUtil.BOOK_3))

        job.cancelAndJoin()
    }

    @Test
    fun receiveBooks() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val channel = booksDao.getBooksFlow().produceIn(this)
        assertThat(channel.receive()).isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
        assertThat(channel.isEmpty).isTrue()

        channel.cancel()
    }

    @Test
    fun receiveBooks_update() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val channel = booksDao.getBooksFlow().produceIn(this)

        assertThat(channel.receive()).isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))

        booksDao.insertBookSuspend(TestUtil.BOOK_3)
        drain() // drain async invalidate
        yield()

        assertThat(channel.receive())
            .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2, TestUtil.BOOK_3))
        assertThat(channel.isEmpty).isTrue()

        channel.cancel()
    }

    @Test
    fun receiveBooks_update_viaSupportDatabase() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        val channel = booksDao.getBooksFlow().produceIn(this)

        assertThat(channel.receive()).containsExactly(TestUtil.BOOK_1)

        // Update table without going through Room's transaction APIs
        database.openHelper.writableDatabase.execSQL(
            "UPDATE Book SET salesCnt = 5 WHERE bookId = 'b1'"
        )
        // Ask for a refresh to occur, validating trigger is installed without going through Room's
        // transaction APIs.
        database.invalidationTracker.refreshVersionsAsync()
        drain() // drain async invalidate
        yield()

        assertThat(channel.receive()).containsExactly(TestUtil.BOOK_1.copy(salesCnt = 5))
        assertThat(channel.isEmpty).isTrue()

        channel.cancel()
    }

    @Test
    fun receiveBooks_update_multipleChannels() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val channels = Array(4) { booksDao.getBooksFlow().produceIn(this) }

        channels.forEach {
            assertThat(it.receive()).isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
        }

        booksDao.insertBookSuspend(TestUtil.BOOK_3)
        drain() // drain async invalidate
        yield()

        channels.forEach {
            assertThat(it.receive())
                .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2, TestUtil.BOOK_3))
            assertThat(it.isEmpty).isTrue()
            it.cancel()
        }
    }

    @Test
    fun receiveBooks_update_multipleChannels_inTransaction() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val channels = Array(4) { booksDao.getBooksFlowInTransaction().produceIn(this) }

        channels.forEach {
            assertThat(it.receive()).isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
        }

        booksDao.insertBookSuspend(TestUtil.BOOK_3)
        drain() // drain async invalidate
        yield()

        channels.forEach {
            assertThat(it.receive())
                .isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2, TestUtil.BOOK_3))
            assertThat(it.isEmpty).isTrue()
            it.cancel()
        }
    }

    @Ignore("Due to b/365506854.")
    @Test
    fun receiveBooks_latestUpdateOnly() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val channel = booksDao.getBooksFlow().buffer(Channel.CONFLATED).produceIn(this)

        assertThat(channel.receive()).isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))

        booksDao.insertBookSuspend(TestUtil.BOOK_3)
        drain() // drain async invalidate
        yield()
        booksDao.deleteBookSuspend(TestUtil.BOOK_1)
        drain() // drain async invalidate
        yield()

        assertThat(channel.receive()).isEqualTo(listOf(TestUtil.BOOK_2, TestUtil.BOOK_3))
        assertThat(channel.isEmpty).isTrue()

        channel.cancel()
    }

    @Test
    fun receiveBooks_async() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val latch = CountDownLatch(1)
        val job =
            async(Dispatchers.IO) {
                for (result in booksDao.getBooksFlow().produceIn(this)) {
                    assertThat(result).isEqualTo(listOf(TestUtil.BOOK_1, TestUtil.BOOK_2))
                    latch.countDown()
                }
            }

        latch.await()
        job.cancelAndJoin()
    }

    @Test
    fun receiveBook_async_update_null() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        val firstResultLatch = CountDownLatch(1)
        val secondResultLatch = CountDownLatch(1)
        val results = mutableListOf<Book?>()
        val job =
            async(Dispatchers.IO) {
                booksDao.getOneBooksFlow(TestUtil.BOOK_1.bookId).collect {
                    when (results.size) {
                        0 -> {
                            results.add(it)
                            firstResultLatch.countDown()
                        }
                        1 -> {
                            results.add(it)
                            secondResultLatch.countDown()
                        }
                        else -> fail("Should have only collected 2 results.")
                    }
                }
            }

        firstResultLatch.await()
        booksDao.deleteBookSuspend(TestUtil.BOOK_1)

        secondResultLatch.await()
        assertThat(results.size).isEqualTo(2)
        assertThat(results[0]).isEqualTo(TestUtil.BOOK_1)
        assertThat(results[1]).isNull()

        job.cancelAndJoin()
    }

    @Test
    fun playlistSongs_async_update(): Unit = runBlocking {
        val musicDao = database.musicDao()
        val song1 = Song(1, "Thriller", "Michael Jackson", "Thriller", 357, 1982)
        val song2 = Song(2, "Billie Jean", "Michael Jackson", "Thriller", 297, 1982)
        musicDao.addSongs(song1, song2)
        musicDao.addPlaylists(Playlist(1), Playlist(2))
        musicDao.addPlaylistSongRelations(PlaylistSongXRef(1, 1))

        val latches = Array(3) { CountDownLatch(1) }
        val results = mutableListOf<PlaylistWithSongs>()
        var collectCall = 0
        val job =
            async(Dispatchers.IO) {
                musicDao.getPlaylistsWithSongsFlow(1).collect {
                    if (collectCall >= latches.size) {
                        fail("Should have only collected 3 results.")
                    }
                    results.add(it)
                    latches[collectCall].countDown()
                    collectCall++
                }
            }

        latches[0].await()
        assertThat(results.size).isEqualTo(1)
        results[0].let { playlist ->
            assertThat(playlist.songs.size).isEqualTo(1)
            assertThat(playlist.songs[0]).isEqualTo(song1)
        }

        musicDao.addPlaylistSongRelations(PlaylistSongXRef(1, 2))

        latches[1].await()
        assertThat(results.size).isEqualTo(2)
        results[1].let { playlist ->
            assertThat(playlist.songs.size).isEqualTo(2)
            assertThat(playlist.songs[0]).isEqualTo(song1)
            assertThat(playlist.songs[1]).isEqualTo(song2)
        }

        musicDao.removePlaylistSongRelations(PlaylistSongXRef(1, 2))

        latches[2].await()
        assertThat(results.size).isEqualTo(3)
        results[2].let { playlist ->
            assertThat(playlist.songs.size).isEqualTo(1)
            assertThat(playlist.songs[0]).isEqualTo(song1)
        }

        job.cancelAndJoin()
    }
}
