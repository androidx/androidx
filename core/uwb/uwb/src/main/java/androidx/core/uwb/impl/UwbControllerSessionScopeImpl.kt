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

package androidx.core.uwb.impl

import androidx.core.uwb.RangingCapabilities
import androidx.core.uwb.RangingParameters
import androidx.core.uwb.RangingResult
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbComplexChannel
import androidx.core.uwb.UwbControllerSessionScope
import androidx.core.uwb.exceptions.UwbSystemCallbackException
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.uwb.UwbClient
import com.google.android.gms.nearby.uwb.UwbStatusCodes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

internal class UwbControllerSessionScopeImpl(
    private val uwbClient: UwbClient,
    override val rangingCapabilities: RangingCapabilities,
    override val localAddress: UwbAddress,
    override val uwbComplexChannel: UwbComplexChannel
) : UwbControllerSessionScope {
    private val uwbClientSessionScope =
        UwbClientSessionScopeImpl(uwbClient, rangingCapabilities, localAddress)

    override suspend fun addControlee(address: UwbAddress) {
        val uwbAddress = com.google.android.gms.nearby.uwb.UwbAddress(address.address)
        try {
            uwbClient.addControlee(uwbAddress).await()
        } catch (e: ApiException) {
            if (e.statusCode == UwbStatusCodes.INVALID_API_CALL) {
                throw IllegalStateException("Please check that the ranging is active and the" +
                    "ranging profile supports multi-device ranging.")
            }
        }
    }

    override suspend fun removeControlee(address: UwbAddress) {
        val uwbAddress = com.google.android.gms.nearby.uwb.UwbAddress(address.address)
        try {
            uwbClient.removeControlee(uwbAddress).await()
        } catch (e: ApiException) {
            when (e.statusCode) {
                UwbStatusCodes.INVALID_API_CALL ->
                    throw IllegalStateException("Please check that the ranging is active and the" +
                        "ranging profile supports multi-device ranging.")
                UwbStatusCodes.UWB_SYSTEM_CALLBACK_FAILURE ->
                    throw UwbSystemCallbackException("The operation failed due to hardware or " +
                        "firmware issues.")
            }
        }
    }

    override fun prepareSession(parameters: RangingParameters): Flow<RangingResult> {
        return uwbClientSessionScope.prepareSession(parameters)
    }
}
