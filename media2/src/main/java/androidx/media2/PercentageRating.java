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
 * A class for rating expressed as a percentage.
 */
@VersionedParcelize
public final class PercentageRating implements Rating {
    private static final float RATING_NOT_RATED = -1.0f;

    @ParcelField(1)
    float mPercent;

    /**
     * Creates a unrated PercentageRating instance.
     */
    public PercentageRating() {
        mPercent = RATING_NOT_RATED;
    }

    /**
     * Creates a PercentageRating instance with the given percentage.
     * If {@code percent} is less than 0f or greater than 100f, it will throw
     * IllegalArgumentException.
     *
     * @param percent the value of the rating
     */
    public PercentageRating(float percent) {
        if (percent < 0.0f || percent > 100.0f) {
            throw new IllegalArgumentException("percent should be in the rage of [0, 100]");
        }
        mPercent = percent;
    }

    @Override
    public boolean isRated() {
        return mPercent != RATING_NOT_RATED;
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(mPercent);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PercentageRating)) {
            return false;
        }
        return mPercent == ((PercentageRating) obj).mPercent;
    }

    @Override
    public String toString() {
        return "PercentageRating: " + (isRated() ? "percentage=" + mPercent : "unrated");
    }

    /**
     * Returns the percentage-based rating value.
     *
     * @return a rating value greater or equal to 0.0f, or a negative value if it is unrated.
     */
    public float getPercentRating() {
        return mPercent;
    }
}
