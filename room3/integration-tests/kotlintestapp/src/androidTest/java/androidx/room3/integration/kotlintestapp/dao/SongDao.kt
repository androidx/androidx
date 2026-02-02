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
package androidx.room3.integration.kotlintestapp.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.integration.kotlintestapp.vo.Song
import androidx.room3.integration.kotlintestapp.vo.SongDescription
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Insert fun insert(song: Song)

    @Insert fun insert(songs: List<Song>)

    @Query("SELECT * FROM SongDescription WHERE SongDescription MATCH :searchQuery")
    fun getSongDescriptions(searchQuery: String): List<SongDescription>

    @Query(
        """
            SELECT s.mSongId, s.mTitle, s.mArtist, s.mAlbum, s.mLength, s.mReleasedYear
            FROM Song as s JOIN SongDescription as fts ON (docid = mSongId)
            WHERE fts.mTitle MATCH :searchQuery
            """
    )
    fun getSongs(searchQuery: String): List<Song>

    @Query("SELECT * FROM Song") fun getFlowSong(): Flow<List<Song>>

    @Query("SELECT * FROM SongDescription")
    fun getFlowSongDescription(): Flow<List<SongDescription>>
}
