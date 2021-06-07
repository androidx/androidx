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

package androidx.room.integration.testapp.vo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Album {
    @PrimaryKey
    public final int mAlbumId;
    public final String mAlbumName;
    public final String mAlbumArtist;
    public final String mAlbumReleaseYear;

    public Album(int albumId, String albumName, String albumArtist, String albumReleaseYear) {
        mAlbumId = albumId;
        mAlbumName = albumName;
        mAlbumArtist = albumArtist;
        mAlbumReleaseYear = albumReleaseYear;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Album album = (Album) o;

        if (mAlbumId != album.mAlbumId) return false;
        if (mAlbumName != null ? !mAlbumName.equals(album.mAlbumName) :
                album.mAlbumName != null) {
            return false;
        }
        if (mAlbumArtist != null ? !mAlbumArtist.equals(album.mAlbumArtist) :
                album.mAlbumArtist != null) {
            return false;
        }
        if (mAlbumReleaseYear != null ? !mAlbumReleaseYear.equals(album.mAlbumReleaseYear) :
                album.mAlbumReleaseYear != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = mAlbumId;
        result = 31 * result + (mAlbumName != null ? mAlbumName.hashCode() : 0);
        result = 31 * result + (mAlbumArtist != null ? mAlbumArtist.hashCode() : 0);
        result = 31 * result + (mAlbumReleaseYear != null ? mAlbumReleaseYear.hashCode() : 0);
        return result;
    }
}
