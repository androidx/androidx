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

package androidx.privacysandbox.ads.adservices.customaudience

import android.adservices.common.AdServicesPermissions
import android.annotation.SuppressLint
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresExtension
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.core.os.asOutcomeReceiver
import androidx.privacysandbox.ads.adservices.common.AdData
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo
import kotlinx.coroutines.suspendCancellableCoroutine

@OptIn(ExperimentalFeatures.Ext10OptIn::class)
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("NewApi", "ClassVerificationFailure")
@RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
open class CustomAudienceManagerImplCommon(
    protected val customAudienceManager: android.adservices.customaudience.CustomAudienceManager
    ) : CustomAudienceManager() {
    @DoNotInline
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    override suspend fun joinCustomAudience(request: JoinCustomAudienceRequest) {
        suspendCancellableCoroutine { continuation ->
            customAudienceManager.joinCustomAudience(
                convertJoinRequest(request),
                Runnable::run,
                continuation.asOutcomeReceiver()
            )
        }
    }

    @DoNotInline
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    override suspend fun fetchAndJoinCustomAudience(request: FetchAndJoinCustomAudienceRequest) {
        if (AdServicesInfo.adServicesVersion() >= 10 || AdServicesInfo.extServicesVersion() >= 10) {
            return Ext10Impl.fetchAndJoinCustomAudience(customAudienceManager, request)
        }
        throw UnsupportedOperationException("API is not available. Min version is API 31 ext 10")
    }

    @DoNotInline
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    override suspend fun leaveCustomAudience(request: LeaveCustomAudienceRequest) {
        suspendCancellableCoroutine { continuation ->
            customAudienceManager.leaveCustomAudience(
                convertLeaveRequest(request),
                Runnable::run,
                continuation.asOutcomeReceiver()
            )
        }
    }

    private fun convertJoinRequest(
        request: JoinCustomAudienceRequest
    ): android.adservices.customaudience.JoinCustomAudienceRequest {
        return android.adservices.customaudience.JoinCustomAudienceRequest.Builder()
            .setCustomAudience(convertCustomAudience(request.customAudience))
            .build()
    }

    private fun convertLeaveRequest(
        request: LeaveCustomAudienceRequest
    ): android.adservices.customaudience.LeaveCustomAudienceRequest {
        return android.adservices.customaudience.LeaveCustomAudienceRequest.Builder()
            .setBuyer(request.buyer.convertToAdServices())
            .setName(request.name)
            .build()
    }

    private fun convertCustomAudience(
        request: CustomAudience
    ): android.adservices.customaudience.CustomAudience {
        return android.adservices.customaudience.CustomAudience.Builder()
            .setActivationTime(request.activationTime)
            .setAds(convertAds(request.ads))
            .setBiddingLogicUri(request.biddingLogicUri)
            .setBuyer(request.buyer.convertToAdServices())
            .setDailyUpdateUri(request.dailyUpdateUri)
            .setExpirationTime(request.expirationTime)
            .setName(request.name)
            .setTrustedBiddingData(convertTrustedSignals(request.trustedBiddingSignals))
            .setUserBiddingSignals(request.userBiddingSignals?.convertToAdServices())
            .build()
    }

    private fun convertAds(
        input: List<AdData>
    ): List<android.adservices.common.AdData> {
        val result = mutableListOf<android.adservices.common.AdData>()
        for (ad in input) {
            result.add(ad.convertToAdServices())
        }
        return result
    }

    private fun convertTrustedSignals(
        input: TrustedBiddingData?
    ): android.adservices.customaudience.TrustedBiddingData? {
        if (input == null) return null
        return android.adservices.customaudience.TrustedBiddingData.Builder()
            .setTrustedBiddingKeys(input.trustedBiddingKeys)
            .setTrustedBiddingUri(input.trustedBiddingUri)
            .build()
    }

    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 10)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 10)
    private class Ext10Impl private constructor() {
        companion object {
            @DoNotInline
            @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
            suspend fun fetchAndJoinCustomAudience(
                customAudienceManager: android.adservices.customaudience.CustomAudienceManager,
                fetchAndJoinCustomAudienceRequest: FetchAndJoinCustomAudienceRequest
            ) {
                suspendCancellableCoroutine { continuation ->
                    customAudienceManager.fetchAndJoinCustomAudience(
                        fetchAndJoinCustomAudienceRequest.convertToAdServices(),
                        Runnable::run,
                        continuation.asOutcomeReceiver()
                    )
                }
            }
        }
    }
}
