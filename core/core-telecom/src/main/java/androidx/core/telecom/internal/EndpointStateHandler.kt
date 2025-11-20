/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.telecom.internal

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.telecom.CallEndpointCompat

@RequiresApi(Build.VERSION_CODES.O)
internal class EndpointStateHandler(
    // The single source of truth for the current audio endpoints.
    // It's private to ensure it's only modified via the class methods.
    private val currentDevices: MutableSet<CallEndpointCompat> = mutableSetOf()
) {

    /**
     * Adds a list of endpoints to the current set.
     *
     * @return `true` if the set was changed as a result of the call.
     */
    fun add(endpoints: List<CallEndpointCompat>): Boolean {
        return currentDevices.addAll(endpoints)
    }

    /**
     * Removes a list of endpoints from the current set.
     *
     * @return `true` if the set was changed as a result of the call.
     */
    fun remove(endpoints: List<CallEndpointCompat>): Boolean {
        return currentDevices.removeAll(endpoints.toSet())
    }

    /** @return A new, sorted list of the current endpoints. */
    fun getSortedEndpoints(): List<CallEndpointCompat> {
        return currentDevices.sorted()
    }

    /** (For testing) A helper to view the raw internal set. */
    @VisibleForTesting internal fun getInternalSet(): Set<CallEndpointCompat> = currentDevices
}
