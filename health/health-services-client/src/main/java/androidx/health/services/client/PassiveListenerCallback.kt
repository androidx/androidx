/*
 * Copyright (C) 2022 The Android Open Source Project
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

import androidx.health.services.client.data.DataPoint
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.HealthEvent
import androidx.health.services.client.data.PassiveGoal
import androidx.health.services.client.data.UserActivityInfo

/** A callback for receiving passive monitoring updates. */
public interface PassiveListenerCallback {

    /** Called when this callback has been successfully registered with Health Services. */
    public fun onRegistered() {}

    /**
     * Called when Health Services reports a failure with the registration of this callback.
     *
     * @param throwable a [Throwable] sent by Health Services with information about the failure
     */
    public fun onRegistrationFailed(throwable: Throwable) {}

    /**
     * Called when new [DataPoint]s are generated.
     *
     * @param dataPoints a list of new [DataPoint]s generated
     */
    public fun onNewDataPointsReceived(dataPoints: DataPointContainer) {}

    /**
     * Called when new [UserActivityInfo] is generated.
     *
     * @param info a new [UserActivityInfo] representing the current state
     */
    public fun onUserActivityInfoReceived(info: UserActivityInfo) {}

    /**
     * Called when a [PassiveGoal] has been completed.
     *
     * @param goal the [PassiveGoal] that has been completed
     */
    public fun onGoalCompleted(goal: PassiveGoal) {}

    /**
     * Called when a [HealthEvent] has been detected.
     *
     * @param event the [HealthEvent] that has been detected
     */
    public fun onHealthEventReceived(event: HealthEvent) {}

    /**
     * Called when the client has lost permission for the passive listener request. If this happens,
     * WHS will automatically unregister the client request and stop the relevant sensors. The
     * client can use this callback to detect the problem and either prompt the user to re-grant the
     * permissions or re-register while requesting only that which the app does have permission for.
     */
    public fun onPermissionLost() {}
}
