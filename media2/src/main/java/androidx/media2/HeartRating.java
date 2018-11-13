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
 * A class for rating with a single degree of rating, "heart" vs "no heart".
 * This can be used to indicate the content referred to is a favorite (or not).
 */
@VersionedParcelize
public final class HeartRating implements Rating {
    @ParcelField(1)
    boolean mIsRated;

    @ParcelField(2)
    boolean mHasHeart;

    /**
     * Creates a unrated HeartRating instance.
     */
    public HeartRating() {
        mIsRated = false;
    }

    /**
     * Creates a HeartRating instance.
     *
     * @param hasHeart true for a "heart selected" rating, false for "heart unselected".
     */
    public HeartRating(boolean hasHeart) {
        mHasHeart = hasHeart;
        mIsRated = true;
    }

    @Override
    public boolean isRated() {
        return mIsRated;
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(mIsRated, mHasHeart);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HeartRating)) {
            return false;
        }
        HeartRating other = (HeartRating) obj;
        return mHasHeart == other.mHasHeart && mIsRated == other.mIsRated;
    }

    @Override
    public String toString() {
        return "HeartRating: " + (mIsRated ? "hasHeart=" + mHasHeart : "unrated");
    }

    /**
     * Returns whether the rating is "heart selected".
     *
     * @return true if the rating is "heart selected", false if the rating is "heart unselected",
     *         or if it is unrated.
     */
    public boolean hasHeart() {
        return mHasHeart;
    }
}
