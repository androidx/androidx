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

package androidx.media2;

import androidx.core.util.ObjectsCompat;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * A class for rating with a single degree of rating, "thumb up" vs "thumb down".
 */
@VersionedParcelize
public final class ThumbRating2 implements Rating2 {
    @ParcelField(1)
    boolean mIsRated;

    @ParcelField(2)
    boolean mThumbUp;

    /**
     * Creates a unrated ThumbRating2 instance.
     */
    public ThumbRating2() {
        mIsRated = false;
    }

    /**
     * Creates a ThumbRating2 instance.
     *
     * @param thumbIsUp true for a "thumb up" rating, false for "thumb down".
     */
    public ThumbRating2(boolean thumbIsUp) {
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
        if (!(obj instanceof ThumbRating2)) {
            return false;
        }
        ThumbRating2 other = (ThumbRating2) obj;
        return mThumbUp == other.mThumbUp && mIsRated == other.mIsRated;
    }

    @Override
    public String toString() {
        return "ThumbRating2: " + (mIsRated ? "isThumbUp=" + mThumbUp : "unrated");
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
