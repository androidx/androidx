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

/**
 * Set of parameters which should be passed to the UWB chip to start ranging.
 *
 * @property uwbConfigType
 * The UWB configuration type. One type specifies one fixed set of pre-defined parameters. The
 * UWB config type includes [UWB_CONFIG_ID_1].
 *
 * @property sessionId
 * The ID of the ranging session. If the value is SESSION_ID_UNSET (0), it will
 * be created from the hash of controller address and complex channel values.
 *
 * The same session IDs should be used at both ends (Controller and controlee).
 *
 * @property sessionKeyInfo
 * The session key info to use for the ranging.
 * If the profile uses STATIC
 * STS, this byte array is 8-byte long with first two bytes as
 * Vendor_ID and next six bytes as STATIC_STS_IV.
 *
 * The same session keys should be used at both ends (Controller and controlee).
 *
 * @property complexChannel
 * Optional. If device type is ROLE_CONTROLEE then complex channel should be set.
 *
 * @property peerDevices
 * The peers to perform ranging with. If using unicast, length should be 1.
 *
 * @property updateRateType
 * The update rate type of the ranging data. The update rate types include
 * [RANGING_UPDATE_RATE_AUTOMATIC], [RANGING_UPDATE_RATE_FREQUENT], and
 * [RANGING_UPDATE_RATE_INFREQUENT].
 */
class RangingParameters(
    val uwbConfigType: Int,
    val sessionId: Int,
    val sessionKeyInfo: ByteArray?,
    val complexChannel: UwbComplexChannel?,
    val peerDevices: List<UwbDevice>,
    val updateRateType: Int
) {

    companion object {

        /**
         * Pre-defined unicast STATIC STS DS-TWR ranging.
         *
         * deferred mode,
         * ranging interval = 240 ms,
         * slot duration = 2400 RSTU,
         * slots per ranging round = 6
         *
         * All other MAC parameters use FiRa/UCI default values.
         *
         * <p> Typical use case: device tracking tags
         */
        @JvmField
        val UWB_CONFIG_ID_1 = 1

        /**
         * Pre-defined one-to-many STATIC STS DS-TWR ranging
         *
         * deferred mode,
         * ranging interval = 200 ms,
         * slot duration = 2400 RSTU,
         * slots per ranging round = 20
         *
         * All other MAC parameters use FiRa/UCI default values.
         *
         * <p> Typical use case: smart phone interacts with many smart devices
         */
        @JvmField
        internal val UWB_CONFIG_ID_2 = 2

        /** Same as CONFIG_ID_1, except AoA data is not reported. */
        @JvmField
        internal val UWB_CONFIG_ID_3 = 3

        /**
         * When the screen is on, the reporting interval is hundreds of milliseconds.
         * When the screen is off, the reporting interval is a few seconds.
         */
        @JvmField
        val RANGING_UPDATE_RATE_AUTOMATIC = 1

        /**
         * The reporting interval is the same as in the AUTOMATIC screen-off case. The
         * The power consumption is optimized by turning off the radio between ranging
         * reports. (The implementation is hardware and software dependent and it may
         * change between different versions.)
         */
        @JvmField
        val RANGING_UPDATE_RATE_INFREQUENT = 2

        /**
         * The reporting interval is the same as in the AUTOMATIC screen-on case.
         *
         * The actual reporting interval is UwbConfigId related. Different
         * configuration may use different values. (The default reporting interval at INFREQUENT mode is 4 seconds)
         */
        @JvmField
        val RANGING_UPDATE_RATE_FREQUENT = 3
    }
}
