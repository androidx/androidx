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
public class Artist {
    @PrimaryKey
    public final int mArtistId;
    public final String mArtistName;

    public Artist(int artistId, String artistName) {
        mArtistId = artistId;
        mArtistName = artistName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Artist artist = (Artist) o;

        if (mArtistId != artist.mArtistId) return false;
        if (mArtistName != null ? !mArtistName.equals(artist.mArtistName) :
                artist.mArtistName != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = mArtistId;
        result = 31 * result + (mArtistName != null ? mArtistName.hashCode() : 0);
        return result;
    }
}
