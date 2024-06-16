/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.CameraProviderInitRetryPolicy;
import androidx.camera.core.impl.RetryPolicyInternal;
import androidx.camera.core.impl.TimeoutRetryPolicy;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Defines a strategy for retrying upon initialization failures of the {@link CameraProvider}.
 * When the initialization task is interrupted by an error or exception during execution, the
 * task will determine whether to be rescheduled based on the specified {@link RetryPolicy}.
 *
 * <p>Several predefined retry policies are available:
 * <ul>
 * <li>{@link RetryPolicy#NEVER}: Never retries the initialization.</li>
 * <li>{@link RetryPolicy#DEFAULT}: The default retry policy, which retries initialization up to
 *   a maximum timeout of {@link #getDefaultRetryTimeoutInMillis()}, providing a general-purpose
 *   approach for handling most errors.</li>
 * <li>{@link RetryPolicy#RETRY_UNAVAILABLE_CAMERA}: The retry policy automatically retries upon
 *   encountering errors like the {@link #DEFAULT} policy, and specifically designed to handle
 *   potential device inconsistencies in reporting available camera instances. In cases where the
 *   initial camera configuration fails due to the device underreporting (i.e., not accurately
 *   disclosing all available camera instances) the policy proactively triggers a
 *   reinitialization attempt. If the cameras are not successfully configured within
 *   {@link #getDefaultRetryTimeoutInMillis()}, the initialization is considered a failure.</li>
 * </ul>
 * <p>If no {@link RetryPolicy} is specified, {@link RetryPolicy#DEFAULT} will be used as
 * the default.
 *
 * <p>Examples of configuring the {@link RetryPolicy}:
 * <pre>{@code
 * // Configuring {@link RetryPolicy#RETRY_UNAVAILABLE_CAMERA} to the CameraXConfig
 * ProcessCameraProvider.configureInstance(
 *    CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
 *      .setCameraProviderInitRetryPolicy(RetryPolicy.RETRY_UNAVAILABLE_CAMERA)
 *      .build()
 * );
 * ...
 * }</pre>
 *
 * <p>Configuring a customized {@link RetryPolicy}:
 * <pre>{@code
 * ProcessCameraProvider.configureInstance(CameraXConfig.Builder.fromConfig(
 *         Camera2Config.defaultConfig()).setCameraProviderInitRetryPolicy(
 *         executionState -> {
 *             if (executionState.getExecutedTimeInMillis() > 10000L
 *                     || executionState.getNumOfAttempts() > 10
 *                     || executionState.getStatus() == ExecutionState.STATUS_CONFIGURATION_FAIL) {
 *                 return RetryConfig.NOT_RETRY;
 *             } else if (executionState.getStatus() == ExecutionState.STATUS_CAMERA_UNAVAILABLE) {
 *                 return RetryConfig.DEFAULT_DELAY_RETRY;
 *             } else {
 *                 Log.d("CameraX", "Unknown error occur: " + executionState.getCause());
 *                 return RetryConfig.MINI_DELAY_RETRY;
 *             }
 *         }).build());
 * ...
 * }</pre>
 * In the second example, the custom retry policy retries the initialization up to 10 times or
 * for a maximum of 10 seconds. If an unknown error occurs, the retry policy delays the next
 * retry after a delay defined by {@link RetryConfig#MINI_DELAY_RETRY}. The retry process
 * stops if the status is {@link ExecutionState#STATUS_CONFIGURATION_FAIL}. For
 * {@link ExecutionState#STATUS_CAMERA_UNAVAILABLE}, the retry policy applies
 * {@link RetryConfig#DEFAULT_DELAY_RETRY}.
 */
@ExperimentalRetryPolicy
public interface RetryPolicy {

    /**
     * Default timeout in milliseconds of the initialization.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    long DEFAULT_RETRY_TIMEOUT_IN_MILLIS = 6000L;

    /**
     * A retry policy that prevents any retry attempts and
     * immediately halts the initialization upon encountering an error.
     */
    @NonNull
    RetryPolicy NEVER = executionState -> RetryConfig.NOT_RETRY;

    /**
     * This retry policy increases initialization success by automatically retrying upon
     * encountering errors.
     *
     * <p>By default, it continues retrying for a maximum of
     * {@link #getDefaultRetryTimeoutInMillis()} milliseconds. To adjust the timeout duration to
     * specific requirements, utilize {@link RetryPolicy.Builder}.
     *
     * <p>Example: Create a policy based on {@link #DEFAULT} with a 10-second timeout:
     * <pre>{@code
     * RetryPolicy customTimeoutPolicy =
     *     new RetryPolicy.Builder(RetryPolicy.DEFAULT).setTimeoutInMillis(10000L).build();
     * }</pre>
     */
    @NonNull
    RetryPolicy DEFAULT = new CameraProviderInitRetryPolicy.Legacy(
            getDefaultRetryTimeoutInMillis());

    /**
     * This retry policy automatically retries upon encountering errors and specifically
     * designed to handle potential device inconsistencies in reporting available camera
     * instances. If the initial camera configuration fails due to underreporting, the policy
     * automatically attempts reinitialization.
     * <p>By default, it perseveres for a maximum of {@link #getDefaultRetryTimeoutInMillis()}
     * milliseconds before conceding initialization as unsuccessful. For finer control over the
     * timeout duration, utilize {@link RetryPolicy.Builder}.
     * <p>Example: Create a policy based on {@link #RETRY_UNAVAILABLE_CAMERA} with a 10-second
     * timeout:
     * <pre>{@code
     * RetryPolicy customTimeoutPolicy = new RetryPolicy.Builder(
     *     RetryPolicy.RETRY_UNAVAILABLE_CAMERA).setTimeoutInMillis(10000L).build();
     * }</pre>
     */
    @NonNull
    RetryPolicy RETRY_UNAVAILABLE_CAMERA =
            new CameraProviderInitRetryPolicy(getDefaultRetryTimeoutInMillis());

    /**
     * Retrieves the default timeout value, in milliseconds, used for initialization retries.
     *
     * @return The default timeout duration, expressed in milliseconds. This value determines the
     * maximum time to wait for a successful initialization before considering the process as
     * failed.
     */
    static long getDefaultRetryTimeoutInMillis() {
        return DEFAULT_RETRY_TIMEOUT_IN_MILLIS;
    }

    /**
     * Called to request a decision on whether to retry the initialization process.
     *
     * @param executionState Information about the current execution state of the camera
     *                       initialization.
     * @return A RetryConfig indicating whether to retry, along with any associated delay.
     */
    @NonNull
    RetryConfig onRetryDecisionRequested(@NonNull ExecutionState executionState);

    /**
     * Returns the maximum allowed retry duration in milliseconds. Initialization will
     * be terminated if retries take longer than this timeout. A value of 0 indicates
     * no timeout is enforced.
     *
     * <p>The default value is 0 (no timeout).
     *
     * @return The retry timeout in milliseconds.
     */
    default long getTimeoutInMillis() {
        return 0;
    }

    /**
     * A builder class for customizing RetryPolicy behavior.
     *
     * <p>Use the {@link #Builder(RetryPolicy)} to modify existing RetryPolicy instances,
     * typically starting with the predefined options like {@link RetryPolicy#DEFAULT} or
     * {@link RetryPolicy#RETRY_UNAVAILABLE_CAMERA}. The most common use is to set a custom timeout.
     *
     * <p>Example: Create a policy based on {@link RetryPolicy#DEFAULT} with a 10-second timeout:
     * <pre>{@code
     * new RetryPolicy.Builder(RetryPolicy.DEFAULT).setTimeoutInMillis(10000L).build()
     * }</pre>
     */
    @ExperimentalRetryPolicy
    final class Builder {

        private final RetryPolicy mBasePolicy;
        private long mTimeoutInMillis;

        /**
         * Creates a builder based on an existing {@link RetryPolicy}.
         *
         * <p>This allows you to start with a predefined policy and add further customizations.
         * For example, set a timeout to prevent retries from continuing endlessly.
         *
         * @param basePolicy The RetryPolicy to use as a starting point.
         */
        public Builder(@NonNull RetryPolicy basePolicy) {
            mBasePolicy = basePolicy;
            mTimeoutInMillis = basePolicy.getTimeoutInMillis();
        }

        /**
         * Sets a timeout in milliseconds. If retries exceed this duration, they will be
         * terminated with {@link RetryConfig#NOT_RETRY}.
         *
         * @param timeoutInMillis The maximum duration for retries in milliseconds. A value of 0
         *                        indicates no timeout.
         * @return {@code this} for method chaining.
         */
        @NonNull
        public Builder setTimeoutInMillis(long timeoutInMillis) {
            mTimeoutInMillis = timeoutInMillis;
            return this;
        }

        /**
         * Creates the customized {@link RetryPolicy} instance.
         *
         * @return The new {@link RetryPolicy}.
         */
        @NonNull
        public RetryPolicy build() {
            if (mBasePolicy instanceof RetryPolicyInternal) {
                return ((RetryPolicyInternal) mBasePolicy).copy(mTimeoutInMillis);
            }
            return new TimeoutRetryPolicy(mTimeoutInMillis, mBasePolicy);
        }
    }

    /**
     * Provides insights into the current execution state of the camera initialization process.
     *
     * <p>The ExecutionState empowers app developers to make informed decisions about retry
     * strategies and fine-tune timeout settings. It also facilitates comprehensive app-level
     * logging for tracking initialization success/failure rates and overall camera performance.
     *
     * <p>Key use cases:
     * <ul>
     *   <li>Determining whether to retry initialization based on specific error conditions.</li>
     *   <li>Logging detailed execution information for debugging and analysis.</li>
     *   <li>Monitoring retry success/failure rates to identify potential issues.</li>
     * </ul>
     */
    @ExperimentalRetryPolicy
    interface ExecutionState {

        /**
         * Defines the status codes for the {@link ExecutionState}.
         */
        @IntDef({STATUS_UNKNOWN_ERROR, STATUS_CONFIGURATION_FAIL, STATUS_CAMERA_UNAVAILABLE})
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Retention(RetentionPolicy.SOURCE)
        @interface Status {
        }

        /**
         * Indicates that the initialization encountered an unknown error. This error may be
         * transient, and retrying may resolve the issue.
         */
        int STATUS_UNKNOWN_ERROR = 0;

        /**
         * Indicates that initialization of CameraX failed due to invalid customization options
         * within the provided {@link CameraXConfig}. This error typically indicates a problem
         * with the configuration settings and may require developer attention to resolve.
         *
         * <p>Possible Causes:
         * <ul>
         *  <li>Incorrect or incompatible settings within the {@link CameraXConfig}.</li>
         *  <li>Conflicting options that prevent successful initialization.</li>
         *  <li>Missing or invalid values for essential configuration parameters.</li>
         * </ul>
         *
         * <p>To troubleshoot:
         * Please see {@code getCause().getMessage()} for details, and review configuration of
         * the {@link CameraXConfig} to identify potential errors or inconsistencies in the
         * customization options.
         *
         * @see androidx.camera.lifecycle.ProcessCameraProvider#configureInstance(CameraXConfig)
         * @see CameraXConfig.Builder
         */
        int STATUS_CONFIGURATION_FAIL = 1;

        /**
         * Indicates that the {@link CameraProvider} failed to initialize due to an unavailable
         * camera.
         * This error suggests a temporary issue with the device's cameras, and retrying may
         * resolve the issue.
         */
        int STATUS_CAMERA_UNAVAILABLE = 2;

        /**
         * Retrieves the status of the most recently completed initialization attempt.
         *
         * @return The status code representing the outcome of the initialization.
         */
        @Status
        int getStatus();

        /**
         * Gets the cause that occurred during the task execution, if any.
         *
         * @return The cause that occurred during the task execution, or null if there was no error.
         */
        @Nullable
        Throwable getCause();

        /**
         * Gets the total execution time of the initialization task in milliseconds.
         *
         * @return The total execution time of the initialization task in milliseconds.
         */
        long getExecutedTimeInMillis();

        /**
         * Indicates the total number of attempts made to initialize the camera, including the
         * current attempt. The count starts from 1.
         *
         * @return The total number of attempts made to initialize the camera.
         */
        int getNumOfAttempts();
    }

    /**
     * Represents the outcome of a {@link RetryPolicy} decision.
     */
    @ExperimentalRetryPolicy
    final class RetryConfig {

        private static final long MINI_DELAY_MILLIS = 100L;
        private static final long DEFAULT_DELAY_MILLIS = 500L;

        /** A RetryConfig indicating that no further retries should be attempted. */
        @NonNull
        public static final RetryConfig NOT_RETRY = new RetryConfig(false, 0L);

        /**
         * A RetryConfig indicating that the initialization should be retried after the default
         * delay (determined by {@link #getDefaultRetryDelayInMillis()}). This delay provides
         * sufficient time for typical device recovery processes, balancing retry efficiency
         * and minimizing user wait time.
         */
        @NonNull
        public static final RetryConfig DEFAULT_DELAY_RETRY = new RetryConfig(true);

        /**
         * A RetryConfig indicating that the initialization should be retried after a minimum
         * delay of 100 milliseconds.
         *
         * This short delay serves two purposes:
         * <ul>
         * <li>Reduced Latency: Minimizes the wait time for the camera to become available,
         * improving user experience.
         * <li>Camera Self-Recovery: Provides a brief window for the camera to potentially
         * recover from temporary issues.
         * </ul>
         * This approach balances quick retries with potential self-recovery, aiming for the
         * fastest possible camera restoration.
         */
        @NonNull
        public static final RetryConfig MINI_DELAY_RETRY = new RetryConfig(true, MINI_DELAY_MILLIS);

        /**
         * A RetryConfig indicating that the initialization should be considered complete
         * without retrying. This config is intended for internal use and is not intended to
         * trigger further retries. It represents the legacy behavior of not failing the
         * initialization task for minor issues.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @NonNull
        public static RetryConfig COMPLETE_WITHOUT_FAILURE = new RetryConfig(false, 0, true);

        /**
         * Returns the recommended default delay to optimize retry attempts and camera recovery.
         *
         * @return The default delay value, carefully calibrated based on extensive lab testing to:
         * <ul>
         *  <li>Provide sufficient time for typical device recovery processes in common bad
         *      camera states.</li>
         *  <li>Strike an optimal balance between minimizing latency and avoiding excessive retries,
         *      ensuring efficient camera initialization without unnecessary overhead.</li>
         * </ul>
         * This value represents the generally recommended delay for most scenarios, striking a
         * balance between providing adequate time for camera recovery and maintaining a smooth
         * user experience.
         */
        public static long getDefaultRetryDelayInMillis() {
            return DEFAULT_DELAY_MILLIS;
        }

        private final long mDelayInMillis;
        private final boolean mShouldRetry;
        private final boolean mCompleteWithoutFailure;

        private RetryConfig(boolean shouldRetry) {
            this(shouldRetry, RetryConfig.getDefaultRetryDelayInMillis());
        }

        private RetryConfig(boolean shouldRetry, long delayInMillis) {
            this(shouldRetry, delayInMillis, false);
        }

        /**
         * Constructor for determining whether to retry the initialization.
         *
         * @param shouldRetry            Whether to retry the initialization.
         * @param delayInMillis          The delay time in milliseconds before starting the next
         *                               retry.
         * @param completeWithoutFailure Indicates whether to skip retries and not fail the
         *                               initialization. This is a flag for legacy behavior to
         *                               avoid failing the initialization task for minor issues.
         *                               When this flag is set to true, `shouldRetry` must be
         *                               false.
         */
        private RetryConfig(boolean shouldRetry, long delayInMillis,
                boolean completeWithoutFailure) {
            mShouldRetry = shouldRetry;
            mDelayInMillis = delayInMillis;
            if (completeWithoutFailure) {
                Preconditions.checkArgument(!shouldRetry,
                        "shouldRetry must be false when completeWithoutFailure is set to true");
            }
            mCompleteWithoutFailure = completeWithoutFailure;
        }

        /**
         * Determines whether the initialization should be retried.
         *
         * @return true if the initialization should be retried, false otherwise.
         */
        public boolean shouldRetry() {
            return mShouldRetry;
        }

        /**
         * Gets the delay time in milliseconds before the next retry attempt should be made.
         *
         * @return The delay time in milliseconds.
         */
        public long getRetryDelayInMillis() {
            return mDelayInMillis;
        }

        /**
         * Signals to treat initialization errors as successful for legacy behavior compatibility.
         *
         * <p>This config is intended for internal use and is not intended to trigger further
         * retries.
         *
         * @return true if initialization should be deemed complete without additional retries,
         * despite any errors encountered. Otherwise, returns false.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public boolean shouldCompleteWithoutFailure() {
            return mCompleteWithoutFailure;
        }

        /**
         * A builder class for creating and customizing {@link RetryConfig} objects.
         *
         * <p>While predefined configs like {@link RetryConfig#DEFAULT_DELAY_RETRY} are
         * recommended for typical recovery scenarios, this builder allows for fine-tuned control
         * when specific requirements necessitate a different approach.
         */
        @ExperimentalRetryPolicy
        public static final class Builder {

            private boolean mShouldRetry = true;
            private long mTimeoutInMillis = RetryConfig.getDefaultRetryDelayInMillis();

            /**
             * Specifies whether a retry should be attempted.
             *
             * @param shouldRetry  If true (the default), initialization will be retried.
             *                     If false, initialization will not be retried.
             * @return {@code this} for method chaining.
             */
            @NonNull
            public Builder setShouldRetry(boolean shouldRetry) {
                mShouldRetry = shouldRetry;
                return this;
            }

            /**
             * Sets the retry delay in milliseconds.
             *
             * <p>If set, the initialization will be retried after the specified delay. For optimal
             * results, the delay should be within the range of 100 to 2000 milliseconds. This
             * aligns with lab testing, which suggests this range provides sufficient recovery
             * time for most common camera issues while minimizing latency.
             *
             * @param timeoutInMillis The delay in milliseconds.
             * @return {@code this} for method chaining.
             */
            @NonNull
            public Builder setRetryDelayInMillis(
                    @IntRange(from = 100, to = 2000) long timeoutInMillis) {
                mTimeoutInMillis = timeoutInMillis;
                return this;
            }

            /**
             * Builds the customized {@link RetryConfig} object.
             *
             * @return The configured RetryConfig.
             */
            @NonNull
            public RetryConfig build() {
                return new RetryConfig(mShouldRetry, mTimeoutInMillis);
            }
        }
    }
}
