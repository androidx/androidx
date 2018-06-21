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

package androidx.work;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A class that represents a request for non-repeating work.
 */

public final class OneTimeWorkRequest extends WorkRequest {

    /**
     * Creates an array of {@link OneTimeWorkRequest} with defaults from an array of
     * {@link Worker} class names.
     *
     * @param workerClasses An array of {@link Worker} class names
     * @return A list of {@link OneTimeWorkRequest} constructed by using defaults in the
     * {@link Builder}
     */
    @SafeVarargs public static @NonNull List<OneTimeWorkRequest> from(
            @NonNull Class<? extends Worker>... workerClasses) {
        return from(Arrays.asList(workerClasses));
    }

    /**
     * Creates a list of {@link OneTimeWorkRequest} with defaults from an array of {@link Worker}
     * class names.
     *
     * @param workerClasses A list of {@link Worker} class names
     * @return A list of {@link OneTimeWorkRequest} constructed by using defaults in the {@link
     * Builder}
     */
    public static @NonNull List<OneTimeWorkRequest> from(
            @NonNull List<Class<? extends Worker>> workerClasses) {
        List<OneTimeWorkRequest> workList = new ArrayList<>(workerClasses.size());
        for (Class<? extends Worker> workerClass : workerClasses) {
            workList.add(new OneTimeWorkRequest.Builder(workerClass).build());
        }
        return workList;
    }

    OneTimeWorkRequest(Builder builder) {
        super(builder.mId, builder.mWorkSpec, builder.mTags);
    }

    /**
     * Builder for {@link OneTimeWorkRequest} class.
     */
    public static final class Builder extends WorkRequest.Builder<Builder, OneTimeWorkRequest> {

        public Builder(@NonNull Class<? extends Worker> workerClass) {
            super(workerClass);
            mWorkSpec.inputMergerClassName = OverwritingInputMerger.class.getName();
        }

        /**
         * Add an initial delay to the {@link OneTimeWorkRequest}.
         *
         * @param duration The length of the delay in {@code timeUnit} units
         * @param timeUnit The units of time for {@code duration}
         * @return The current {@link Builder}
         */
        public @NonNull Builder setInitialDelay(long duration, @NonNull TimeUnit timeUnit) {
            mWorkSpec.initialDelay = timeUnit.toMillis(duration);
            return this;
        }

        /**
         * Add an initial delay to the {@link OneTimeWorkRequest}.
         *
         * @param duration The length of the delay
         * @return The current {@link Builder}
         */
        @RequiresApi(26)
        public @NonNull Builder setInitialDelay(@NonNull Duration duration) {
            mWorkSpec.initialDelay = duration.toMillis();
            return this;
        }

        /**
         * Specify the {@link InputMerger} class name for this {@link OneTimeWorkRequest}.  An
         * InputMerger takes one or more {@link Data} inputs to a {@link Worker} and converts
         * them to a single merged {@link Data} to be used as its input.
         * The default InputMerger is {@link OverwritingInputMerger}.
         *
         * @param inputMerger The class name of the {@link InputMerger} for this
         *                    {@link OneTimeWorkRequest}
         * @return The current {@link Builder}
         */
        public @NonNull Builder setInputMerger(@NonNull Class<? extends InputMerger> inputMerger) {
            mWorkSpec.inputMergerClassName = inputMerger.getName();
            return this;
        }


        @Override
        public @NonNull OneTimeWorkRequest build() {
            if (mBackoffCriteriaSet
                    && Build.VERSION.SDK_INT >= 23
                    && mWorkSpec.constraints.requiresDeviceIdle()) {
                throw new IllegalArgumentException(
                        "Cannot set backoff criteria on an idle mode job");
            }
            return new OneTimeWorkRequest(this);
        }

        @Override
        @NonNull Builder getThis() {
            return this;
        }
    }
}
