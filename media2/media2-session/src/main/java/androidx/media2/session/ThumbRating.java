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

package androidx.media2.session;

import androidx.core.util.ObjectsCompat;
import androidx.media2.common.Rating;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * A class for rating with a single degree of rating, "thumb up" vs "thumb down".
 *
 * @deprecated androidx.media2 is deprecated. Please migrate to <a
 *     href="https://developer.android.com/guide/topics/media/media3">androidx.media3</a>.
 */
@Deprecated
@VersionedParcelize
public final class ThumbRating implements Rating {
    @ParcelField(1)
    boolean mIsRated;

    @ParcelField(2)
    boolean mThumbUp;

    // WARNING: Adding a new ParcelField may break old library users (b/152830728)

    /**
     * Creates a unrated ThumbRating instance.
     */
    public ThumbRating() {
        mIsRated = false;
    }

    /**
     * Creates a ThumbRating instance.
     *
     * @param thumbIsUp true for a "thumb up" rating, false for "thumb down".
     */
    public ThumbRating(boolean thumbIsUp) {
        mThumbUp = thumbIsUp;
        mIsRated = true;
    }

    @Override
    public boolean isRated() {
        return mIsRated;
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(mIsRated, mThumbUp);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ThumbRating)) {
            return false;
        }
        ThumbRating other = (ThumbRating) obj;
        return mThumbUp == other.mThumbUp && mIsRated == other.mIsRated;
    }

    @Override
    public String toString() {
        return "ThumbRating: " + (mIsRated ? "isThumbUp=" + mThumbUp : "unrated");
    }

    /**
     * Returns whether the rating is "thumb up".
     *
     * @return true if the rating is "thumb up", false if the rating is "thumb down",
     *         or if it is unrated.
     */
    public boolean isThumbUp() {
        return mThumbUp;
    }
}
