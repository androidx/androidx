/*
 * Copyright 2017 The Android Open Source Project
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


public class ReleasedAlbum {
    private String mReleaseYear;
    private String mAlbumName;

    public ReleasedAlbum(String releaseYear, String albumName) {
        mReleaseYear = releaseYear;

        mAlbumName = albumName;
    }

    public String getReleaseYear() {
        return mReleaseYear;
    }

    public String getAlbumName() {
        return mAlbumName;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReleasedAlbum that = (ReleasedAlbum) o;
        if (mReleaseYear != null ? !mReleaseYear.equals(that.mReleaseYear) :
                that.mReleaseYear != null) {
            return false;
        }
        return mAlbumName != null ? mAlbumName.equals(that.mAlbumName) : that.mAlbumName == null;
    }

    @Override
    public int hashCode() {
        int result = mReleaseYear != null ? mReleaseYear.hashCode() : 0;
        result = 31 * result + (mAlbumName != null ? mAlbumName.hashCode() : 0);
        return result;
    }
}
