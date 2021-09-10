/*
 * Copyright (C) 2021 The Android Open Source Project
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

import androidx.health.services.client.data.ExerciseCapabilities
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseGoal
import androidx.health.services.client.data.ExerciseInfo
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.WarmUpConfig
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

/** Client which provides a way to subscribe to the health data of a device during an exercise. */
public interface ExerciseClient {
    /**
     * Prepares for a new exercise.
     *
     * Once called, Health Services will warmup the sensors based on the [ExerciseType] and
     * requested [DataType]s
     *
     * If the calling app already has an active exercise in progress or if it does not have the
     * required permissions, then this call returns a failed future. If another app owns the active
     * exercise then this call will succeed.
     *
     * Sensors available for warmup are GPS [DataType.LOCATION] and HeartRate
     * [DataType.HEART_RATE_BPM]. Other [DataType]s requested for warmup based on exercise
     * capabilities will be a no-op for the prepare stage.
     *
     * The DataType availability can be obtained through the
     * [ExerciseUpdateListener.onAvailabilityChanged] callback. [ExerciseUpdate]s with the supported
     * DataType [DataPoint] will also be returned in the [ExerciseState#PREPARING] state, though no
     * aggregation will occur until the exercise is started.
     *
     * If an app is actively preparing and another app starts tracking an active exercise then the
     * preparing app should expect to receive an [ExerciseUpdate] with [ExerciseState#TERMINATED]
     * indicating that their session has been superseded and ended. At that point no additional
     * updates to availability or data will be sent until the app calls prepareExercise again.
     *
     * @return a [ListenableFuture] that completes once Health Services starts preparing the sensors
     * or fails due to missing permissions or the app owning another active exercise.
     */
    public fun prepareExercise(configuration: WarmUpConfig): ListenableFuture<Void>

    /**
     * Starts a new exercise.
     *
     * Once started, Health Services will begin collecting data associated with the exercise.
     *
     * Since Health Services only allows a single active exercise at a time, this will terminate any
     * active exercise currently in progress before starting the new one. If this occurs, clients
     * can expect to receive an [ExerciseUpdate] with [ExerciseState#TERMINATED], indicating that
     * their exercise has been superseded and that no additional updates will be sent. Clients can
     * use [currentExerciseInfo] (described below) to check if they or another app has an active
     * exercise in-progress.
     *
     * If the client fails to maintain a live [ExerciseStateListener] for at least five minutes
     * during the duration of the exercise, Health Services can decide to terminate the exercise. If
     * this occurs, clients can expect to receive an [ExerciseUpdate] with
     * [ExerciseState#AUTO_ENDED] indicating that their exercise has been automatically ended due to
     * the lack of listener.
     *
     * Clients should only request [ExerciseType]s, [DataType]s, goals, and auto-pause enabled that
     * matches the [ExerciseCapabilities] returned by [capabilities] since Health Services will
     * reject requests asking for unsupported configurations.
     *
     * @return a [ListenableFuture] that completes once the exercise has been started or fails due
     * to the application missing the required permissions or requesting metrics which are not
     * supported for the given [ExerciseType].
     */
    public fun startExercise(configuration: ExerciseConfig): ListenableFuture<Void>

    /**
     * Pauses the current exercise, if it is currently started.
     *
     * Before transitioning to [ExerciseState.PAUSED], Health Services will flush and return the
     * sensor data. While the exercise is paused, active time and cumulative metrics such as
     * distance will not accumulate. Instantaneous measurements such as speed and heart rate will
     * continue to update if requested in the [ExerciseConfig].
     *
     * Note that GPS and other sensors may be stopped when the exercise is paused in order to
     * conserve battery. This may happen immediately, or after some time. (The exact behavior is
     * hardware dependent.) Should this happen, access will automatically resume when the exercise
     * is resumed.
     *
     * If the exercise is already paused then this method has no effect. If the exercise has ended
     * then the returned future will fail.
     *
     * @return a [ListenableFuture] that completes once the exercise has been paused or fails if the
     * calling application does not own the active exercise.
     */
    public fun pauseExercise(): ListenableFuture<Void>

    /**
     * Resumes the current exercise, if it is currently paused.
     *
     * Once resumed active time and cumulative metrics such as distance will resume accumulating.
     *
     * If the exercise has been started but is not currently paused this method has no effect. If
     * the exercise has ended then the returned future will fail.
     *
     * @return a [ListenableFuture] that completes once the exercise has been resumed or fails if
     * the calling application does not own the active exercise.
     */
    public fun resumeExercise(): ListenableFuture<Void>

    /**
     * Ends the current exercise, if it has been started.
     *
     * Health Services will flush and then shut down the active sensors and return an
     * [ExerciseUpdate] with [ExerciseState#USER_ENDED] to the [ExerciseUpdateListener]. If the
     * exercise has ended then this future will fail.
     *
     * No additional metrics will be produced for the exercise and any on device persisted data
     * about the exercise will be deleted after the summary has been sent back.
     *
     * @return a [ListenableFuture] that completes once the exercise has been ended or fails if the
     * calling application does not own the active exercise.
     */
    public fun endExercise(): ListenableFuture<Void>

    /**
     * Flushes the sensors for the active exercise. This call should be used sparingly and will be
     * subject to throttling by Health Services.
     *
     * @return a [ListenableFuture] that completes once the flush has been completed or fails if the
     * calling application does not own the active exercise.
     */
    public fun flushExercise(): ListenableFuture<Void>

    /**
     * Ends the current lap, calls [ExerciseStateListener.onLapSummary] with data spanning the
     * marked lap and starts a new lap. If the exercise supports laps this method can be called at
     * any point after an exercise has been started and before it has been ended regardless of the
     * exercise status.
     *
     * The metrics in the lap summary will start from either the start time of the exercise or the
     * last time a lap was marked to the time this method is being called.
     *
     * If there's no exercise being tracked or if the exercise does not support laps then this
     * future will fail.
     */
    public fun markLap(): ListenableFuture<Void>

    /**
     * Returns the current [ExerciseInfo].
     *
     * This can be used by clients to determine if they or another app already owns an active
     * exercise being tracked by Health Services. For example, if an app is killed and it learns it
     * owns the active exercise it can register a new [ExerciseUpdateListener] and pick tracking up
     * from where it left off.
     */
    public val currentExerciseInfo: ListenableFuture<ExerciseInfo>

    /**
     * Sets the listener for the current [ExerciseUpdate].
     *
     * This listener won't be called until the calling application prepares or starts an exercise.
     * It will only receive updates from exercises tracked by this app.
     *
     * If an exercise is in progress, the [ExerciseUpdateListener] is immediately called with the
     * associated [ExerciseUpdate], and subsequently whenever the state is updated or an event is
     * triggered. Health Services will cache [ExerciseUpdate]s of an active exercise that are
     * generated while a listener is not active (for example, due to the app getting killed) and
     * deliver them as soon as the listener is registered again. If the client fails to maintain a
     * live [ExerciseStateListener] for at least five minutes during the duration of the exercise
     * Health Services can decide to terminate the exercise automatically. If this occurs, clients
     * can expect to receive an [ExerciseUpdate] with [ExerciseState#AUTO_ENDED] indicating that
     * their exercise has been automatically ended due to the lack of listener.
     *
     * Calls to the listener will be executed on the main application thread. To control where to
     * execute the listener, see the overload taking an [Executor]. To remove the listener use
     * [clearUpdateListener].
     */
    public fun setUpdateListener(listener: ExerciseUpdateListener): ListenableFuture<Void>

    /**
     * Calls to the listener will be executed using the specified [Executor]. To execute the
     * listener on the main application thread use the overload without the [Executor].
     */
    public fun setUpdateListener(
        listener: ExerciseUpdateListener,
        executor: Executor
    ): ListenableFuture<Void>

    /**
     * Clears the listener set using [setUpdateListener].
     *
     * If this listener is not already registered then this will be a no-op.
     *
     * @return a [ListenableFuture] that completes once the listener has been cleared (or verified
     * not to be set).
     */
    public fun clearUpdateListener(listener: ExerciseUpdateListener): ListenableFuture<Void>

    /**
     * Adds an [ExerciseGoal] for an active exercise.
     *
     * Goals apply to only active exercises owned by the client, and will be invalidated once the
     * exercise is complete.
     *
     * @return a [ListenableFuture] that completes once the exercise goal has been added. This
     * returned [ListenableFuture] fails if the calling app does not own the active exercise.
     */
    public fun addGoalToActiveExercise(exerciseGoal: ExerciseGoal): ListenableFuture<Void>

    /**
     * Removes an exercise goal for an active exercise.
     *
     * Takes into account equivalent milestones (i.e. milestones which are not equal but are
     * different representation of a common milestone. e.g. milestone A for every 2kms, currently at
     * threshold of 10kms, and milestone B for every 2kms, currently at threshold of 8kms).
     *
     * @return a [ListenableFuture] that completes once the exercise goal has been removed. This
     * returned [ListenableFuture] fails if the exercise is not active, and will be a no-op if
     * [exerciseGoal] has not been added in the past.
     */
    public fun removeGoalFromActiveExercise(exerciseGoal: ExerciseGoal): ListenableFuture<Void>

    /**
     * Enables or disables auto pause/resume for the current exercise.
     *
     * @param enabled a boolean to indicate if should be enabled or disabled
     */
    public fun overrideAutoPauseAndResumeForActiveExercise(enabled: Boolean): ListenableFuture<Void>

    /**
     * Returns the [ExerciseCapabilities] of this client for the device.
     *
     * This can be used to determine what [ExerciseType]s and [DataType]s this device supports.
     * Clients should use the capabilities to inform their requests since Health Services will
     * typically reject requests made for [DataTypes] or features (such as auto-pause) which are not
     * enabled for the rejected [ExerciseType].
     */
    public val capabilities: ListenableFuture<ExerciseCapabilities>
}
