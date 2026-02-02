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

package androidx.room3.integration.kotlintestapp.dao

import androidx.collection.ArrayMap
import androidx.collection.LongSparseArray
import androidx.collection.SparseArrayCompat
import androidx.lifecycle.LiveData
import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.MapColumn
import androidx.room3.Query
import androidx.room3.RawQuery
import androidx.room3.RewriteQueriesToDropUnusedColumns
import androidx.room3.RoomRawQuery
import androidx.room3.Transaction
import androidx.room3.Update
import androidx.room3.integration.kotlintestapp.vo.Album
import androidx.room3.integration.kotlintestapp.vo.AlbumNameAndBandName
import androidx.room3.integration.kotlintestapp.vo.AlbumWithSongs
import androidx.room3.integration.kotlintestapp.vo.Artist
import androidx.room3.integration.kotlintestapp.vo.Image
import androidx.room3.integration.kotlintestapp.vo.ImageFormat
import androidx.room3.integration.kotlintestapp.vo.Playlist
import androidx.room3.integration.kotlintestapp.vo.PlaylistSongXRef
import androidx.room3.integration.kotlintestapp.vo.PlaylistWithSongs
import androidx.room3.integration.kotlintestapp.vo.ReleasedAlbum
import androidx.room3.integration.kotlintestapp.vo.Song
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSetMultimap
import io.reactivex.rxjava3.core.Flowable
import java.nio.ByteBuffer
import java.util.Date
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    @Insert fun addSongs(vararg songs: Song)

    @Update fun updateSong(song: Song)

    @Insert fun addArtists(vararg artists: Artist)

    @Insert fun addAlbums(vararg albums: Album)

    @Insert fun addPlaylists(vararg playlists: Playlist)

    @Insert fun addPlaylistSongRelations(vararg relations: PlaylistSongXRef)

    @Delete fun removePlaylistSongRelations(vararg relations: PlaylistSongXRef)

    @Insert fun addImages(vararg images: Image)

    /* Map of Object to Object */
    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    fun getArtistAndFirstSongMap(): Map<Artist, Song>

    @Query("SELECT * FROM Song JOIN Artist ON Song.mArtist = Artist.mArtistName")
    fun getSongAndArtist(): Map<Song, Artist>

    @Query("SELECT * FROM Artist JOIN Album ON Artist.mArtistName = Album.mAlbumArtist")
    @Transaction
    fun getAllArtistAndTheirAlbumsWithSongs(): Map<Artist, AlbumWithSongs>

    @RawQuery fun getAllArtistAndTheirSongsRawQuery(query: RoomRawQuery): Map<Artist, Song>

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
    fun getAllArtistAndTheirSongsRawQueryList(query: RoomRawQuery): Map<Artist, List<Song>>

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

    @RawQuery fun getAllArtistAndTheirSongsRawQuerySet(query: RoomRawQuery): Map<Artist, Set<Song>>

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    fun allArtistAndTheirSongsAsLiveDataSet(): LiveData<Map<Artist, Set<Song>>>

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    fun allArtistAndTheirSongsAsFlowableSet(): Flowable<Map<Artist, Set<Song>>>

    /* Guava ImmutableMap */
    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    fun allArtistAndTheirSongsImmutableMap(): ImmutableMap<Artist, List<Song>>

    @RawQuery
    fun getAllArtistAndTheirSongsRawQueryImmutableMap(
        query: RoomRawQuery
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
        query: RoomRawQuery
    ): ImmutableSetMultimap<Artist, Song>

    @RawQuery
    fun getAllArtistAndTheirSongsRawQueryGuavaImmutableListMultimap(
        query: RoomRawQuery
    ): ImmutableListMultimap<Artist, Song>

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    fun allArtistAndTheirSongsAsLiveDataGuavaImmutableSetMultimap():
        LiveData<ImmutableSetMultimap<Artist, Song>>

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    fun allArtistAndTheirSongsAsLiveDataGuavaImmutableListMultimap():
        LiveData<ImmutableListMultimap<Artist, Song>>

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    @RewriteQueriesToDropUnusedColumns
    fun artistNameToSongs(): Map<@MapColumn("mArtistName") String, List<Song>>

    @Query("SELECT * FROM Album JOIN Song ON Song.mReleasedYear = Album.mAlbumReleaseYear")
    @RewriteQueriesToDropUnusedColumns
    fun releaseYearToAlbums(): Map<@MapColumn("mReleasedYear") Int, List<Song>>

    @Query("SELECT * FROM Album JOIN Song ON Song.mReleasedYear = Album.mAlbumReleaseYear")
    @RewriteQueriesToDropUnusedColumns
    fun releaseYearToSongNames():
        Map<@MapColumn("mReleasedYear") Int, List<@MapColumn("mTitle") String>>

    @RewriteQueriesToDropUnusedColumns
    @RawQuery
    fun getArtistNameToSongsRawQuery(
        query: RoomRawQuery
    ): Map<@MapColumn("mArtistName") String, List<Song>>

    @RewriteQueriesToDropUnusedColumns
    @RawQuery
    fun getReleaseYearToAlbumsRawQuery(
        query: RoomRawQuery
    ): Map<@MapColumn("mReleasedYear") Int, List<Song>>

    @RewriteQueriesToDropUnusedColumns
    @RawQuery
    fun getReleaseYearToSongNamesRawQuery(
        query: RoomRawQuery
    ): Map<@MapColumn("mReleasedYear") Int, List<@MapColumn("mTitle") String>>

    @Query(
        "SELECT *, COUNT(mSongId) as songCount FROM Artist JOIN Song ON Artist.mArtistName = " +
            "Song.mArtist GROUP BY mArtistName"
    )
    @RewriteQueriesToDropUnusedColumns
    fun artistAndSongCountMap(): Map<Artist, @MapColumn("songCount") Int>

    @RewriteQueriesToDropUnusedColumns
    @RawQuery
    fun getArtistAndSongCountMapRawQuery(
        query: RoomRawQuery
    ): Map<Artist, @MapColumn("songCount") Int>

    // Other Map Key/Value Types
    @Query("SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image.mArtistInImage")
    @RewriteQueriesToDropUnusedColumns
    fun allArtistsWithAlbumCovers(): ImmutableMap<Artist, @MapColumn("mAlbumCover") ByteBuffer>

    @RawQuery
    fun getAllArtistsWithAlbumCoversRawQuery(
        query: RoomRawQuery
    ): ImmutableMap<Artist, @MapColumn("mAlbumCover") ByteBuffer>

    @Query("SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image.mArtistInImage")
    @RewriteQueriesToDropUnusedColumns
    fun allArtistsWithAlbumCoverYear(): ImmutableMap<Artist, @MapColumn("mImageYear") Long>

    @RawQuery
    fun getAllAlbumCoverYearToArtistsWithRawQuery(
        query: RoomRawQuery
    ): ImmutableMap<@MapColumn("mImageYear") Long, Artist>

    @Query("SELECT * FROM Image JOIN Artist ON Artist.mArtistName = Image.mArtistInImage")
    fun albumCoversWithBandActivity():
        ImmutableMap<@MapColumn("mAlbumCover") ByteBuffer, @MapColumn("mIsActive") Boolean>

    @RawQuery
    fun getAlbumCoversWithBandActivityRawQuery(
        query: RoomRawQuery
    ): ImmutableMap<@MapColumn("mAlbumCover") ByteBuffer, @MapColumn("mIsActive") Boolean>

    @Query("SELECT * FROM Image JOIN Artist ON Artist.mArtistName = Image.mArtistInImage")
    fun albumDateWithBandActivity():
        ImmutableMap<@MapColumn("mDateReleased") Date, @MapColumn("mIsActive") Boolean>

    @RawQuery
    fun getAlbumDateWithBandActivityRawQuery(
        query: RoomRawQuery
    ): ImmutableMap<@MapColumn("mDateReleased") Date, @MapColumn("mIsActive") Boolean>

    @Query("SELECT * FROM Image JOIN Artist ON Artist.mArtistName = Image.mArtistInImage")
    fun imageFormatWithBandActivity():
        ImmutableMap<@MapColumn("mFormat") ImageFormat, @MapColumn("mIsActive") Boolean>

    @RawQuery
    fun getImageFormatWithBandActivityRawQuery(
        query: RoomRawQuery
    ): ImmutableMap<@MapColumn("mFormat") ImageFormat, @MapColumn("mIsActive") Boolean>

    @RawQuery
    fun getMapWithInvalidColumnRawQuery(
        query: RoomRawQuery
    ): Map<@MapColumn("dog") Artist, @MapColumn("cat") Int>

    @Query("SELECT * FROM Artist LEFT JOIN Album ON Artist.mArtistName = Album.mAlbumArtist")
    fun artistAndAlbumsLeftJoin(): Map<Artist, List<Album>>

    @Query("SELECT * FROM Artist LEFT JOIN Album ON Artist.mArtistName = Album.mAlbumArtist")
    fun artistAndAlbumsLeftJoinGuava(): ImmutableListMultimap<Artist, Album>

    @Query("SELECT * FROM Artist LEFT JOIN Album ON Artist.mArtistName = Album.mAlbumArtist")
    @RewriteQueriesToDropUnusedColumns
    fun artistAndAlbumNamesLeftJoin(): Map<Artist, List<@MapColumn("mAlbumName") String>>

    @Query("SELECT * FROM Album LEFT JOIN Artist ON Artist.mArtistName = Album.mAlbumArtist")
    fun albumToArtistLeftJoin(): Map<Album, Artist>

    @Query("SELECT * FROM Album LEFT JOIN Artist ON Artist.mArtistName = Album.mAlbumArtist")
    fun artistToAlbumLeftJoin(): Map<Artist, Album>

    @Query("SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image.mArtistInImage")
    @RewriteQueriesToDropUnusedColumns
    fun allArtistsWithAlbumCoverYearArrayMap(): ArrayMap<Artist, @MapColumn("mImageYear") Long>

    @RawQuery
    fun getAllAlbumCoverYearToArtistsWithRawQueryArrayMap(
        query: RoomRawQuery
    ): ArrayMap<@MapColumn("mImageYear") Long, Artist>

    @Query("SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image.mArtistInImage")
    @RewriteQueriesToDropUnusedColumns
    fun allAlbumCoverYearToArtistsWithLongSparseArray():
        @MapColumn("mImageYear") LongSparseArray<Artist>

    @Query("SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image.mArtistInImage")
    @RewriteQueriesToDropUnusedColumns
    fun allAlbumCoverYearToArtistsWithIntSparseArray():
        @MapColumn("mImageYear") SparseArrayCompat<Artist>

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
    fun getImageToArtistToAlbumsMappedToSongs(): Map<Image, Map<Artist, Map<Album, List<Song>>>>

    @Query(
        """
        SELECT * FROM Artist
        LEFT JOIN Album ON (Artist.mArtistName = Album.mAlbumArtist)
        LEFT JOIN Song ON (Album.mAlbumName = Song.mAlbum)
        """
    )
    @RewriteQueriesToDropUnusedColumns
    fun getArtistToAlbumsMappedToSongNamesMapColumnLeftJoin():
        Map<Artist, Map<Album, @MapColumn("mTitle") String>>

    @Query(
        """
        SELECT * FROM Image
        LEFT JOIN Artist ON Image.mArtistInImage = Artist.mArtistName
        LEFT JOIN Album ON Artist.mArtistName = Album.mAlbumArtist
        LEFT JOIN Song ON Album.mAlbumName = Song.mAlbum
        """
    )
    @RewriteQueriesToDropUnusedColumns
    fun getImageYearToArtistToAlbumsMappedToSongs():
        Map<@MapColumn("mImageYear") Long, Map<Artist, Map<Album, List<Song>>>>

    @Query(
        """
        SELECT * FROM Image
        LEFT JOIN Artist ON Image.mArtistInImage = Artist.mArtistName
        LEFT JOIN Album ON Artist.mArtistName = Album.mAlbumArtist
        LEFT JOIN Song ON Album.mAlbumName = Song.mAlbum
        """
    )
    @RewriteQueriesToDropUnusedColumns
    fun getNestedMapWithMapColumnKeyAndValue():
        Map<
            @MapColumn("mImageYear")
            Long,
            Map<Artist, Map<Album, List<@MapColumn("mTitle") String>>>,
        >

    @Transaction
    @Query("SELECT * FROM Playlist WHERE mPlaylistId = :id")
    fun getPlaylistsWithSongsFlow(id: Int): Flow<PlaylistWithSongs>

    @Transaction
    @Query("SELECT * FROM Album WHERE mAlbumId = :id")
    fun getAlbumWithSongsFlow(id: Int): Flow<AlbumWithSongs>

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    @RewriteQueriesToDropUnusedColumns
    fun artistNameToSongsMapColumn():
        Map<@MapColumn("mArtistName") String, List<@MapColumn("mReleasedYear") Int>>

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
        Map<
            @MapColumn("mImageYear")
            Long,
            Map<Artist, Map<@MapColumn("mAlbumName") String, List<Song>>>,
        >

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
        Map<
            Image,
            Map<Artist, Map<@MapColumn("mAlbumName") String, List<@MapColumn("mReleasedYear") Int>>>,
        >

    @RawQuery
    @RewriteQueriesToDropUnusedColumns
    fun getImageYearToArtistToAlbumsToSongsMultiMapColumn(
        query: RoomRawQuery
    ): Map<
        Image,
        Map<Artist, Map<@MapColumn("mAlbumName") String, List<@MapColumn("mReleasedYear") Int>>>,
    >
}
