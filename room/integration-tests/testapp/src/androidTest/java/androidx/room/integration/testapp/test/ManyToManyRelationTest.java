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

package androidx.room.integration.testapp.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import android.content.Context;

import androidx.room.Room;
import androidx.room.integration.testapp.MusicTestDatabase;
import androidx.room.integration.testapp.dao.MusicDao;
import androidx.room.integration.testapp.vo.MultiSongPlaylistWithSongs;
import androidx.room.integration.testapp.vo.Playlist;
import androidx.room.integration.testapp.vo.PlaylistSongXRef;
import androidx.room.integration.testapp.vo.PlaylistWithSongTitles;
import androidx.room.integration.testapp.vo.PlaylistWithSongs;
import androidx.room.integration.testapp.vo.Song;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ManyToManyRelationTest {

    private MusicDao mMusicDao;

    private final Song mSong1 = new Song(
            1,
            "Desde el Corazón",
            "Bad Bunny",
            "BPPR Single",
            127,
            2019);
    private final Song mSong2 = new Song(
            2,
            "Cambumbo",
            "Tego Calderon",
            "El Abayarde",
            180,
            2002);

    private final Playlist mPlaylist1 = new Playlist(1);
    private final Playlist mPlaylist2 = new Playlist(2);

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        MusicTestDatabase db = Room.inMemoryDatabaseBuilder(context, MusicTestDatabase.class)
                .build();
        mMusicDao = db.getDao();
    }

    @Test
    public void relationWithJumpTable() {
        mMusicDao.addSongs(mSong1, mSong2);
        mMusicDao.addPlaylists(mPlaylist1, mPlaylist2);

        mMusicDao.addPlaylistSongRelation(
                new PlaylistSongXRef(1, 1),
                new PlaylistSongXRef(1, 2),
                new PlaylistSongXRef(2, 1)
        );

        List<PlaylistWithSongs> playlistWithSongs = mMusicDao.getAllPlaylistWithSongs();
        assertThat(playlistWithSongs.size(), is(2));

        assertThat(playlistWithSongs.get(0).playlist, is(mPlaylist1));
        assertThat(playlistWithSongs.get(0).songs.size(), is(2));
        assertThat(playlistWithSongs.get(0).songs.get(0), is(mSong1));
        assertThat(playlistWithSongs.get(0).songs.get(1), is(mSong2));

        assertThat(playlistWithSongs.get(1).playlist, is(mPlaylist2));
        assertThat(playlistWithSongs.get(1).songs.size(), is(1));
        assertThat(playlistWithSongs.get(1).songs.get(0), is(mSong1));
    }

    @Test
    public void relationWithJumpTable_projection() {
        mMusicDao.addSongs(mSong1, mSong2);
        mMusicDao.addPlaylists(mPlaylist1, mPlaylist2);

        mMusicDao.addPlaylistSongRelation(
                new PlaylistSongXRef(1, 1),
                new PlaylistSongXRef(1, 2),
                new PlaylistSongXRef(2, 1)
        );

        PlaylistWithSongTitles playlistWithSongTitles = mMusicDao.getPlaylistWithSongTitles(1);

        assertThat(playlistWithSongTitles.playlist, is(mPlaylist1));
        assertThat(playlistWithSongTitles.titles.size(), is(2));
        assertThat(playlistWithSongTitles.titles.get(0), is(mSong1.mTitle));
        assertThat(playlistWithSongTitles.titles.get(1), is(mSong2.mTitle));
    }

    @Test
    public void relationWithJumpView() {
        mMusicDao.addSongs(mSong1, mSong2);
        mMusicDao.addPlaylists(mPlaylist1, mPlaylist2);

        mMusicDao.addPlaylistSongRelation(
                new PlaylistSongXRef(1, 1),
                new PlaylistSongXRef(1, 2),
                new PlaylistSongXRef(2, 1)
        );

        List<MultiSongPlaylistWithSongs> playlistWithSongs =
                mMusicDao.getAllMultiSongPlaylistWithSongs();
        assertThat(playlistWithSongs.size(), is(2));

        assertThat(playlistWithSongs.get(0).playlist, is(mPlaylist1));
        assertThat(playlistWithSongs.get(0).songs.size(), is(2));
        assertThat(playlistWithSongs.get(0).songs.get(0), is(mSong1));
        assertThat(playlistWithSongs.get(0).songs.get(1), is(mSong2));

        assertThat(playlistWithSongs.get(1).playlist, is(mPlaylist2));
        assertThat(playlistWithSongs.get(1).songs.size(), is(0));
    }
}
