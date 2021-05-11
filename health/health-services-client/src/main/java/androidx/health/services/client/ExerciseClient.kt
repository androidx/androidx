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
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

/** Client which provides a way to subscribe to the health data of a device during an exercise. */
public interface ExerciseClient {
    /**
     * Starts a new exercise.
     *
     * Once started, Health Services will begin collecting data associated with the exercise.
     *
     * Since Health Services only allows a single active exercise at a time, this will terminate any
     * active exercise currently in progress before starting the new one.
     *
     * @return a [ListenableFuture] that completes once the exercise has been started.
     */
    public fun startExercise(configuration: ExerciseConfig): ListenableFuture<Void>

    /**
     * Pauses the current exercise, if it is currently started.
     *
     * While the exercise is paused active time and cumulative metrics such as distance will not
     * accumulate. Instantaneous measurements such as speed and heart rate will continue to update
     * if requested in the [ExerciseConfig].
     *
     * If the exercise remains paused for a long period of time, Health Services will reduce or
     * suspend access to sensors and GPS in order to conserve battery. Should this happen, access
     * will automatically resume when the exercise is resumed.
     *
     * If the exercise is already paused then this method has no effect. If the exercise has ended
     * then the returned future will fail.
     *
     * @return a [ListenableFuture] that completes once the exercise has been paused.
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
     * @return a [ListenableFuture] that completes once the exercise has been resumed.
     */
    public fun resumeExercise(): ListenableFuture<Void>

    /**
     * Ends the current exercise, if it has been started. If the exercise has ended then this future
     * will fail.
     *
     * No additional metrics will be produced for the exercise and any on device persisted data
     * about the exercise will be deleted after the summary has been sent back.
     */
    public fun endExercise(): ListenableFuture<Void>

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

    /** Returns the [ExerciseInfo]. */
    public val currentExerciseInfo: ListenableFuture<ExerciseInfo>

    /**
     * Sets the listener for the current [ExerciseUpdate].
     *
     * This listener won't be called until an exercise is in progress. It will also only receive
     * updates from exercises tracked in this app.
     *
     * If an exercise is in progress, the [ExerciseUpdateListener] is immediately called with the
     * associated [ExerciseUpdate], and subsequently whenever the state is updated or an event is
     * triggered.
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
     * If the listener wasn't set, the returned [ListenableFuture] will fail.
     */
    public fun clearUpdateListener(listener: ExerciseUpdateListener): ListenableFuture<Void>

    /**
     * Adds an [ExerciseGoal] for an active exercise.
     *
     * An [ExerciseGoal] is a one-time goal, such as achieving a target total step count.
     *
     * Goals apply to only active exercises owned by the client, and will be invalidated once the
     * exercise is complete.
     *
     * @return a [ListenableFuture] that completes once the exercise goal has been added. This
     * returned [ListenableFuture] fails if the exercise is not active.
     */
    public fun addGoalToActiveExercise(exerciseGoal: ExerciseGoal): ListenableFuture<Void>

    /**
     * Enables or disables the auto pause/resume for the current exercise.
     *
     * @param enabled a boolean to indicate if should be enabled or disabled
     */
    public fun overrideAutoPauseAndResumeForActiveExercise(enabled: Boolean): ListenableFuture<Void>

    /** Returns the [ExerciseCapabilities] of this client for the device. */
    public val capabilities: ListenableFuture<ExerciseCapabilities>
}
