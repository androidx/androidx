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
// To prevent an empty UtilsKt class from being exposed in APi files
@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package androidx.health.connect.client.records

import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

@RequiresApi(Build.VERSION_CODES.R)
internal fun isAtLeastSdkExtension13(): Boolean {
    return SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 13
}

internal fun <T : Comparable<T>> T.requireNotLess(other: T, name: String) {
    require(this >= other) { "$name must not be less than $other, currently $this." }
}

internal fun <T : Comparable<T>> T.requireNotMore(other: T, name: String) {
    require(this <= other) { "$name must not be more than $other, currently $this." }
}

internal fun requireNonNegative(value: Long, name: String) {
    require(value >= 0) { "$name must not be negative" }
}

internal fun requireNonNegative(value: Double, name: String) {
    require(value >= 0.0) { "$name must not be negative" }
}

internal fun Map<String, Int>.reverse(): Map<Int, String> {
    return entries.associateBy({ it.value }, { it.key })
}

internal fun <T : Comparable<T>> T.requireInRange(min: T, max: T, name: String) {
    requireNotLess(min, name)
    requireNotMore(max, name)
}
