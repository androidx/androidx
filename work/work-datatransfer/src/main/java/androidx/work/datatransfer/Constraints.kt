/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.work.datatransfer

import android.net.NetworkCapabilities
import android.net.NetworkRequest

/**
 * A specification of the requirements that need to be met before a [UserInitiatedTaskRequest] can
 * run. By default, UserInitiatedTaskRequest only require a network constraint to be specified.
 * By adding additional constraints, you can make sure that work only runs in certain situations -
 * for example, when you have an unmetered network and are charging.
 */
class Constraints constructor(
    /**
     * The network request required for the work to run.
     *
     * **The default value assumes a requirement of any internet.**
     */
    private val requiredNetwork: NetworkRequest = getDefaultNetworkRequest()
) {
    val networkRequest: NetworkRequest
        get() = requiredNetwork

    companion object {
        /**
         * Copies an existing [Constraints] object.
         */
        @JvmStatic
        fun copyFrom(constraints: Constraints): Constraints {
            return Constraints(constraints.requiredNetwork)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Constraints
        return requiredNetwork == that.requiredNetwork
    }

    override fun hashCode(): Int {
        return 31 * requiredNetwork.hashCode()
    }
}

fun getDefaultNetworkRequest(): NetworkRequest {
    return NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
}
