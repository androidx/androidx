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

package androidx.room.integration.testapp.dao;

import androidx.collection.ArrayMap;
import androidx.collection.LongSparseArray;
import androidx.collection.SparseArrayCompat;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.MapInfo;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.RewriteQueriesToDropUnusedColumns;
import androidx.room.Transaction;
import androidx.room.integration.testapp.vo.Album;
import androidx.room.integration.testapp.vo.AlbumNameAndBandName;
import androidx.room.integration.testapp.vo.AlbumWithSongs;
import androidx.room.integration.testapp.vo.Artist;
import androidx.room.integration.testapp.vo.Image;
import androidx.room.integration.testapp.vo.ImageFormat;
import androidx.room.integration.testapp.vo.MultiSongPlaylistWithSongs;
import androidx.room.integration.testapp.vo.Playlist;
import androidx.room.integration.testapp.vo.PlaylistSongXRef;
import androidx.room.integration.testapp.vo.PlaylistWithSongTitles;
import androidx.room.integration.testapp.vo.PlaylistWithSongs;
import androidx.room.integration.testapp.vo.ReleasedAlbum;
import androidx.room.integration.testapp.vo.Song;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Flowable;

@Dao
@SuppressWarnings("ROOM_EXPAND_PROJECTION_WITH_UNUSED_COLUMNS")
public interface MusicDao {

    @Insert
    void addSongs(Song... songs);

    @Insert
    void addArtists(Artist... artists);

    @Insert
    void addAlbums(Album... albums);

    @Insert
    void addPlaylists(Playlist... playlists);

    @Insert
    void addPlaylistSongRelation(PlaylistSongXRef... relations);

    @Insert
    void addImages(Image... images);

    @Transaction
    @Query("SELECT * FROM Playlist")
    List<PlaylistWithSongs> getAllPlaylistWithSongs();

    @Transaction
    @Query("SELECT * FROM Playlist WHERE mPlaylistId = :id")
    LiveData<PlaylistWithSongs> getPlaylistsWithSongsLiveData(int id);

    @Transaction
    @Query("SELECT * FROM Playlist WHERE mPlaylistId = :playlistId")
    PlaylistWithSongTitles getPlaylistWithSongTitles(int playlistId);

    @Transaction
    @Query("SELECT * FROM Playlist")
    List<MultiSongPlaylistWithSongs> getAllMultiSongPlaylistWithSongs();

    /* Map of Object to Object */
    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    Map<Artist, Song> getArtistAndFirstSongMap();

    @Query("SELECT * FROM Song JOIN Artist ON Song.mArtist = Artist.mArtistName")
    Map<Song, Artist> getSongAndArtist();

    @Transaction
    @Query("SELECT * FROM Artist JOIN Album ON Artist.mArtistName = Album.mAlbumArtist")
    Map<Artist, AlbumWithSongs> getAllArtistAndTheirAlbumsWithSongs();

    @RawQuery
    Map<Artist, Song> getAllArtistAndTheirSongsRawQuery(SupportSQLiteQuery query);

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    LiveData<Map<Artist, Song>> getAllArtistAndTheirSongsAsLiveData();

    /* Map of Object to List */
    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    Map<Artist, List<Song>> getAllArtistAndTheirSongsList();

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist "
            + "ORDER BY mArtistName ASC")
    Map<Artist, List<Song>> getAllArtistAndTheirSongsListOrdered();

    @Transaction
    @Query("SELECT * FROM Artist JOIN Album ON Artist.mArtistName = Album.mAlbumArtist")
    Map<Artist, List<AlbumWithSongs>> getAllArtistAndTheirAlbumsWithSongsList();

    @RawQuery
    Map<Artist, List<Song>> getAllArtistAndTheirSongsRawQueryList(SupportSQLiteQuery query);

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    LiveData<Map<Artist, List<Song>>> getAllArtistAndTheirSongsAsLiveDataList();

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    Flowable<Map<Artist, List<Song>>> getAllArtistAndTheirSongsAsFlowableList();

    @Query("SELECT Album.mAlbumReleaseYear as mReleaseYear, Album.mAlbumName, Album.mAlbumArtist "
            + "as mBandName"
            + " from Album "
            + "JOIN Song "
            + "ON Album.mAlbumArtist = Song.mArtist AND Album.mAlbumName = Song.mAlbum")
    Map<ReleasedAlbum, List<AlbumNameAndBandName>> getReleaseYearToAlbumsAndBandsList();

    /* Map of Object to Set */
    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    Map<Artist, Set<Song>> getAllArtistAndTheirSongsSet();

    @RawQuery
    Map<Artist, Set<Song>> getAllArtistAndTheirSongsRawQuerySet(SupportSQLiteQuery query);

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    LiveData<Map<Artist, Set<Song>>> getAllArtistAndTheirSongsAsLiveDataSet();

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    Flowable<Map<Artist, Set<Song>>> getAllArtistAndTheirSongsAsFlowableSet();

    /* Guava ImmutableMap */
    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    ImmutableMap<Artist, List<Song>> getAllArtistAndTheirSongsImmutableMap();

    @RawQuery
    ImmutableMap<Artist, List<Song>> getAllArtistAndTheirSongsRawQueryImmutableMap(
            SupportSQLiteQuery query
    );

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    LiveData<ImmutableMap<Artist, Set<Song>>> getAllArtistAndTheirSongsAsLiveDataImmutableMap();

    /* Guava Multimap */
    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    ImmutableSetMultimap<Artist, Song> getAllArtistAndTheirSongsGuavaImmutableSetMultimap();

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    ImmutableListMultimap<Artist, Song> getAllArtistAndTheirSongsGuavaImmutableListMultimap();

    @Transaction
    @Query("SELECT * FROM Artist JOIN Album ON Artist.mArtistName = Album.mAlbumArtist")
    ImmutableSetMultimap<Artist, AlbumWithSongs>
            getAllArtistAndTheirAlbumsWithSongsGuavaImmutableSetMultimap();

    @Transaction
    @Query("SELECT * FROM Artist JOIN Album ON Artist.mArtistName = Album.mAlbumArtist")
    ImmutableListMultimap<Artist, AlbumWithSongs>
            getAllArtistAndTheirAlbumsWithSongsGuavaImmutableListMultimap();

    @RawQuery
    ImmutableSetMultimap<Artist, Song> getAllArtistAndTheirSongsRawQueryGuavaImmutableSetMultimap(
            SupportSQLiteQuery query
    );

    @RawQuery
    ImmutableListMultimap<Artist, Song> getAllArtistAndTheirSongsRawQueryGuavaImmutableListMultimap(
            SupportSQLiteQuery query
    );

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    LiveData<ImmutableSetMultimap<Artist, Song>>
            getAllArtistAndTheirSongsAsLiveDataGuavaImmutableSetMultimap();

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    LiveData<ImmutableListMultimap<Artist, Song>>
            getAllArtistAndTheirSongsAsLiveDataGuavaImmutableListMultimap();

    @RewriteQueriesToDropUnusedColumns
    @MapInfo(keyColumn = "mArtistName")
    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    Map<String, List<Song>> getArtistNameToSongs();

    @RewriteQueriesToDropUnusedColumns
    @MapInfo(keyColumn = "mReleasedYear", valueColumn = "mReleasedYear")
    @Query("SELECT * FROM Album JOIN Song ON Song.mReleasedYear = Album.mAlbumReleaseYear")
    Map<Integer, List<Song>> getReleaseYearToAlbums();

    @RewriteQueriesToDropUnusedColumns
    @MapInfo(keyColumn = "mArtistId")
    @Query("SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image.mArtistInImage")
    LongSparseArray<Artist> getAllAlbumCoverYearToArtistsWithLongSparseArray();

    @RewriteQueriesToDropUnusedColumns
    @MapInfo(keyColumn = "mArtistId")
    @Query("SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image.mArtistInImage")
    SparseArrayCompat<Artist> getAllAlbumCoverYearToArtistsWithIntSparseArray();

    @RewriteQueriesToDropUnusedColumns
    @MapInfo(keyColumn = "mReleasedYear", valueColumn = "mTitle")
    @Query("SELECT * FROM Album JOIN Song ON Song.mReleasedYear = Album.mAlbumReleaseYear")
    Map<Integer, List<String>> getReleaseYearToSongNames();

    @RewriteQueriesToDropUnusedColumns
    @MapInfo(keyColumn = "mArtistName", valueColumn = "mArtist")
    @RawQuery
    Map<String, List<Song>> getArtistNameToSongsRawQuery(SupportSQLiteQuery query);

    @RewriteQueriesToDropUnusedColumns
    @MapInfo(keyColumn = "mReleasedYear", valueColumn = "mReleasedYear")
    @RawQuery
    Map<Integer, List<Song>> getReleaseYearToAlbumsRawQuery(SupportSQLiteQuery query);

    @RewriteQueriesToDropUnusedColumns
    @MapInfo(keyColumn = "mReleasedYear", valueColumn = "mTitle")
    @RawQuery
    Map<Integer, List<String>> getReleaseYearToSongNamesRawQuery(SupportSQLiteQuery query);

    @RewriteQueriesToDropUnusedColumns
    @MapInfo(valueColumn = "songCount")
    @Query("SELECT *, COUNT(mSongId) as songCount FROM Artist JOIN Song ON Artist.mArtistName = "
            + "Song.mArtist GROUP BY mArtistName")
    Map<Artist, Integer> getArtistAndSongCountMap();

    @RewriteQueriesToDropUnusedColumns
    @MapInfo(valueColumn = "songCount")
    @RawQuery
    Map<Artist, Integer> getArtistAndSongCountMapRawQuery(SupportSQLiteQuery query);

    // Other Map Key/Value Types
    @RewriteQueriesToDropUnusedColumns
    @MapInfo(valueColumn = "mAlbumCover")
    @Query("SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image.mArtistInImage")
    ImmutableMap<Artist, ByteBuffer> getAllArtistsWithAlbumCovers();

    @MapInfo(valueColumn = "mAlbumCover")
    @RawQuery
    ImmutableMap<Artist, ByteBuffer> getAllArtistsWithAlbumCoversRawQuery(SupportSQLiteQuery query);

    @RewriteQueriesToDropUnusedColumns
    @MapInfo(valueColumn = "mImageYear")
    @Query("SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image.mArtistInImage")
    ImmutableMap<Artist, Long> getAllArtistsWithAlbumCoverYear();

    @RewriteQueriesToDropUnusedColumns
    @MapInfo(valueColumn = "mImageYear")
    @Query("SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image.mArtistInImage")
    ArrayMap<Artist, Long> getAllArtistsWithAlbumCoverYearArrayMap();

    @MapInfo(keyColumn = "mImageYear")
    @RawQuery
    ImmutableMap<Long, Artist> getAllAlbumCoverYearToArtistsWithRawQuery(SupportSQLiteQuery query);

    @MapInfo(keyColumn = "mImageYear")
    @RawQuery
    ArrayMap<Long, Artist> getAllAlbumCoverYearToArtistsWithRawQueryArrayMap(
            SupportSQLiteQuery query
    );

    @MapInfo(keyColumn = "mAlbumCover", valueColumn = "mIsActive")
    @Query("SELECT * FROM Image JOIN Artist ON Artist.mArtistName = Image.mArtistInImage")
    ImmutableMap<ByteBuffer, Boolean> getAlbumCoversWithBandActivity();

    @MapInfo(keyColumn = "mAlbumCover", valueColumn = "mIsActive")
    @RawQuery
    ImmutableMap<ByteBuffer, Boolean> getAlbumCoversWithBandActivityRawQuery(
            SupportSQLiteQuery query
    );

    @MapInfo(keyColumn = "mDateReleased", valueColumn = "mIsActive")
    @Query("SELECT * FROM Image JOIN Artist ON Artist.mArtistName = Image.mArtistInImage")
    ImmutableMap<Date, Boolean> getAlbumDateWithBandActivity();

    @MapInfo(keyColumn = "mDateReleased", valueColumn = "mIsActive")
    @RawQuery
    ImmutableMap<Date, Boolean> getAlbumDateWithBandActivityRawQuery(SupportSQLiteQuery query);

    @MapInfo(keyColumn = "mFormat", valueColumn = "mIsActive")
    @Query("SELECT * FROM Image JOIN Artist ON Artist.mArtistName = Image.mArtistInImage")
    ImmutableMap<ImageFormat, Boolean> getImageFormatWithBandActivity();

    @MapInfo(keyColumn = "mFormat", valueColumn = "mIsActive")
    @RawQuery
    ImmutableMap<ImageFormat, Boolean> getImageFormatWithBandActivityRawQuery(
            SupportSQLiteQuery query
    );

    @MapInfo(keyColumn = "dog", valueColumn = "cat")
    @RawQuery
    Map<Artist, Integer> getMapWithInvalidColumnRawQuery(SupportSQLiteQuery query);
}
