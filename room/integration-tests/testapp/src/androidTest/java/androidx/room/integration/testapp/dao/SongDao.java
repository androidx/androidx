/*
 * Copyright 2018 The Android Open Source Project
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
import androidx.room.integration.testapp.vo.Song;
import androidx.room.integration.testapp.vo.SongDescription;

import java.util.List;

@Dao
public interface SongDao {

    @Insert
    void insert(Song song);

    @Insert
    void insert(List<Song> songs);

    @Query("SELECT * FROM SongDescription WHERE SongDescription MATCH :searchQuery")
    List<SongDescription> getSongDescriptions(String searchQuery);

    @Query("SELECT s.mSongId, s.mTitle, s.mArtist, s.mAlbum, s.mLength, s.mReleasedYear FROM "
            + "Song as s JOIN SongDescription as fts ON (docid = mSongId) "
            + "WHERE fts.mTitle MATCH :searchQuery")
    List<Song> getSongs(String searchQuery);

    @Query("SELECT * FROM Song")
    LiveData<List<Song>> getLiveDataSong();

    @Query("SELECT * FROM SongDescription")
    LiveData<List<SongDescription>> getLiveDataSongDescription();
}
