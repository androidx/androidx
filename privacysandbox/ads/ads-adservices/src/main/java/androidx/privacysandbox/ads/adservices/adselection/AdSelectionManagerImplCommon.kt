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

package androidx.privacysandbox.ads.adservices.adselection

import android.adservices.common.AdServicesPermissions
import android.annotation.SuppressLint
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresExtension
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.core.os.asOutcomeReceiver
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo
import kotlinx.coroutines.suspendCancellableCoroutine

@OptIn(ExperimentalFeatures.Ext8OptIn::class, ExperimentalFeatures.Ext10OptIn::class)
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("NewApi", "ClassVerificationFailure")
@RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
open class AdSelectionManagerImplCommon(
    protected val mAdSelectionManager: android.adservices.adselection.AdSelectionManager
    ) : AdSelectionManager() {

    @DoNotInline
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    override suspend fun selectAds(adSelectionConfig: AdSelectionConfig): AdSelectionOutcome {
        return AdSelectionOutcome(
            selectAdsInternal(
                adSelectionConfig.convertToAdServices()
            )
        )
    }

    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    private suspend fun selectAdsInternal(
        adSelectionConfig: android.adservices.adselection.AdSelectionConfig
    ): android.adservices.adselection.AdSelectionOutcome = suspendCancellableCoroutine { cont
        ->
        mAdSelectionManager.selectAds(
            adSelectionConfig,
            Runnable::run,
            cont.asOutcomeReceiver()
        )
    }

    @DoNotInline
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    override suspend fun selectAds(adSelectionFromOutcomesConfig: AdSelectionFromOutcomesConfig):
        AdSelectionOutcome {
        if (AdServicesInfo.adServicesVersion() >= 10 || AdServicesInfo.extServicesVersion() >= 10) {
            return Ext10Impl.selectAds(
                mAdSelectionManager,
                adSelectionFromOutcomesConfig
            )
        }
        throw UnsupportedOperationException("API is not available. Min version is API 31 ext 10")
    }

    @DoNotInline
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    override suspend fun reportImpression(reportImpressionRequest: ReportImpressionRequest) {
        suspendCancellableCoroutine<Any> { continuation ->
            mAdSelectionManager.reportImpression(
                reportImpressionRequest.convertToAdServices(),
                Runnable::run,
                continuation.asOutcomeReceiver()
            )
        }
    }

    @DoNotInline
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    override suspend fun reportEvent(reportEventRequest: ReportEventRequest) {
        if (AdServicesInfo.adServicesVersion() >= 8 || AdServicesInfo.extServicesVersion() >= 9) {
            return Ext8Impl.reportEvent(
                mAdSelectionManager,
                reportEventRequest
            )
        }
        throw UnsupportedOperationException("API is unsupported. Min version is API 33 ext 8 or " +
            "API 31/32 ext 9")
    }

    @DoNotInline
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    override suspend fun updateAdCounterHistogram(
        updateAdCounterHistogramRequest: UpdateAdCounterHistogramRequest
    ) {
        if (AdServicesInfo.adServicesVersion() >= 8 || AdServicesInfo.extServicesVersion() >= 9) {
            return Ext8Impl.updateAdCounterHistogram(
                mAdSelectionManager,
                updateAdCounterHistogramRequest
            )
        }
        throw UnsupportedOperationException("API is unsupported. Min version is API 33 ext 8 or " +
            "API 31/32 ext 9")
    }

    @DoNotInline
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    override suspend fun getAdSelectionData(
        getAdSelectionDataRequest: GetAdSelectionDataRequest
    ): GetAdSelectionDataOutcome {
        if (AdServicesInfo.adServicesVersion() >= 10 || AdServicesInfo.extServicesVersion() >= 10) {
            return Ext10Impl.getAdSelectionData(
                mAdSelectionManager,
                getAdSelectionDataRequest
            )
        }
        throw UnsupportedOperationException("API is not available. Min version is API 31 ext 10")
    }

    @DoNotInline
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    override suspend fun persistAdSelectionResult(
        persistAdSelectionResultRequest: PersistAdSelectionResultRequest
    ): AdSelectionOutcome {
        if (AdServicesInfo.adServicesVersion() >= 10 || AdServicesInfo.extServicesVersion() >= 10) {
            return Ext10Impl.persistAdSelectionResult(
                mAdSelectionManager,
                persistAdSelectionResultRequest
            )
        }
        throw UnsupportedOperationException("API is not available. Min version is API 31 ext 10")
    }

    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 10)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 10)
    private class Ext10Impl private constructor() {
        companion object {
            @DoNotInline
            @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
            suspend fun getAdSelectionData(
                adSelectionManager: android.adservices.adselection.AdSelectionManager,
                getAdSelectionDataRequest: GetAdSelectionDataRequest
            ): GetAdSelectionDataOutcome {
                return GetAdSelectionDataOutcome(suspendCancellableCoroutine<
                        android.adservices.adselection.GetAdSelectionDataOutcome> { continuation ->
                    adSelectionManager.getAdSelectionData(
                        getAdSelectionDataRequest.convertToAdServices(),
                        Runnable::run,
                        continuation.asOutcomeReceiver()
                    )
                })
            }

            @DoNotInline
            @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
            suspend fun persistAdSelectionResult(
                adSelectionManager: android.adservices.adselection.AdSelectionManager,
                persistAdSelectionResultRequest: PersistAdSelectionResultRequest
            ): AdSelectionOutcome {
                return AdSelectionOutcome(suspendCancellableCoroutine { continuation ->
                    adSelectionManager.persistAdSelectionResult(
                        persistAdSelectionResultRequest.convertToAdServices(),
                        Runnable::run,
                        continuation.asOutcomeReceiver()
                    )
                })
            }

            @DoNotInline
            @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
            suspend fun selectAds(
                adSelectionManager: android.adservices.adselection.AdSelectionManager,
                adSelectionFromOutcomesConfig: AdSelectionFromOutcomesConfig
            ): AdSelectionOutcome {
                return AdSelectionOutcome(suspendCancellableCoroutine { continuation ->
                    adSelectionManager.selectAds(
                        adSelectionFromOutcomesConfig.convertToAdServices(),
                        Runnable::run,
                        continuation.asOutcomeReceiver()
                    )
                })
            }
        }
    }

    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 8)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    private class Ext8Impl private constructor() {
        companion object {
            @DoNotInline
            @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
            suspend fun updateAdCounterHistogram(
                adSelectionManager: android.adservices.adselection.AdSelectionManager,
                updateAdCounterHistogramRequest: UpdateAdCounterHistogramRequest
            ) {
                suspendCancellableCoroutine<Any> { cont ->
                    adSelectionManager.updateAdCounterHistogram(
                        updateAdCounterHistogramRequest.convertToAdServices(),
                        Runnable::run,
                        cont.asOutcomeReceiver()
                    )
                }
            }

            @DoNotInline
            @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
            suspend fun reportEvent(
                adSelectionManager: android.adservices.adselection.AdSelectionManager,
                reportEventRequest: ReportEventRequest
            ) {
                suspendCancellableCoroutine<Any> { continuation ->
                    adSelectionManager.reportEvent(
                        reportEventRequest.convertToAdServices(),
                        Runnable::run,
                        continuation.asOutcomeReceiver()
                    )
                }
            }
        }
    }
}
