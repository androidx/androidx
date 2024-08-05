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

package androidx.core.uwb

import kotlinx.coroutines.flow.Flow

/** Interface for client session that is established between nearby UWB devices. */
public interface UwbClientSessionScope {
    /**
     * Returns a flow of [RangingResult]. Consuming the flow will initiate the UWB ranging and only
     * one flow can be initiated. To consume the flow from multiple consumers, convert the flow to a
     * SharedFlow.
     *
     * @throws [IllegalStateException] if a new flow was consumed again after the UWB ranging is
     *   already initiated.
     * @throws [androidx.core.uwb.exceptions.UwbSystemCallbackException] if the backend UWB system
     *   has resulted in an error.
     * @throws [SecurityException] if ranging does not have the android.permission.UWB_RANGING
     *   permission. Apps must have requested and been granted this permission before calling this
     *   method.
     * @throws [IllegalArgumentException] if the client starts a ranging session without setting
     *   complex channel and peer address.
     * @throws [IllegalArgumentException] if the client starts a ranging session with invalid config
     *   id or ranging update type.
     */
    public fun prepareSession(parameters: RangingParameters): Flow<RangingResult>

    /** Returns the [RangingCapabilities] which the device supports. */
    public val rangingCapabilities: RangingCapabilities

    /**
     * A local address can only be used for a single ranging session. After a ranging session is
     * ended, a new address will be allocated.
     *
     * Ranging session duration may also be limited to prevent addresses from being used for too
     * long. In this case, your ranging session would be suspended and clients would need to
     * exchange the new address with their peer before starting again.
     */
    public val localAddress: UwbAddress

    /**
     * Dynamically reconfigures range data notification config to an active ranging session.
     *
     * @throws [IllegalStateException] if the ranging is inactive.
     *
     * Otherwise, this method will return successfully, then clients are expected to handle
     * [RangingResult.RangingResultPeerDisconnected] with the controlee as parameter of the
     * callback.
     */
    public suspend fun reconfigureRangeDataNtf(
        configType: Int,
        proximityNear: Int,
        proximityFar: Int
    )
}
