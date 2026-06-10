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

import androidx.annotation.OptIn;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

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

    /**
     * Constructor for {@link StopwatchLap}.
     *
     * @param builder The builder to construct the {@link StopwatchLap} from.
     */
    @ExperimentalAppSearchApi
    public StopwatchLap(@NonNull BuilderBase<?> builder) {
        super(builder);
        mLapNumber = builder.mLapNumber;
        mLapDurationMillis = builder.mLapDurationMillis;
        mAccumulatedLapDurationMillis = builder.mAccumulatedLapDurationMillis;
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
    @Document.BuilderProducer
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static final class Builder extends BuilderBase<Builder> {
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
    @ExperimentalAppSearchApi
    public static class BuilderBase<T extends BuilderBase<T>> extends Thing.BuilderBase<T> {
        private int mLapNumber;
        private long mLapDurationMillis;
        private long mAccumulatedLapDurationMillis;

        /**
         * Constructor for {@link StopwatchLap.BuilderBase}.
         *
         * @param namespace Namespace for the Document. See {@link Document.Namespace}.
         * @param id Unique identifier for the Document. See {@link Document.Id}.
         */
        public BuilderBase(@NonNull String namespace, @NonNull String id) {
            super(namespace, id);
        }

        /**
         * Constructor for {@link StopwatchLap.BuilderBase} with all the existing values.
         *
         * @param stopwatchLap The existing {@link StopwatchLap} to copy values from.
         */
        public BuilderBase(@NonNull StopwatchLap stopwatchLap) {
            super(stopwatchLap);

            this.mLapNumber = stopwatchLap.getLapNumber();
            this.mLapDurationMillis = stopwatchLap.getLapDurationMillis();
            this.mAccumulatedLapDurationMillis =
                    stopwatchLap.getAccumulatedLapDurationMillis();
        }

        /** Sets the position of the current {@link StopwatchLap}, starting at 1. */
        public @NonNull T setLapNumber(int lapNumber) {
            Preconditions.checkArgument(lapNumber >= 1, "Lap number must start at 1");
            mLapNumber = lapNumber;
            return (T) this;
        }

        /**
         * Sets the total duration in milliseconds accumulated by the current {@link StopwatchLap}.
         */
        public @NonNull T setLapDurationMillis(long lapDurationMillis) {
            mLapDurationMillis = lapDurationMillis;
            return (T) this;
        }

        /**
         * Sets the total duration in milliseconds accumulated by all the {@link StopwatchLap}
         * instances up to and including this one.
         */
        public @NonNull T setAccumulatedLapDurationMillis(long accumulatedLapDurationMillis) {
            mAccumulatedLapDurationMillis = accumulatedLapDurationMillis;
            return (T) this;
        }

        /** Builds the {@link StopwatchLap}. */
        @Override
        public @NonNull StopwatchLap build() {
            return new StopwatchLap(this);
        }
    }
}
