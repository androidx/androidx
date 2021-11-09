/*
 * Copyright 2021 The Android Open Source Project
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

package foo.bar;
import androidx.room.*;

@Entity
public class Song {
    @PrimaryKey
    public final int mSongId;
    public final String mTitle;
    public final String mArtist;
    public final String mAlbum;
    public final int mLength; // in seconds
    public final int mReleasedYear;


    public Song(int songId, String title, String artist, String album, int length,
            int releasedYear) {
        mSongId = songId;
        mTitle = title;
        mArtist = artist;
        mAlbum = album;
        mLength = length;
        mReleasedYear = releasedYear;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Song song = (Song) o;

        if (mSongId != song.mSongId) return false;
        if (mLength != song.mLength) return false;
        if (mReleasedYear != song.mReleasedYear) return false;
        if (mTitle != null ? !mTitle.equals(song.mTitle) : song.mTitle != null) return false;
        if (mArtist != null ? !mArtist.equals(song.mArtist) : song.mArtist != null) return false;
        return mAlbum != null ? mAlbum.equals(song.mAlbum) : song.mAlbum == null;
    }

    @Override
    public int hashCode() {
        int result = mSongId;
        result = 31 * result + (mTitle != null ? mTitle.hashCode() : 0);
        result = 31 * result + (mArtist != null ? mArtist.hashCode() : 0);
        result = 31 * result + (mAlbum != null ? mAlbum.hashCode() : 0);
        result = 31 * result + mLength;
        result = 31 * result + mReleasedYear;
        return result;
    }
}