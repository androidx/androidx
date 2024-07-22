/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.appsearch.localstorage.stats;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.core.util.Preconditions;

// TODO(b/319285816): link converter here.
/**
 * Class holds detailed stats of a click action, converted from
 * {@link androidx.appsearch.app.PutDocumentsRequest#getTakenActionGenericDocuments}.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ClickStats {
    private final long mTimestampMillis;

    private final long mTimeStayOnResultMillis;

    private final int mResultRankInBlock;

    private final int mResultRankGlobal;

    private final boolean mIsGoodClick;

    ClickStats(@NonNull Builder builder) {
        Preconditions.checkNotNull(builder);
        mTimestampMillis = builder.mTimestampMillis;
        mTimeStayOnResultMillis = builder.mTimeStayOnResultMillis;
        mResultRankInBlock = builder.mResultRankInBlock;
        mResultRankGlobal = builder.mResultRankGlobal;
        mIsGoodClick = builder.mIsGoodClick;
    }

    /** Returns the click action timestamp in milliseconds since Unix epoch. */
    public long getTimestampMillis() {
        return mTimestampMillis;
    }

    /** Returns the time (duration) of the user staying on the clicked result. */
    public long getTimeStayOnResultMillis() {
        return mTimeStayOnResultMillis;
    }

    /** Returns the in-block rank of the clicked result. */
    public int getResultRankInBlock() {
        return mResultRankInBlock;
    }

    /** Returns the global rank of the clicked result. */
    public int getResultRankGlobal() {
        return mResultRankGlobal;
    }

    /**
     * Returns whether this click is a good click or not.
     *
     * @see Builder#setIsGoodClick
     */
    public boolean isGoodClick() {
        return mIsGoodClick;
    }

    /** Builder for {@link ClickStats} */
    public static final class Builder {
        private long mTimestampMillis;

        private long mTimeStayOnResultMillis;

        private int mResultRankInBlock;

        private int mResultRankGlobal;

        private boolean mIsGoodClick = true;

        /** Sets the click action timestamp in milliseconds since Unix epoch. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setTimestampMillis(long timestampMillis) {
            mTimestampMillis = timestampMillis;
            return this;
        }

        /** Sets the time (duration) of the user staying on the clicked result. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setTimeStayOnResultMillis(long timeStayOnResultMillis) {
            mTimeStayOnResultMillis = timeStayOnResultMillis;
            return this;
        }

        /** Sets the in-block rank of the clicked result. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setResultRankInBlock(int resultRankInBlock) {
            mResultRankInBlock = resultRankInBlock;
            return this;
        }

        /** Sets the global rank of the clicked result. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setResultRankGlobal(int resultRankGlobal) {
            mResultRankGlobal = resultRankGlobal;
            return this;
        }

        /**
         * Sets the flag indicating whether the click is good or not.
         *
         * <p>A good click means the user is satisfied by the clicked document. The caller should
         * define its own criteria and set this field accordingly.
         *
         * <p>The default value is true if unset. We should treat it as a good click by default if
         * the caller didn't specify or could not determine for several reasons:
         *
         * <ul>
         *   <li>It may be difficult for the caller to determine if the user is satisfied by the
         *       clicked document or not.
         *   <li>AppSearch collects search quality metrics that are related to number of good
         *       clicks. We don't want to demote the quality score aggressively by the undetermined
         *       ones.
         * </ul>
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setIsGoodClick(boolean isGoodClick) {
            mIsGoodClick = isGoodClick;
            return this;
        }

        /** Builds a new {@link ClickStats} from the {@link ClickStats.Builder}. */
        @NonNull
        public ClickStats build() {
            return new ClickStats(/* builder= */ this);
        }
    }
}
