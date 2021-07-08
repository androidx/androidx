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

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Transaction;
import androidx.room.integration.testapp.vo.Album;
import androidx.room.integration.testapp.vo.AlbumNameAndBandName;
import androidx.room.integration.testapp.vo.AlbumWithSongs;
import androidx.room.integration.testapp.vo.Artist;
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

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Flowable;

@Dao
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
}
