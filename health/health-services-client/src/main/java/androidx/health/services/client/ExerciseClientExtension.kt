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
package androidx.health.services.client

import androidx.health.services.client.data.BatchingMode
import androidx.health.services.client.data.DataPoint
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseCapabilities
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseEndReason
import androidx.health.services.client.data.ExerciseGoal
import androidx.health.services.client.data.ExerciseInfo
import androidx.health.services.client.data.ExerciseState
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseTypeConfig
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.WarmUpConfig

/**
 * Prepares for a new exercise.
 *
 * Once called, Health Services will warmup the sensors based on the [ExerciseType] and
 * requested [DataType]s.
 *
 * If the calling app already has an active exercise in progress or if it does not have the
 * required permissions, then this call throws [HealthServicesException]. If another app owns the
 * active exercise then this call will succeed.
 *
 * Sensors available for warmup are GPS [DataType.LOCATION] and HeartRate
 * [DataType.HEART_RATE_BPM]. Other [DataType]s requested for warmup based on exercise
 * capabilities will be a no-op for the prepare stage.
 *
 * The DataType availability can be obtained through the
 * [ExerciseUpdateCallback.onAvailabilityChanged] callback. [ExerciseUpdate]s with the supported
 * DataType [DataPoint] will also be returned in the [ExerciseState.PREPARING] state, though no
 * aggregation will occur until the exercise is started.
 *
 * If an app is actively preparing and another app starts tracking an active exercise then the
 * preparing app should expect to receive an [ExerciseUpdate] with [ExerciseState.ENDED] along with
 * the reason [ExerciseEndReason.AUTO_END_SUPERSEDED] to the [ExerciseUpdateCallback] indicating
 * that their session has been superseded and ended. At that point no additional updates to
 * availability or data will be sent until the app calls prepareExercise again.
 *
 * @param configuration the [WarmUpConfig] containing the desired exercise and data types
 * @throws HealthServicesException if the calling app already has an active exercise in progress or
 * if Health Service fails to process the call
 * @throws SecurityException if the calling app does not have the required permissions
 */
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun ExerciseClient.prepareExercise(configuration: WarmUpConfig) {
    prepareExerciseAsync(configuration).awaitWithException()
}

/**
 * Starts a new exercise.
 *
 * Once started, Health Services will begin collecting data associated with the exercise.
 *
 * Since Health Services only allows a single active exercise at a time, this will terminate any
 * active exercise currently in progress before starting the new one. If this occurs, clients
 * can expect to receive an [ExerciseUpdate] with [ExerciseState.ENDED], indicating that
 * their exercise has been superseded and that no additional updates will be sent. Clients can
 * use [getCurrentExerciseInfo] (described below) to check if they or another app has an
 * active exercise in-progress.
 *
 * The exercise will be terminated and clients can expect to receive an [ExerciseUpdate] with
 * [ExerciseState.ENDED] along with the reason [ExerciseEndReason.AUTO_END_MISSING_LISTENER]
 * (indicating that their exercise has been automatically ended due to the lack of callback) if
 * there is ever a five minute period where no [ExerciseUpdateCallback] is registered. A notable
 * example is if the process with the registered [ExerciseUpdateCallback] dies and does not
 * re-register the [ExerciseUpdateCallback] within five minutes.
 *
 * Clients should only request [ExerciseType]s, [DataType]s, goals, and auto-pause enabled that
 * matches the [ExerciseCapabilities] returned by [getCapabilities] since Health Services
 * will reject requests asking for unsupported configurations.
 *
 * @param configuration the [ExerciseConfig] describing this exercise
 * @throws HealthServicesException if Health Service fails to process the call
 * @throws SecurityException if the calling app does not have the required permissions
 */
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun ExerciseClient.startExercise(configuration: ExerciseConfig) {
    startExerciseAsync(configuration).awaitWithException()
}

/**
 * Pauses the current exercise, if it is currently started.
 *
 * Before transitioning to [ExerciseState.USER_PAUSED], Health Services will flush and return
 * the sensor data. While the exercise is paused, active time and cumulative metrics such as
 * distance will not accumulate. Instantaneous measurements such as speed and heart rate will
 * continue to update if requested in the [ExerciseConfig].
 *
 * Note that GPS and other sensors may be stopped when the exercise is paused in order to
 * conserve battery. This may happen immediately, or after some time. (The exact behavior is
 * hardware dependent.) Should this happen, access will automatically resume when the exercise
 * is resumed.
 *
 * If the exercise is already paused then this method has no effect. If the exercise has ended
 * then [HealthServicesException] is thrown.
 *
 * @throws HealthServicesException if the exercise has ended or if Health Service fails to process
 * the call
 */
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun ExerciseClient.pauseExercise() = pauseExerciseAsync().awaitWithException()

/**
 * Resumes the current exercise, if it is currently paused.
 *
 * Once resumed active time and cumulative metrics such as distance will resume accumulating.
 *
 * If the exercise has been started but is not currently paused this method has no effect. If
 * the exercise has ended then [HealthServicesException] is thrown.
 *
 * @throws HealthServicesException if the exercise has ended or if Health Service fails to
 * process the call
 */
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun ExerciseClient.resumeExercise() = resumeExerciseAsync().awaitWithException()

/**
 * Ends the current exercise, if it has been started.
 *
 * Health Services will flush and then shut down the active sensors and return an
 * [ExerciseUpdate] with [ExerciseState.ENDED] along with the reason
 * [ExerciseEndReason.USER_END] to the [ExerciseUpdateCallback]. If the exercise has already
 * ended then this call fails with a [HealthServicesException].
 *
 * No additional metrics will be produced for the exercise and any on device persisted data
 * about the exercise will be deleted after the summary has been sent back.
 *
 * @throws HealthServicesException if exercise has already ended or if Health Service fails to
 * process the call
 */
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun ExerciseClient.endExercise() = endExerciseAsync().awaitWithException()

/**
 * Flushes the sensors for the active exercise. This call should be used sparingly and will be
 * subject to throttling by Health Services.
 *
 * @throws HealthServicesException if the Health Service fails to process the request
 */
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun ExerciseClient.flush() = flushAsync().awaitWithException()

/**
 * Ends the current lap, calls [ExerciseUpdateCallback.onLapSummaryReceived] with data spanning
 * the marked lap and starts a new lap. If the exercise supports laps this method can be called
 * at any point after an exercise has been started and before it has been ended regardless of
 * the exercise status.
 *
 * The metrics in the lap summary will start from either the start time of the exercise or the
 * last time a lap was marked to the time this method is being called.
 *
 * @throws HealthServicesException If there's no exercise being tracked or if Health Service fails
 * to process the call
 */
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun ExerciseClient.markLap() = markLapAsync().awaitWithException()

/**
 * Returns the current [ExerciseInfo].
 *
 * This can be used by clients to determine if they or another app already owns an active
 * exercise being tracked by Health Services. For example, if an app is killed and it learns it
 * owns the active exercise it can register a new [ExerciseUpdateCallback] and pick tracking up
 * from where it left off.
 *
 * @return a [ExerciseInfo] that contains information about the current exercise
 * @throws HealthServicesException if Health Service fails to process the call
 */
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun ExerciseClient.getCurrentExerciseInfo() =
    getCurrentExerciseInfoAsync().awaitWithException()

/**
 * Clears the callback set using [ExerciseClient.setUpdateCallback].
 *
 * If this callback is not already registered then this will be a no-op.
 *
 * @param callback the [ExerciseUpdateCallback] to clear
 * @throws HealthServicesException if Health Service fails to process the call
 */
@Suppress("ExecutorRegistration")
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun ExerciseClient.clearUpdateCallback(callback: ExerciseUpdateCallback) =
    clearUpdateCallbackAsync(callback).awaitWithException()

/**
 * Adds an [ExerciseGoal] for an active exercise.
 *
 * Goals apply to only active exercises owned by the client, and will be invalidated once the
 * exercise is complete.
 *
 * @param exerciseGoal the [ExerciseGoal] to add to this exercise
 * @throws HealthServicesException if Health Service fails to process the call
 */
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun ExerciseClient.addGoalToActiveExercise(exerciseGoal: ExerciseGoal<*>) =
    addGoalToActiveExerciseAsync(exerciseGoal).awaitWithException()

/**
 * Removes an exercise goal for an active exercise.
 *
 * Takes into account equivalent milestones (i.e. milestones which are not equal but are
 * different representation of a common milestone. e.g. milestone A for every 2kms, currently at
 * threshold of 10kms, and milestone B for every 2kms, currently at threshold of 8kms).
 *
 * @param exerciseGoal the [ExerciseGoal] to remove from this exercise
 * @throws HealthServicesException if Health Service fails to process the call
 */
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun ExerciseClient.removeGoalFromActiveExercise(exerciseGoal: ExerciseGoal<*>) =
    removeGoalFromActiveExerciseAsync(exerciseGoal).awaitWithException()

/**
 * Enables or disables auto pause/resume for the current exercise.
 *
 * @param enabled a boolean to indicate if should be enabled or disabled
 * @throws HealthServicesException if Health Service fails to process the call
 */
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun ExerciseClient.overrideAutoPauseAndResumeForActiveExercise(
    enabled: Boolean
) = overrideAutoPauseAndResumeForActiveExerciseAsync(enabled).awaitWithException()

/**
 * Sets the batching mode for the current exercise synchronously.
 *
 * @param batchingModes [BatchingMode] overrides for exercise updates. Passing an empty set will
 * clear all existing overrides.
 * @throws HealthServicesException if an exercise is not active for this app or Health Service fails
 * to process the call
 */
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun ExerciseClient.overrideBatchingModesForActiveExercise(
    batchingModes: Set<BatchingMode>
) = overrideBatchingModesForActiveExerciseAsync(batchingModes).awaitWithException()

/**
 * Returns the [ExerciseCapabilities] of this client for the device.
 *
 * This can be used to determine what [ExerciseType]s and [DataType]s this device supports.
 * Clients should use the capabilities to inform their requests since Health Services will
 * typically reject requests made for [DataType]s or features (such as auto-pause) which are not
 * enabled for the rejected [ExerciseType].
 *
 * @return a the [ExerciseCapabilities] for this device
 * @throws HealthServicesException if Health Service fails to process the call
 */
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun ExerciseClient.getCapabilities() = getCapabilitiesAsync().awaitWithException()

/**
 * Updates the configurable exercise type attributes for the current exercise.
 *
 * This can be used to update the configurable attributes for the ongoing exercise, as defined
 * in [ExerciseTypeConfig].
 *
 * @param exerciseTypeConfig configuration containing the new values for the configurable attributes
 * @throws HealthServicesException if Health Service fails to process the call
 */
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun ExerciseClient.updateExerciseTypeConfig(
    exerciseTypeConfig: ExerciseTypeConfig
) = updateExerciseTypeConfigAsync(exerciseTypeConfig).awaitWithException()
