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
import androidx.annotation.Nullable;
import androidx.appsearch.annotation.Document;
import androidx.core.util.Preconditions;

import java.util.List;

/**
 * An AppSearch document representing a {@link StopwatchLap} entity.
 *
 * <p>A {@link Stopwatch} can create a Lap, which will take a snapshot of the duration from when the
 * previous Lap was created to when the new Lap is created. For example: if a {@link Stopwatch}
 * starts at 12:00, creates a Lap at 12:05, and creates another Lap at 12:15, then it would have
 * created two Laps with 5 minutes duration and 10 minutes duration respectively.
 */
@Document(name = "builtin:StopwatchLap")
public class StopwatchLap extends Thing {
    @Document.LongProperty
    private final int mLapNumber;

    @Document.LongProperty
    private final long mLapDurationMillis;

    @Document.LongProperty
    private final long mAccumulatedLapDurationMillis;

    StopwatchLap(@NonNull String namespace, @NonNull String id, int documentScore,
            long creationTimestampMillis, long documentTtlMillis, @Nullable String name,
            @Nullable List<String> alternateNames, @Nullable String description,
            @Nullable String image, @Nullable String url,
            @NonNull List<PotentialAction> potentialActions,
            int lapNumber, long lapDurationMillis, long accumulatedLapDurationMillis) {
        super(namespace, id, documentScore, creationTimestampMillis, documentTtlMillis, name,
                alternateNames, description, image, url, potentialActions);
        mLapNumber = lapNumber;
        mLapDurationMillis = lapDurationMillis;
        mAccumulatedLapDurationMillis = accumulatedLapDurationMillis;
    }

    /** Returns the position of the current {@link StopwatchLap}, starting at 1. */
    public int getLapNumber() {
        return mLapNumber;
    }

    /**
     * Returns the total duration in milliseconds accumulated by the current {@link StopwatchLap}.
     */
    public long getLapDurationMillis() {
        return mLapDurationMillis;
    }

    /**
     * Returns the total duration in milliseconds accumulated by all the {@link StopwatchLap}
     * instances up to and including this one.
     */
    public long getAccumulatedLapDurationMillis() {
        return mAccumulatedLapDurationMillis;
    }

    /** Builder for {@link StopwatchLap}. */
    public static final class Builder extends BuilderImpl<Builder> {
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
            super(stopwatchLap);
        }
    }

    @SuppressWarnings("unchecked")
    static class BuilderImpl<T extends BuilderImpl<T>> extends Thing.BuilderImpl<T> {
        protected int mLapNumber;
        protected long mLapDurationMillis;
        protected long mAccumulatedLapDurationMillis;

        BuilderImpl(@NonNull String namespace, @NonNull String id) {
            super(namespace, id);
        }

        BuilderImpl(@NonNull StopwatchLap stopwatchLap) {
            super(new Thing.Builder(stopwatchLap).build());

            this.mLapNumber = stopwatchLap.getLapNumber();
            this.mLapDurationMillis = stopwatchLap.getLapDurationMillis();
            this.mAccumulatedLapDurationMillis =
                    stopwatchLap.getAccumulatedLapDurationMillis();
        }

        /** Sets the position of the current {@link StopwatchLap}, starting at 1. */
        @NonNull
        public T setLapNumber(int lapNumber) {
            Preconditions.checkArgument(lapNumber >= 1, "Lap number must start at 1");
            mLapNumber = lapNumber;
            return (T) this;
        }

        /**
         * Sets the total duration in milliseconds accumulated by the current {@link StopwatchLap}.
         */
        @NonNull
        public T setLapDurationMillis(long lapDurationMillis) {
            mLapDurationMillis = lapDurationMillis;
            return (T) this;
        }

        /**
         * Sets the total duration in milliseconds accumulated by all the {@link StopwatchLap}
         * instances up to and including this one.
         */
        @NonNull
        public T setAccumulatedLapDurationMillis(long accumulatedLapDurationMillis) {
            mAccumulatedLapDurationMillis = accumulatedLapDurationMillis;
            return (T) this;
        }

        /** Builds the {@link StopwatchLap}. */
        @NonNull
        @Override
        public StopwatchLap build() {
            return new StopwatchLap(mNamespace, mId, mDocumentScore, mCreationTimestampMillis,
                    mDocumentTtlMillis, mName, mAlternateNames, mDescription, mImage, mUrl,
                    mPotentialActions,
                    mLapNumber, mLapDurationMillis, mAccumulatedLapDurationMillis);
        }
    }
}
