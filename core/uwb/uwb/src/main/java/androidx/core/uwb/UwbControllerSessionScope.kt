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

package androidx.core.uwb

/** Interface for controller client session that is established between nearby UWB devices. */
public interface UwbControllerSessionScope : UwbClientSessionScope {
    /**
     * The local device's complex channel which can be used for ranging.
     *
     * A complex channel can only be used for a single ranging session. After the ranging session is
     * ended, a new channel will be allocated.
     *
     * Ranging session duration may also be limited to prevent channels from being used for too
     * long. In this case, your ranging session would be suspended and clients would need to
     * exchange the new channel with their peer before starting again.
     */
    public val uwbComplexChannel: UwbComplexChannel

    /**
     * Dynamically adds a controlee to an active ranging session. The controlee to be added must be
     * configured with the a set of parameters that can join the existing connection.
     *
     * @throws [IllegalStateException] if the ranging is inactive or if the ranging profile is that
     *   of a unicast profile.
     *
     * Otherwise, this method will return successfully, and clients are expected to handle either
     * [RangingResult.RangingResultPosition] or [RangingResult.RangingResultPeerDisconnected] to
     * listen for starts or failures.
     */
    public suspend fun addControlee(address: UwbAddress)

    /**
     * Dynamically removes a controlee from an active ranging session.
     *
     * @throws [IllegalStateException] if the ranging is inactive, if the ranging profile is that of
     *   a unicast profile, or if the requested device is not being ranged to.
     * @throws [androidx.core.uwb.exceptions.UwbSystemCallbackException] if the operation failed due
     *   to hardware or firmware issues.
     *
     * Otherwise, this method will return successfully, and clients are expected to handle
     * [RangingResult.RangingResultPeerDisconnected] to listen for disconnects.
     */
    public suspend fun removeControlee(address: UwbAddress)

    /**
     * Dynamically reconfigures ranging interval to an active ranging session.
     *
     * @throws [IllegalStateException] if the ranging is inactive.
     *
     * Otherwise, this method will return successfully with the ranging session reconfigured to skip
     * number of ranging intervals set in intervalSkipCount. If intervalSkipCount is set to 0, the
     * ranging interval will be set to the interval used when startRanging was called.
     */
    public suspend fun reconfigureRangingInterval(intervalSkipCount: Int)

    /**
     * Dynamically adds a controlee to an active ranging session. The controlee to be added must be
     * configured with the a set of parameters that can join the existing connection.
     *
     * @throws [IllegalStateException] if the ranging is inactive or if the ranging profile is not
     *   the provisioned STS individual key case.
     *
     * Otherwise, this method will return successfully, and clients are expected to handle either
     * [RangingResult.RangingResultPosition] or [RangingResult.RangingResultPeerDisconnected] to
     * listen for starts or failures.
     */
    public suspend fun addControlee(address: UwbAddress, parameters: RangingControleeParameters)
}
