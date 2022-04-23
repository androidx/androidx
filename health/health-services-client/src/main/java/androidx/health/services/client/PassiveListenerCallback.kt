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
import androidx.health.services.client.data.PassiveGoal
import androidx.health.services.client.data.UserActivityInfo

/** A callback for receiving passive monitoring updates. */
// TODO(b/227475943): Update method naming to be past tense, add onRegistered and
//  onRegistrationFailed, open up visibility, and consider renaming to PassiveMonitoringCallback
internal interface PassiveListenerCallback {

    /**
     * Called when new [DataPoint]s are generated.
     *
     * @param dataPoints a list of new [DataPoint]s generated
     */
    public fun onNewDataPoints(dataPoints: List<DataPoint>) {}

    /**
     * Called when new [UserActivityInfo] is generated.
     *
     * @param info a new [UserActivityInfo] representing the current state
     */
    public fun onUserActivityInfo(info: UserActivityInfo) {}

    /**
     * Called when a [PassiveGoal] has been completed.
     *
     * @param goal the goal that has been completed
     */
    public fun onGoalCompleted(goal: PassiveGoal) {}

    /**
     * Called when the client has lost permission for the passive listener request. If this happens,
     * WHS will automatically unregister the client request and stop the relevant sensors. The
     * client can use this callback to detect the problem and either prompt the user to re-grant the
     * permissions or re-register while requesting only that which the app does have permission for.
     */
    public fun onPermissionLost() {}
}
