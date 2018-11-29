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

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import java.util.concurrent.TimeUnit;

/**
 * The result of a {@link ListenableWorker}'s computation. Call {@link #success()},
 * {@link #failure()}, or {@link #retry()} or one of their variants to generate an object
 * indicating what happened in your background work.
 */
public abstract class Result {
    /**
     * Returns an instance of {@link Result} that can be used to indicate that the work completed
     * successfully. Any work that depends on this can be executed as long as all of its other
     * dependencies and constraints are met.
     *
     * @return An instance of {@link Result} indicating successful execution of work
     */
    @NonNull
    public static Result success() {
        return new Success();
    }

    /**
     * Returns an instance of {@link Result} that can be used to indicate that the work completed
     * successfully. Any work that depends on this can be executed as long as all of its other
     * dependencies and constraints are met.
     *
     * @param outputData A {@link Data} object that will be merged into the input Data of any
     *                   OneTimeWorkRequest that is dependent on this work
     * @return An instance of {@link Result} indicating successful execution of work
     */
    @NonNull
    public static Result success(@NonNull Data outputData) {
        return new Success(outputData);
    }

    /**
     * Returns an instance of {@link Result} that can be used to indicate that the work encountered
     * a transient failure and should be retried with backoff specified in
     * {@link WorkRequest.Builder#setBackoffCriteria(BackoffPolicy, long, TimeUnit)}.
     *
     * @return An instance of {@link Result} indicating that the work needs to be retried
     */
    @NonNull
    public static Result retry() {
        return new Retry();
    }

    /**
     * Returns an instance of {@link Result} that can be used to indicate that the work completed
     * with a permanent failure. Any work that depends on this will also be marked as failed and
     * will not be run. <b>If you need child workers to run, you need to use {@link #success()} or
     * {@link #success(Data)}</b>; failure indicates a permanent stoppage of the chain of work.
     *
     * @return An instance of {@link Result} indicating failure when executing work
     */
    @NonNull
    public static Result failure() {
        return new Failure();
    }

    /**
     * Returns an instance of {@link Result} that can be used to indicate that the work completed
     * with a permanent failure. Any work that depends on this will also be marked as failed and
     * will not be run. <b>If you need child workers to run, you need to use {@link #success()} or
     * {@link #success(Data)}</b>; failure indicates a permanent stoppage of the chain of work.
     *
     * @param outputData A {@link Data} object that can be used to keep track of why the work
     *                   failed
     * @return An instance of {@link Result} indicating failure when executing work
     */
    @NonNull
    public static Result failure(@NonNull Data outputData) {
        return new Failure(outputData);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    Result() {
        // Restricting access to the constructor, to give Result a sealed class
        // like behavior.
    }

    /**
     * Used to indicate that the work completed successfully.  Any work that depends on this
     * can be executed as long as all of its other dependencies and constraints are met.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final class Success extends Result {
        private final Data mOutputData;

        public Success() {
            this(Data.EMPTY);
        }

        /**
         * @param outputData A {@link Data} object that will be merged into the input Data of any
         *                   OneTimeWorkRequest that is dependent on this work
         */
        public Success(@NonNull Data outputData) {
            super();
            mOutputData = outputData;
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public Data getOutputData() {
            return mOutputData;
        }
    }

    /**
     * Used to indicate that the work completed with a permanent failure.  Any work that depends on
     * this will also be marked as failed and will not be run. <b>If you need child workers to run,
     * you need to return {@link Success}</b>; failure indicates a permanent stoppage of the chain
     * of work.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final class Failure extends Result {
        private final Data mOutputData;

        public Failure() {
            this(Data.EMPTY);
        }

        /**
         * @param outputData A {@link Data} object that can be used to keep track of why the work
         *                   failed
         */
        public Failure(@NonNull Data outputData) {
            super();
            mOutputData = outputData;
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public Data getOutputData() {
            return mOutputData;
        }
    }

    /**
     * Used to indicate that the work encountered a transient failure and should be retried with
     * backoff specified in
     * {@link WorkRequest.Builder#setBackoffCriteria(BackoffPolicy, long, TimeUnit)}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final class Retry extends Result {
        public Retry() {
            super();
        }
    }
}
