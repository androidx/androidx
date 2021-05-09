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

package androidx.health.services.client.impl;

import androidx.health.services.client.impl.IExerciseUpdateListener;
import androidx.health.services.client.impl.internal.IExerciseInfoCallback;
import androidx.health.services.client.impl.internal.IStatusCallback;
import androidx.health.services.client.impl.request.AutoPauseAndResumeConfigRequest;
import androidx.health.services.client.impl.request.CapabilitiesRequest;
import androidx.health.services.client.impl.request.ExerciseGoalRequest;
import androidx.health.services.client.impl.request.StartExerciseRequest;
import androidx.health.services.client.impl.response.CapabilitiesResponse;

/**
 * Interface to make ipc calls for health services exercise api.
 *
 * The next method added to the interface should use ID: 13
 * (this id needs to be incremented for each added method)
 *
 * @hide
 */
interface IExerciseApiService {
    /**
     * API version of the AIDL interface. Should be incremented every time a new
     * method is added.
     *
     */
    const int API_VERSION = 1;

    /**
     * Handles a given request to start an exercise.
     */
    void startExercise(in StartExerciseRequest startExerciseRequest, IStatusCallback statusCallback) = 5;

    /**
     * Method to pause the active exercise for the calling app.
     */
    void pauseExercise(in String packageName, IStatusCallback statusCallback) = 0;

    /**
     * Method to resume the active exercise for the calling app.
     */
    void resumeExercise(in String packageName, IStatusCallback statusCallback) = 1;

    /**
     * Method to end the active exercise for the calling app.
     */
    void endExercise(in String packageName, IStatusCallback statusCallback) = 2;

    /**
    * Method to end the current lap in the active exercise for the calling app.
    */
    void markLap(in String packageName, IStatusCallback statusCallback) = 9;

    /**
     * Returns version of this AIDL interface.
     *
     * <p> Can be used by client to detect version of the API on the service
     * side. Returned version should be always > 0.
     */
    int getApiVersion() = 3;

    /**
     * Returns the current exercise info.
     */
    void getCurrentExerciseInfo(in String packageName, IExerciseInfoCallback exerciseInfoCallback) = 12;

    /**
     * Sets the listener for the current exercise state.
     */
    void setUpdateListener(in String packageName, in IExerciseUpdateListener listener, IStatusCallback statusCallback)  = 6;

    /**
     * Clears the listener set using {@link #setUpdateListener}.
     */
    void clearUpdateListener(in String packageName, in IExerciseUpdateListener listener, IStatusCallback statusCallback) = 7;

    /**
     * Adds an exercise goal for an active exercise.
     *
     * <p>An exercise goal is a one-time goal, such as achieving a target total step count.
     *
     * <p>Goals apply to only active exercises owned by the client, and will be invalidated once the
     * exercise is complete. A goal can be added only after an exercise has been started.
     */
    void addGoalToActiveExercise(in ExerciseGoalRequest request, IStatusCallback statusCallback) = 8;

    /**
     * Sets whether auto-pause should be enabled
     */
    void overrideAutoPauseAndResumeForActiveExercise(in AutoPauseAndResumeConfigRequest request, IStatusCallback statusCallback) = 10;

    /**
     * Method to get capabilities.
     */
    CapabilitiesResponse getCapabilities(in CapabilitiesRequest request) = 11;
}