/*
 * Copyright (C) 2025 The Android Open Source Project
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
// To prevent an empty UtilsKt class from being exposed in APi files
@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package androidx.health.connect.client.feature

import android.os.Build
import androidx.annotation.RestrictTo
import androidx.health.connect.client.HealthConnectFeatures.Companion.FEATURE_PERSONAL_HEALTH_RECORD
import androidx.health.connect.client.HealthConnectFeatures.Companion.FEATURE_STATUS_AVAILABLE
import kotlin.reflect.KClass

internal const val FEATURE_CONSTANT_NAME_PHR = "FEATURE_PERSONAL_HEALTH_RECORD"

@OptIn(ExperimentalPersonalHealthRecordApi::class)
internal fun isPersonalHealthRecordFeatureAvailableInPlatform(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return false
    }
    return HealthConnectFeaturesPlatformImpl.getFeatureStatus(FEATURE_PERSONAL_HEALTH_RECORD) ==
        FEATURE_STATUS_AVAILABLE
}

/** Create an [UnsupportedOperationException] with given [featureConstantName] and [apiName]. */
internal fun createExceptionDueToFeatureUnavailable(featureConstantName: String, apiName: String) =
    UnsupportedOperationException(
        "\"$apiName\" must only be called if \"$featureConstantName\" feature is available. To check whether the feature is available, use `HealthConnectFeatures.getFeatureStatus(HealthConnectFeatures.$featureConstantName) == FEATURE_STATUS_AVAILABLE`."
    )

/**
 * Similar to [with], this method executes `block` if PHR feature is available, otherwise throwing
 * an [UnsupportedOperationException] pointing to `apiName`.
 */
internal fun <T> withPhrFeatureCheck(kClass: KClass<*>, block: () -> T): T =
    withPhrFeatureCheck("${kClass.simpleName}", block)

/**
 * Similar to [with], this method executes `block` if PHR feature is available, otherwise throwing
 * an [UnsupportedOperationException] pointing to `apiName`.
 */
internal fun <T> withPhrFeatureCheck(kClass: KClass<*>, methodName: String, block: () -> T): T =
    withPhrFeatureCheck("${kClass.simpleName}#$methodName", block)

/**
 * Similar to [with], this method executes `block` if PHR feature is available, otherwise throwing
 * an [UnsupportedOperationException] pointing to `apiName`.
 */
internal fun <T> withPhrFeatureCheck(apiName: String, block: () -> T): T {
    if (isPersonalHealthRecordFeatureAvailableInPlatform()) {
        return block()
    } else {
        throw createExceptionDueToFeatureUnavailable(FEATURE_CONSTANT_NAME_PHR, apiName)
    }
}

/**
 * Similar to [with], this method executes `block` if PHR feature is available, otherwise throwing
 * an [UnsupportedOperationException] pointing to `apiName`.
 */
internal suspend fun <T> withPhrFeatureCheckSuspend(
    kClass: KClass<*>,
    methodName: String,
    block: suspend () -> T,
): T {
    return withPhrFeatureCheckSuspend("${kClass.simpleName}#$methodName", block)
}

/**
 * Similar to [with], this method executes `block` if PHR feature is available, otherwise throwing
 * an [UnsupportedOperationException] pointing to `apiName`.
 */
internal suspend fun <T> withPhrFeatureCheckSuspend(apiName: String, block: suspend () -> T): T {
    if (isPersonalHealthRecordFeatureAvailableInPlatform()) {
        return block()
    } else {
        throw createExceptionDueToFeatureUnavailable(FEATURE_CONSTANT_NAME_PHR, apiName)
    }
}
