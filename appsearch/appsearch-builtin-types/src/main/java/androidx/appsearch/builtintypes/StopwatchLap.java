/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appsearch.builtintypes;

import androidx.annotation.NonNull;
import androidx.appsearch.annotation.Document;
import androidx.core.util.Preconditions;

/**
 * An AppSearch document representing a {@link StopwatchLap} entity.
 *
 * <p>A {@link Stopwatch} can create a Lap, which will take a snapshot of the duration from when the
 * previous Lap was created to when the new Lap is created. For example: if a {@link Stopwatch}
 * starts at 12:00, creates a Lap at 12:05, and creates another Lap at 12:15, then it would have
 * created two Laps with 5 minutes duration and 10 minutes duration respectively.
 */
@Document(name = "builtin:StopwatchLap")
public class StopwatchLap {
    @Document.Namespace
    private final String mNamespace;

    @Document.Id
    private final String mId;

    @Document.Score
    private final int mDocumentScore;

    @Document.CreationTimestampMillis
    private final long mCreationTimestampMillis;

    @Document.TtlMillis
    private final long mDocumentTtlMillis;

    @Document.LongProperty
    private final int mLapNumber;

    @Document.LongProperty
    private final long mLapDurationMillis;

    @Document.LongProperty
    private final long mCumulativeLapDurationMillis;

    StopwatchLap(@NonNull String namespace, @NonNull String id, int documentScore,
            long creationTimestampMillis, long documentTtlMillis, int lapNumber,
            long lapDurationMillis, long cumulativeLapDurationMillis) {
        mNamespace = Preconditions.checkNotNull(namespace);
        mId = Preconditions.checkNotNull(id);
        mDocumentScore = documentScore;
        mCreationTimestampMillis = creationTimestampMillis;
        mDocumentTtlMillis = documentTtlMillis;
        mLapNumber = lapNumber;
        mLapDurationMillis = lapDurationMillis;
        mCumulativeLapDurationMillis = cumulativeLapDurationMillis;
    }

    /** Returns the namespace. */
    @NonNull
    public String getNamespace() {
        return mNamespace;
    }

    /** Returns the unique identifier. */
    @NonNull
    public String getId() {
        return mId;
    }

    /**
     * Returns the user-provided opaque document score of the current AppSearch document, which can
     * be used for ranking using
     * {@link androidx.appsearch.app.SearchSpec.RankingStrategy#RANKING_STRATEGY_DOCUMENT_SCORE}.
     *
     * <p>See {@link Document.Score} for more information on score.
     */
    public int getDocumentScore() {
        return mDocumentScore;
    }

    /**
     * Returns the creation timestamp for the current AppSearch entity, in milliseconds using the
     * {@link System#currentTimeMillis()} time base.
     *
     * <p>This timestamp refers to the creation time of the AppSearch entity, not when the
     * document is written into AppSearch.
     *
     * <p>If not set, then the current timestamp will be used.
     *
     * <p>See {@link androidx.appsearch.annotation.Document.CreationTimestampMillis} for more
     * information on creation timestamp.
     */
    public long getCreationTimestampMillis() {
        return mCreationTimestampMillis;
    }

    /**
     * Returns the time-to-live (TTL) for the current AppSearch document as a duration in
     * milliseconds.
     *
     * <p>The document will be automatically deleted when the TTL expires.
     *
     * <p>See {@link Document.TtlMillis} for more information on TTL.
     */
    public long getDocumentTtlMillis() {
        return mDocumentTtlMillis;
    }

    /** Returns the position of the current {@link StopwatchLap}, starting at 1. */
    public int getLapNumber() {
        return mLapNumber;
    }

    /**
     * Returns the duration in milliseconds for the current {@link StopwatchLap}.
     *
     * <p>The duration for the current {@link StopwatchLap} is counted from the previous
     * {@link StopwatchLap} instance, or from the beginning of the {@link Stopwatch} if no previous
     * instance exists, to when the current {@link StopwatchLap} instance is created.
     */
    public long getLapDurationMillis() {
        return mLapDurationMillis;
    }

    /**
     * Returns the cumulative duration in milliseconds for all previous {@link StopwatchLap}
     * instances up to and including this one.
     */
    public long getCumulativeLapDurationMillis() {
        return mCumulativeLapDurationMillis;
    }

    /** Builder for {@link StopwatchLap}. */
    public static final class Builder extends BaseBuiltinTypeBuilder<Builder> {
        private int mLapNumber;
        private long mLapDurationMillis;
        private long mCumulativeLapDurationMillis;

        /**
         * Constructor for {@link StopwatchLap.Builder}.
         *
         * @param namespace Namespace for the Document. See {@link Document.Namespace}.
         * @param id Unique identifier for the Document. See {@link Document.Id}.
         */
        public Builder(@NonNull String namespace, @NonNull String id) {
            super(namespace, id);
        }

        /**
         * Constructor for {@link StopwatchLap.Builder} with all the existing values.
         */
        public Builder(@NonNull StopwatchLap stopwatchLap) {
            this(stopwatchLap.getNamespace(), stopwatchLap.getId());

            this.mDocumentScore = stopwatchLap.getDocumentScore();
            this.mCreationTimestampMillis = stopwatchLap.getCreationTimestampMillis();
            this.mDocumentTtlMillis = stopwatchLap.getDocumentTtlMillis();
            this.mLapNumber = stopwatchLap.getLapNumber();
            this.mLapDurationMillis = stopwatchLap.getLapDurationMillis();
            this.mCumulativeLapDurationMillis =
                    stopwatchLap.getCumulativeLapDurationMillis();
        }

        /** Sets the position of the current {@link StopwatchLap}, starting at 1. */
        @NonNull
        public Builder setLapNumber(int lapNumber) {
            Preconditions.checkArgument(lapNumber >= 1, "Lap number must start at 1");
            mLapNumber = lapNumber;
            return this;
        }

        /**
         * Sets the duration in milliseconds for the current {@link StopwatchLap}.
         *
         * <p>The duration for the current {@link StopwatchLap} is counted from the previous
         * {@link StopwatchLap} instance, or from the beginning of the {@link Stopwatch} if no
         * previous instance exists, to when the current {@link StopwatchLap} instance is created.
         */
        @NonNull
        public Builder setLapDurationMillis(long lapDurationMillis) {
            mLapDurationMillis = lapDurationMillis;
            return this;
        }

        /**
         * Sets the cumulative duration in milliseconds for all previous {@link StopwatchLap}
         * instances up to and including this one.
         */
        @NonNull
        public Builder setCumulativeLapDurationMillis(long cumulativeLapDurationMillis) {
            mCumulativeLapDurationMillis = cumulativeLapDurationMillis;
            return this;
        }

        /** Builds the {@link StopwatchLap}. */
        @NonNull
        public StopwatchLap build() {
            return new StopwatchLap(mNamespace, mId, mDocumentScore,
                    mCreationTimestampMillis, mDocumentTtlMillis, mLapNumber,
                    mLapDurationMillis, mCumulativeLapDurationMillis);
        }
    }
}
