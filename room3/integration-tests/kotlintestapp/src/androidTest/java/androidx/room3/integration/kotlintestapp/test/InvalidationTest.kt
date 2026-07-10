/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.room3.immediateTransaction
import androidx.room3.integration.kotlintestapp.vo.Album
import androidx.room3.integration.kotlintestapp.vo.Playlist
import androidx.room3.integration.kotlintestapp.vo.PlaylistSongXRef
import androidx.room3.integration.kotlintestapp.vo.Song
import androidx.room3.useWriterConnection
import androidx.test.filters.SmallTest
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@SmallTest
@RunWith(Parameterized::class)
class InvalidationTest(driver: UseDriver) :
    TestDatabaseTest(useDriver = driver, useInMemoryDatabase = false) {

    private companion object {
        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = arrayOf(UseDriver.ANDROID, UseDriver.BUNDLED)
    }

    @Test
    fun invalidationOnInsert() = runTest {
        val publishers = booksDao.getPublishersFlow().produceIn(this)
        assertThat(publishers.receive()).isEmpty()

        booksDao.addPublishers(TestUtil.PUBLISHER)
        assertThat(publishers.receive()).containsExactly(TestUtil.PUBLISHER)

        publishers.cancel()
    }

    @Test
    fun invalidationOnUpdate() = runTest {
        booksDao.addPublishers(TestUtil.PUBLISHER)

        val publishers = booksDao.getPublishersFlow().produceIn(this)
        assertThat(publishers.receive()).containsExactly(TestUtil.PUBLISHER)

        booksDao.updatePublishers(TestUtil.PUBLISHER.copy(name = "updatedName"))
        assertThat(publishers.receive().single().name).isEqualTo("updatedName")

        publishers.cancel()
    }

    @Test
    fun invalidationOnDelete() = runTest {
        booksDao.addPublishers(TestUtil.PUBLISHER)

        val publishers = booksDao.getPublishersFlow().produceIn(this)
        assertThat(publishers.receive()).containsExactly(TestUtil.PUBLISHER)

        booksDao.deletePublishers(TestUtil.PUBLISHER)
        assertThat(publishers.receive()).isEmpty()

        publishers.cancel()
    }

    @Test
    fun invalidationUsingWriteConnection() = runTest {
        val publishers = booksDao.getPublishersFlow().produceIn(this)
        assertThat(publishers.receive()).isEmpty()

        database.useWriterConnection { connection ->
            connection.usePrepared("INSERT INTO publisher (publisherId, name) VALUES (?, ?)") { stmt
                ->
                stmt.bindText(1, TestUtil.PUBLISHER.publisherId)
                stmt.bindText(2, TestUtil.PUBLISHER.name)
                stmt.step()
            }
        }
        assertThat(publishers.receive()).containsExactly(TestUtil.PUBLISHER)

        publishers.cancel()
    }

    @Test
    fun invalidationUsingWriteTransaction() = runTest {
        val publishers = booksDao.getPublishersFlow().produceIn(this)
        assertThat(publishers.receive()).isEmpty()

        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                connection.usePrepared("INSERT INTO publisher (publisherId, name) VALUES (?, ?)") {
                    stmt ->
                    stmt.reset()
                    stmt.bindText(1, TestUtil.PUBLISHER.publisherId)
                    stmt.bindText(2, TestUtil.PUBLISHER.name)
                    stmt.step()

                    stmt.reset()
                    stmt.bindText(1, TestUtil.PUBLISHER2.publisherId)
                    stmt.bindText(2, TestUtil.PUBLISHER2.name)
                    stmt.step()

                    stmt.reset()
                    stmt.bindText(1, TestUtil.PUBLISHER3.publisherId)
                    stmt.bindText(2, TestUtil.PUBLISHER3.name)
                    stmt.step()
                }
            }
        }
        assertThat(publishers.receive())
            .containsExactly(TestUtil.PUBLISHER, TestUtil.PUBLISHER2, TestUtil.PUBLISHER3)

        publishers.cancel()
    }

    @Test
    fun invalidationOfRelationOneToMany() = runTest {
        database
            .musicDao()
            .addAlbums(
                Album(
                    mAlbumId = 1,
                    mAlbumName = "DATA",
                    mAlbumArtist = "Tainy",
                    mAlbumReleaseYear = 2023,
                    mFeaturedArtist = null,
                )
            )
        database
            .musicDao()
            .addSongs(
                Song(
                    mSongId = 5,
                    mTitle = "Mojabi Ghost",
                    mArtist = "Tainy & Bad Bunny",
                    mAlbum = "DATA",
                    mLength = 233,
                    mReleasedYear = 2023,
                )
            )

        val album = database.musicDao().getAlbumWithSongsFlow(1).produceIn(this)
        assertThat(album.receive().songs.map { it.mTitle }).containsExactly("Mojabi Ghost")

        database
            .musicDao()
            .addSongs(
                Song(
                    mSongId = 6,
                    mTitle = "11 y Once",
                    mArtist = "Tainy, Sech & E.VAX",
                    mAlbum = "DATA",
                    mLength = 197,
                    mReleasedYear = 2023,
                )
            )
        assertThat(album.receive().songs.map { it.mTitle })
            .containsExactly("Mojabi Ghost", "11 y Once")

        album.cancel()
    }

    @Test
    fun invalidationOfRelationManyToMany() = runTest {
        database.musicDao().addPlaylists(Playlist(1))
        database
            .musicDao()
            .addSongs(
                Song(
                    mSongId = 787,
                    mTitle = "Tú Con Él",
                    mArtist = "Frankie Ruiz",
                    mAlbum = "Solista pero no solo",
                    mLength = 301,
                    mReleasedYear = 1985,
                )
            )
        database.musicDao().addPlaylistSongRelations(PlaylistSongXRef(1, 787))

        val playlist = database.musicDao().getPlaylistsWithSongsFlow(1).produceIn(this)
        assertThat(playlist.receive().songs.map { it.mArtist }).containsExactly("Frankie Ruiz")

        database
            .musicDao()
            .updateSong(
                Song(
                    mSongId = 787,
                    mTitle = "Tú Con Él",
                    mArtist = "Rauw Alejandro",
                    mAlbum = "Cosa Nuestra",
                    mLength = 290,
                    mReleasedYear = 2024,
                )
            )
        assertThat(playlist.receive().songs.map { it.mArtist }).containsExactly("Rauw Alejandro")

        playlist.cancel()
    }

    @Test
    fun invalidationOfRelationManyToManyJunction() = runTest {
        database.musicDao().addPlaylists(Playlist(1))
        database
            .musicDao()
            .addSongs(
                Song(
                    mSongId = 14,
                    mTitle = "Safaera",
                    mArtist = "Bad Bunny, Jowell & Randy & Ñengo Flow",
                    mAlbum = "YHLQMDLG",
                    mLength = 296,
                    mReleasedYear = 2020,
                ),
                Song(
                    mSongId = 15,
                    mTitle = "EoO",
                    mArtist = "Bad Bunny",
                    mAlbum = "DeBÍ TiRAR MáS FOToS",
                    mLength = 205,
                    mReleasedYear = 2025,
                ),
                Song(
                    mSongId = 10,
                    mTitle = "COLMILLO",
                    mArtist = "Tainy, J Balvin & Young Miko (feat. Jowell & Randy)",
                    mAlbum = "DATA",
                    mLength = 266,
                    mReleasedYear = 2023,
                ),
            )
        database.musicDao().addPlaylistSongRelations(PlaylistSongXRef(1, 14))
        database.musicDao().addPlaylistSongRelations(PlaylistSongXRef(1, 15))

        val playlist = database.musicDao().getPlaylistsWithSongsFlow(1).produceIn(this)
        assertThat(playlist.receive().songs.map { it.mTitle }).containsExactly("Safaera", "EoO")

        database.musicDao().addPlaylistSongRelations(PlaylistSongXRef(1, 10))
        assertThat(playlist.receive().songs.map { it.mTitle })
            .containsExactly("Safaera", "EoO", "COLMILLO")

        playlist.cancel()
    }
}
