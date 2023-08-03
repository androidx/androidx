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

@file:Suppress("DEPRECATION") // For @MapInfo

package androidx.room.integration.kotlintestapp.dao

import androidx.collection.ArrayMap
import androidx.collection.LongSparseArray
import androidx.collection.SparseArrayCompat
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.MapInfo
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.integration.kotlintestapp.vo.Album
import androidx.room.integration.kotlintestapp.vo.AlbumNameAndBandName
import androidx.room.integration.kotlintestapp.vo.AlbumWithSongs
import androidx.room.integration.kotlintestapp.vo.Artist
import androidx.room.integration.kotlintestapp.vo.Image
import androidx.room.integration.kotlintestapp.vo.ImageFormat
import androidx.room.integration.kotlintestapp.vo.Playlist
import androidx.room.integration.kotlintestapp.vo.PlaylistSongXRef
import androidx.room.integration.kotlintestapp.vo.PlaylistWithSongs
import androidx.room.integration.kotlintestapp.vo.ReleasedAlbum
import androidx.room.integration.kotlintestapp.vo.Song
import androidx.sqlite.db.SupportSQLiteQuery
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSetMultimap
import io.reactivex.Flowable
import java.nio.ByteBuffer
import java.util.Date
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    @Insert
    fun addSongs(vararg songs: Song)

    @Insert
    fun addArtists(vararg artists: Artist)

    @Insert
    fun addAlbums(vararg albums: Album)

    @Insert
    fun addPlaylists(vararg playlists: Playlist)

    @Insert
    fun addPlaylistSongRelations(vararg relations: PlaylistSongXRef)

    @Delete
    fun removePlaylistSongRelations(vararg relations: PlaylistSongXRef)

    @Insert
    fun addImages(vararg images: Image)

    /* Map of Object to Object */
    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    fun getArtistAndFirstSongMap(): Map<Artist, Song>

    @Query("SELECT * FROM Song JOIN Artist ON Song.mArtist = Artist.mArtistName")
    fun getSongAndArtist(): Map<Song, Artist>

    @Query("SELECT * FROM Artist JOIN Album ON Artist.mArtistName = Album.mAlbumArtist")
    @Transaction
    fun getAllArtistAndTheirAlbumsWithSongs(): Map<Artist, AlbumWithSongs>

    @RawQuery
    fun getAllArtistAndTheirSongsRawQuery(query: SupportSQLiteQuery): Map<Artist, Song>

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    fun getAllArtistAndTheirSongsAsLiveData(): LiveData<Map<Artist, Song>>

    /* Map of Object to List */
    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    fun getAllArtistAndTheirSongsList(): Map<Artist, List<Song>>

    @Query(
        "SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist " +
            "ORDER BY mArtistName ASC"
    )
    fun getAllArtistAndTheirSongsListOrdered(): Map<Artist, List<Song>>

    @Query("SELECT * FROM Artist JOIN Album ON Artist.mArtistName = Album.mAlbumArtist")
    @Transaction
    fun getAllArtistAndTheirAlbumsWithSongsList(): Map<Artist, List<AlbumWithSongs>>

    @RawQuery
    fun getAllArtistAndTheirSongsRawQueryList(query: SupportSQLiteQuery): Map<Artist, List<Song>>

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    fun getAllArtistAndTheirSongsAsLiveDataList(): LiveData<Map<Artist, List<Song>>>

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    fun getAllArtistAndTheirSongsAsFlowableList(): Flowable<Map<Artist, List<Song>>>

    @Query(
        "SELECT Album.mAlbumReleaseYear as mReleaseYear, Album.mAlbumName, Album.mAlbumArtist " +
            "as mBandName" +
            " from Album " +
            "JOIN Song " +
            "ON Album.mAlbumArtist = Song.mArtist AND Album.mAlbumName = Song.mAlbum"
    )
    fun getReleaseYearToAlbumsAndBandsList(): Map<ReleasedAlbum, List<AlbumNameAndBandName>>

    /* Map of Object to Set */
    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    fun getAllArtistAndTheirSongsSet(): Map<Artist, Set<Song>>

    @RawQuery
    fun getAllArtistAndTheirSongsRawQuerySet(query: SupportSQLiteQuery): Map<Artist, Set<Song>>

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    fun allArtistAndTheirSongsAsLiveDataSet(): LiveData<Map<Artist, Set<Song>>>

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    fun allArtistAndTheirSongsAsFlowableSet(): Flowable<Map<Artist, Set<Song>>>

    /* Guava ImmutableMap */
    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    fun allArtistAndTheirSongsImmutableMap(): ImmutableMap<Artist, List<Song>>

    @RawQuery
    fun getAllArtistAndTheirSongsRawQueryImmutableMap(
        query: SupportSQLiteQuery
    ): ImmutableMap<Artist, List<Song>>

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    fun allArtistAndTheirSongsAsLiveDataImmutableMap(): LiveData<ImmutableMap<Artist, Set<Song>>>

    /* Guava Multimap */
    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    fun allArtistAndTheirSongsGuavaImmutableSetMultimap(): ImmutableSetMultimap<Artist, Song>

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    fun allArtistAndTheirSongsGuavaImmutableListMultimap(): ImmutableListMultimap<Artist, Song>

    @Query("SELECT * FROM Artist JOIN Album ON Artist.mArtistName = Album.mAlbumArtist")
    @Transaction
    fun allArtistAndTheirAlbumsWithSongsGuavaImmutableSetMultimap():
        ImmutableSetMultimap<Artist, AlbumWithSongs>

    @Query("SELECT * FROM Artist JOIN Album ON Artist.mArtistName = Album.mAlbumArtist")
    @Transaction
    fun allArtistAndTheirAlbumsWithSongsGuavaImmutableListMultimap():
        ImmutableListMultimap<Artist, AlbumWithSongs>

    @RawQuery
    fun getAllArtistAndTheirSongsRawQueryGuavaImmutableSetMultimap(
        query: SupportSQLiteQuery
    ): ImmutableSetMultimap<Artist, Song>

    @RawQuery
    fun getAllArtistAndTheirSongsRawQueryGuavaImmutableListMultimap(
        query: SupportSQLiteQuery
    ): ImmutableListMultimap<Artist, Song>

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    fun allArtistAndTheirSongsAsLiveDataGuavaImmutableSetMultimap():
        LiveData<ImmutableSetMultimap<Artist, Song>>

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    fun allArtistAndTheirSongsAsLiveDataGuavaImmutableListMultimap():
        LiveData<ImmutableListMultimap<Artist, Song>>

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    @MapInfo(keyColumn = "mArtistName")
    @RewriteQueriesToDropUnusedColumns
    fun artistNameToSongs(): Map<String, List<Song>>

    @Query("SELECT * FROM Album JOIN Song ON Song.mReleasedYear = Album.mAlbumReleaseYear")
    @MapInfo(keyColumn = "mReleasedYear", valueColumn = "mReleasedYear")
    @RewriteQueriesToDropUnusedColumns
    fun releaseYearToAlbums(): Map<Int, List<Song>>

    @Query("SELECT * FROM Album JOIN Song ON Song.mReleasedYear = Album.mAlbumReleaseYear")
    @MapInfo(keyColumn = "mReleasedYear", valueColumn = "mTitle")
    @RewriteQueriesToDropUnusedColumns
    fun releaseYearToSongNames(): Map<Int, List<String>>

    @RewriteQueriesToDropUnusedColumns
    @MapInfo(keyColumn = "mArtistName", valueColumn = "mArtist")
    @RawQuery
    fun getArtistNameToSongsRawQuery(query: SupportSQLiteQuery): Map<String, List<Song>>

    @RewriteQueriesToDropUnusedColumns
    @MapInfo(keyColumn = "mReleasedYear", valueColumn = "mReleasedYear")
    @RawQuery
    fun getReleaseYearToAlbumsRawQuery(query: SupportSQLiteQuery): Map<Int, List<Song>>

    @RewriteQueriesToDropUnusedColumns
    @MapInfo(keyColumn = "mReleasedYear", valueColumn = "mTitle")
    @RawQuery
    fun getReleaseYearToSongNamesRawQuery(query: SupportSQLiteQuery): Map<Int, List<String>>

    @Query(
        "SELECT *, COUNT(mSongId) as songCount FROM Artist JOIN Song ON Artist.mArtistName = " +
            "Song.mArtist GROUP BY mArtistName"
    )
    @MapInfo(valueColumn = "songCount")
    @RewriteQueriesToDropUnusedColumns
    fun artistAndSongCountMap(): Map<Artist, Int>

    @RewriteQueriesToDropUnusedColumns
    @MapInfo(valueColumn = "songCount")
    @RawQuery
    fun getArtistAndSongCountMapRawQuery(query: SupportSQLiteQuery): Map<Artist, Int>

    // Other Map Key/Value Types
    @Query("SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image.mArtistInImage")
    @MapInfo(valueColumn = "mAlbumCover")
    @RewriteQueriesToDropUnusedColumns
    fun allArtistsWithAlbumCovers(): ImmutableMap<Artist, ByteBuffer>

    @MapInfo(valueColumn = "mAlbumCover")
    @RawQuery
    fun getAllArtistsWithAlbumCoversRawQuery(query: SupportSQLiteQuery):
        ImmutableMap<Artist, ByteBuffer>

    @Query("SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image.mArtistInImage")
    @MapInfo(valueColumn = "mImageYear")
    @RewriteQueriesToDropUnusedColumns
    fun allArtistsWithAlbumCoverYear(): ImmutableMap<Artist, Long>

    @MapInfo(keyColumn = "mImageYear")
    @RawQuery
    fun getAllAlbumCoverYearToArtistsWithRawQuery(query: SupportSQLiteQuery):
        ImmutableMap<Long, Artist>

    @Query("SELECT * FROM Image JOIN Artist ON Artist.mArtistName = Image.mArtistInImage")
    @MapInfo(keyColumn = "mAlbumCover", valueColumn = "mIsActive")
    fun albumCoversWithBandActivity(): ImmutableMap<ByteBuffer, Boolean>

    @MapInfo(keyColumn = "mAlbumCover", valueColumn = "mIsActive")
    @RawQuery
    fun getAlbumCoversWithBandActivityRawQuery(
        query: SupportSQLiteQuery
    ): ImmutableMap<ByteBuffer, Boolean>

    @Query("SELECT * FROM Image JOIN Artist ON Artist.mArtistName = Image.mArtistInImage")
    @MapInfo(keyColumn = "mDateReleased", valueColumn = "mIsActive")
    fun albumDateWithBandActivity(): ImmutableMap<Date, Boolean>

    @MapInfo(keyColumn = "mDateReleased", valueColumn = "mIsActive")
    @RawQuery
    fun getAlbumDateWithBandActivityRawQuery(query: SupportSQLiteQuery): ImmutableMap<Date, Boolean>

    @Query("SELECT * FROM Image JOIN Artist ON Artist.mArtistName = Image.mArtistInImage")
    @MapInfo(keyColumn = "mFormat", valueColumn = "mIsActive")
    fun imageFormatWithBandActivity(): ImmutableMap<ImageFormat, Boolean>

    @MapInfo(keyColumn = "mFormat", valueColumn = "mIsActive")
    @RawQuery
    fun getImageFormatWithBandActivityRawQuery(
        query: SupportSQLiteQuery
    ): ImmutableMap<ImageFormat, Boolean>

    @MapInfo(keyColumn = "dog", valueColumn = "cat")
    @RawQuery
    fun getMapWithInvalidColumnRawQuery(query: SupportSQLiteQuery): Map<Artist, Int>

    @Query("SELECT * FROM Artist LEFT JOIN Album ON Artist.mArtistName = Album.mAlbumArtist")
    fun artistAndAlbumsLeftJoin(): Map<Artist, List<Album>>

    @Query("SELECT * FROM Artist LEFT JOIN Album ON Artist.mArtistName = Album.mAlbumArtist")
    fun artistAndAlbumsLeftJoinGuava(): ImmutableListMultimap<Artist, Album>

    @Query("SELECT * FROM Artist LEFT JOIN Album ON Artist.mArtistName = Album.mAlbumArtist")
    @MapInfo(valueColumn = "mAlbumName")
    @RewriteQueriesToDropUnusedColumns
    fun artistAndAlbumNamesLeftJoin(): Map<Artist, List<String>>

    @Query("SELECT * FROM Album LEFT JOIN Artist ON Artist.mArtistName = Album.mAlbumArtist")
    fun albumToArtistLeftJoin(): Map<Album, Artist>

    @Query("SELECT * FROM Album LEFT JOIN Artist ON Artist.mArtistName = Album.mAlbumArtist")
    fun artistToAlbumLeftJoin(): Map<Artist, Album>

    @Query("SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image.mArtistInImage")
    @MapInfo(valueColumn = "mImageYear")
    @RewriteQueriesToDropUnusedColumns
    fun allArtistsWithAlbumCoverYearArrayMap(): ArrayMap<Artist, Long>

    @MapInfo(keyColumn = "mImageYear")
    @RawQuery
    fun getAllAlbumCoverYearToArtistsWithRawQueryArrayMap(
        query: SupportSQLiteQuery
    ): ArrayMap<Long, Artist>

    @Query("SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image.mArtistInImage")
    @MapInfo(keyColumn = "mImageYear")
    @RewriteQueriesToDropUnusedColumns
    fun allAlbumCoverYearToArtistsWithLongSparseArray(): LongSparseArray<Artist>

    @Query("SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image.mArtistInImage")
    @MapInfo(keyColumn = "mImageYear")
    @RewriteQueriesToDropUnusedColumns
    fun allAlbumCoverYearToArtistsWithIntSparseArray(): SparseArrayCompat<Artist>

    @Query(
        """
        SELECT * FROM Artist
        JOIN Album ON (Artist.mArtistName = Album.mAlbumArtist)
        JOIN Song ON (Album.mAlbumName = Song.mAlbum)
        """
    )
    @RewriteQueriesToDropUnusedColumns
    fun getArtistToAlbumsMappedToSongs(): Map<Artist, Map<Album, List<Song>>>

    @Query(
        """
        SELECT * FROM Image
        JOIN Artist ON Image.mArtistInImage = Artist.mArtistName
        JOIN Album ON Artist.mArtistName = Album.mAlbumArtist
        JOIN Song ON Album.mAlbumName = Song.mAlbum
        """
    )
    @RewriteQueriesToDropUnusedColumns
    fun getImageToArtistToAlbumsMappedToSongs():
        Map<Image, Map<Artist, Map<Album, List<Song>>>>

    @Query(
        """
        SELECT * FROM Artist
        LEFT JOIN Album ON (Artist.mArtistName = Album.mAlbumArtist)
        LEFT JOIN Song ON (Album.mAlbumName = Song.mAlbum)
        """
    )
    @MapInfo(valueColumn = "mTitle")
    @RewriteQueriesToDropUnusedColumns
    fun getArtistToAlbumsMappedToSongNamesMapInfoLeftJoin(): Map<Artist, Map<Album, String>>

    @Query(
        """
        SELECT * FROM Image
        LEFT JOIN Artist ON Image.mArtistInImage = Artist.mArtistName
        LEFT JOIN Album ON Artist.mArtistName = Album.mAlbumArtist
        LEFT JOIN Song ON Album.mAlbumName = Song.mAlbum
        """
    )
    @MapInfo(keyColumn = "mImageYear")
    @RewriteQueriesToDropUnusedColumns
    fun getImageYearToArtistToAlbumsMappedToSongs(): Map<Long, Map<Artist, Map<Album, List<Song>>>>

    @Query(
        """
        SELECT * FROM Image
        LEFT JOIN Artist ON Image.mArtistInImage = Artist.mArtistName
        LEFT JOIN Album ON Artist.mArtistName = Album.mAlbumArtist
        LEFT JOIN Song ON Album.mAlbumName = Song.mAlbum
        """
    )
    @MapInfo(keyColumn = "mImageYear", valueColumn = "mTitle")
    @RewriteQueriesToDropUnusedColumns
    fun getNestedMapWithMapInfoKeyAndValue(): Map<Long, Map<Artist, Map<Album, List<String>>>>

    @Transaction
    @Query("SELECT * FROM Playlist WHERE mPlaylistId = :id")
    fun getPlaylistsWithSongsFlow(id: Int): Flow<PlaylistWithSongs>

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    @RewriteQueriesToDropUnusedColumns
    fun artistNameToSongsMapColumn():
        Map<@MapColumn(columnName = "mArtistName") String,
            List<@MapColumn(columnName = "mReleasedYear") Int>>

    @Query(
        """
        SELECT * FROM Image
        LEFT JOIN Artist ON Image.mArtistInImage = Artist.mArtistName
        LEFT JOIN Album ON Artist.mArtistName = Album.mAlbumArtist
        LEFT JOIN Song ON Album.mAlbumName = Song.mAlbum
        """
    )
    @RewriteQueriesToDropUnusedColumns
    fun getImageYearToArtistToAlbumsToSongsMapColumn():
        Map<@MapColumn(columnName = "mImageYear") Long, Map<Artist,
            Map<@MapColumn(columnName = "mAlbumName") String, List<Song>>>>

    @Query(
        """
        SELECT * FROM Image
        LEFT JOIN Artist ON Image.mArtistInImage = Artist.mArtistName
        LEFT JOIN Album ON Artist.mArtistName = Album.mAlbumArtist
        LEFT JOIN Song ON Album.mAlbumName = Song.mAlbum
        """
    )
    @RewriteQueriesToDropUnusedColumns
    fun getImageYearToArtistToAlbumsToSongsMultiMapColumn():
        Map<Image, Map<Artist, Map<@MapColumn(columnName = "mAlbumName") String,
            List<@MapColumn(columnName = "mReleasedYear") Int>>>>

    @RawQuery
    @RewriteQueriesToDropUnusedColumns
    fun getImageYearToArtistToAlbumsToSongsMultiMapColumn(query: SupportSQLiteQuery):
        Map<Image, Map<Artist, Map<@MapColumn(columnName = "mAlbumName") String,
            List<@MapColumn(columnName = "mReleasedYear") Int>>>>
}
