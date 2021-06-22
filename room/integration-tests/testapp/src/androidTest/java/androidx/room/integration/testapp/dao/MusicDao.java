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
import androidx.room.RoomWarnings;
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

import java.util.List;
import java.util.Map;

import io.reactivex.Flowable;

@Dao
@SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
// TODO: (b/191693863) Cannot use @RewriteQueriesToDropUnusedColumns due to this bug.
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

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    Map<Artist, List<Song>> getAllArtistAndTheirSongs();

    @Query("SELECT * FROM Artist JOIN Album ON Artist.mArtistName = Album.mAlbumArtist")
    Map<Artist, List<AlbumWithSongs>> getAllArtistAndTheirAlbumsWithSongs();

    @RawQuery
    Map<Artist, List<Song>> getAllArtistAndTheirSongsRawQuery(SupportSQLiteQuery query);

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    LiveData<Map<Artist, List<Song>>> getAllArtistAndTheirSongsAsLiveData();

    @Query("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
    Flowable<Map<Artist, List<Song>>> getAllArtistAndTheirSongsAsFlowable();

    @Query("SELECT Album.mAlbumReleaseYear as mReleaseYear, Album.mAlbumName, Album.mAlbumArtist "
            + "as mBandName"
            + " from Album "
            + "JOIN Song "
            + "ON Album.mAlbumArtist = Song.mArtist AND Album.mAlbumName = Song.mAlbum")
    Map<ReleasedAlbum, List<AlbumNameAndBandName>> getReleaseYearToAlbumsAndBands();
}
