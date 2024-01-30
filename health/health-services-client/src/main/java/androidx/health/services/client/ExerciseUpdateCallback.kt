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

import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseEvent
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseState
import androidx.health.services.client.data.ExerciseUpdate

/** Callback that is called when the state of the current exercise is updated. */
// TODO(b/179756577): Add onExerciseEnd(ExerciseSummary) method.
public interface ExerciseUpdateCallback {

    /** Called when this callback has been successfully registered with Health Services. */
    public fun onRegistered()

    /**
     * Called when Health Services reports a failure with the registration of this callback.
     *
     * @param throwable a throwable sent by Health Services with information about the failure
     */
    public fun onRegistrationFailed(throwable: Throwable)

    /**
     * Called during an ACTIVE exercise or on any changes in [ExerciseState].
     *
     * @param update the [ExerciseUpdate] containing the latest exercise information
     */
    public fun onExerciseUpdateReceived(update: ExerciseUpdate)

    /**
     * Called during an [ExerciseState.ACTIVE] exercise once a lap has been marked.
     *
     * @param lapSummary an [ExerciseLapSummary] containing a summary of data collected during the
     * past lap
     */
    public fun onLapSummaryReceived(lapSummary: ExerciseLapSummary)

    /**
     * Called during an [ExerciseState.ACTIVE] exercise when the availability of a [DataType]
     * changes.
     *
     * @param dataType the [DataType] which experienced a change in availability
     * @param availability the new [Availability] state
     */
    public fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability)

    /**
     * Called when an [ExerciseEvent] is emitted. May be called during any exercise state
     * except for PREPARING or ENDED.
     *
     * @param event the latest [ExerciseEvent] received during an active exercise. To access the
     * data for each received [ExerciseEvent], clients can use conditional `when` on [ExerciseEvent]
     * for a specific event type to access the event-specific data.
     */
    public fun onExerciseEventReceived(event: ExerciseEvent) {}
}
