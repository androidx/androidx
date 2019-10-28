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

package androidx.room.integration.testapp.vo;

import androidx.room.DatabaseView;

// View of join table with playlists with more than 1 song
@DatabaseView("SELECT * FROM PlaylistSongXRef WHERE mPlaylistId IN (SELECT mPlaylistId FROM"
        + " PlaylistSongXRef GROUP BY mPlaylistId HAVING COUNT(mSongId) > 1)")
public class PlaylistMultiSongXRefView {
    public final int mPlaylistId;
    public final int mSongId;

    public PlaylistMultiSongXRefView(int playlistId, int songId) {
        mPlaylistId = playlistId;
        mSongId = songId;
    }
}
