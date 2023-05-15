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

import androidx.health.services.client.impl.event.ExerciseUpdateListenerEvent;
import androidx.health.services.client.impl.response.AvailabilityResponse;
import androidx.health.services.client.impl.response.ExerciseLapSummaryResponse;
import androidx.health.services.client.impl.response.ExerciseUpdateResponse;

/**
 * Interface to get exercise updates.
 *
 * @hide
 */
oneway interface IExerciseUpdateListener {
    /** Called when there is an exercise update event. */
    void onExerciseUpdateListenerEvent(in ExerciseUpdateListenerEvent event) = 0;
}