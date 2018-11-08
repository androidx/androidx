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

import androidx.annotation.IntRange;
import androidx.core.util.ObjectsCompat;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * A class for rating expressed as the number of stars.
 */
@VersionedParcelize
public final class StarRating implements Rating {
    private static final float RATING_NOT_RATED = -1.0f;

    @ParcelField(1)
    int mMaxStars;

    @ParcelField(2)
    float mStarRating;

    /*
     * Used for VersionedParcelable
     */
    StarRating() {
    }

    /**
     * Creates a unrated StarRating instance with {@code maxStars}.
     * If {@code maxStars} is not a positive integer, it will throw IllegalArgumentException.
     *
     * @param maxStars a range of this star rating from 0.0f to {@code maxStars}
     */
    public StarRating(@IntRange(from = 1) int maxStars) {
        if (maxStars <= 0) {
            throw new IllegalArgumentException("maxStars should be a positive integer");
        }
        mMaxStars = maxStars;
        mStarRating = RATING_NOT_RATED;
    }

    /**
     * Creates a StarRating instance with {@code maxStars} and the given integer or fractional
     * number of stars. Non integer values can for instance be used to represent an average rating
     * value, which might not be an integer number of stars.
     * If {@code maxStars} is not a positive integer or {@code starRating} has invalid value,
     * it will throw IllegalArgumentException.
     *
     * @param maxStars the maximum number of stars which this rating can have.
     * @param starRating a number ranging from 0.0f to {@code maxStars}
     */
    public StarRating(@IntRange(from = 1) int maxStars, float starRating) {
        if (maxStars <= 0) {
            throw new IllegalArgumentException("maxStars should be a positive integer");
        } else if (starRating < 0.0f || starRating > maxStars) {
            throw new IllegalArgumentException("starRating is out of range [0, maxStars]");
        }
        mMaxStars = maxStars;
        mStarRating = starRating;
    }

    @Override
    public boolean isRated() {
        return mStarRating >= 0.0f;
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(mMaxStars, mStarRating);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StarRating)) {
            return false;
        }
        StarRating other = (StarRating) obj;
        return mMaxStars == other.mMaxStars && mStarRating == other.mStarRating;
    }

    @Override
    public String toString() {
        return "StarRating: maxStars=" + mMaxStars
                + (isRated() ? ", starRating=" + mStarRating : ", unrated");
    }

    /**
     * Returns the max stars.
     *
     * @return a max number of stars for this star rating.
     */
    public int getMaxStars() {
        return mMaxStars;
    }

    /**
     * Returns the star-based rating value.
     *
     * @return a rating value greater or equal to 0.0f, or a negative value if it is unrated.
     */
    public float getStarRating() {
        return mStarRating;
    }
}
