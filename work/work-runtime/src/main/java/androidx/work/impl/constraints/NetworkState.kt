/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.work.impl.constraints

/**
 * Stores information about network state.
 */
data class NetworkState(
    /**
     * Determines if the network is connected.
     */
    val isConnected: Boolean,

    /**
     * Determines if the network is validated - has a working Internet connection.
     */
    val isValidated: Boolean,

    /**
     * Determines if the network is metered.
     */
    val isMetered: Boolean,

    /**
     * Determines if the network is not roaming.
     */
    val isNotRoaming: Boolean
)
