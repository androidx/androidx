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
 * A {@link WorkRequest} for non-repeating work.
 * <p>
 * OneTimeWorkRequests can be put in simple or complex graphs of work by using methods like
 * {@link WorkManager#beginWith(OneTimeWorkRequest)} or {@link WorkManager#beginWith(List)}.
 */
public final class OneTimeWorkRequest extends WorkRequest {

    /**
     * Creates an array of {@link OneTimeWorkRequest} with defaults from an array of
     * {@link ListenableWorker} class names.
     *
     * @param workerClasses An array of {@link ListenableWorker} class names
     * @return A list of {@link OneTimeWorkRequest} constructed by using defaults in the
     * {@link Builder}
     */
    @SafeVarargs public static @NonNull List<OneTimeWorkRequest> from(
            @NonNull Class<? extends ListenableWorker>... workerClasses) {
        return from(Arrays.asList(workerClasses));
    }

    /**
     * Creates a list of {@link OneTimeWorkRequest} with defaults from an array of
     * {@link ListenableWorker} class names.
     *
     * @param workerClasses A list of {@link ListenableWorker} class names
     * @return A list of {@link OneTimeWorkRequest} constructed by using defaults in the {@link
     * Builder}
     */
    public static @NonNull List<OneTimeWorkRequest> from(
            @NonNull List<Class<? extends ListenableWorker>> workerClasses) {
        List<OneTimeWorkRequest> workList = new ArrayList<>(workerClasses.size());
        for (Class<? extends ListenableWorker> workerClass : workerClasses) {
            workList.add(new OneTimeWorkRequest.Builder(workerClass).build());
        }
        return workList;
    }

    OneTimeWorkRequest(Builder builder) {
        super(builder.mId, builder.mWorkSpec, builder.mTags);
    }

    /**
     * Builder for {@link OneTimeWorkRequest}s.
     */
    public static final class Builder extends WorkRequest.Builder<Builder, OneTimeWorkRequest> {

        /**
         * Creates a {@link OneTimeWorkRequest}.
         *
         * @param workerClass The {@link ListenableWorker} class to run for this work
         */
        public Builder(@NonNull Class<? extends ListenableWorker> workerClass) {
            super(workerClass);
            mWorkSpec.inputMergerClassName = OverwritingInputMerger.class.getName();
        }

        /**
         * Sets an initial delay for the {@link OneTimeWorkRequest}.
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
         * Sets an initial delay for the {@link OneTimeWorkRequest}.
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
         * Specifies the {@link InputMerger} class name for this {@link OneTimeWorkRequest}.
         * <p>
         * Before workers run, they receive input {@link Data} from their parent workers, as well as
         * anything specified directly to them via {@link WorkRequest.Builder#setInputData(Data)}.
         * An InputMerger takes all of these objects and converts them to a single merged
         * {@link Data} to be used as the worker input.  The default InputMerger is
         * {@link OverwritingInputMerger}.  This library also offers
         * {@link ArrayCreatingInputMerger}; you can also specify your own.
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
        @NonNull OneTimeWorkRequest buildInternal() {
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
