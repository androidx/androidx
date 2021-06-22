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

package androidx.room.integration.testapp;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.integration.testapp.dao.MusicDao;
import androidx.room.integration.testapp.vo.Album;
import androidx.room.integration.testapp.vo.Artist;
import androidx.room.integration.testapp.vo.Playlist;
import androidx.room.integration.testapp.vo.PlaylistMultiSongXRefView;
import androidx.room.integration.testapp.vo.PlaylistSongXRef;
import androidx.room.integration.testapp.vo.Song;

@Database(
        entities = {Song.class, Playlist.class, PlaylistSongXRef.class, Artist.class, Album.class},
        views = {PlaylistMultiSongXRefView.class},
        version = 1,
        exportSchema = false)
public abstract class MusicTestDatabase extends RoomDatabase {
    public abstract MusicDao getDao();
}
