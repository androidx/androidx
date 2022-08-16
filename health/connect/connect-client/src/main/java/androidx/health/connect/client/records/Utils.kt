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

import androidx.annotation.RestrictTo

internal fun <T : Comparable<T>> T.requireNotLess(other: T, name: String) {
    require(this >= other) { "$name must not be less than $other, currently $this." }
}

internal fun requireNonNegative(value: Int, name: String) {
    require(value >= 0) { "$name must not be negative" }
}

internal fun requireNonNegative(value: Long, name: String) {
    require(value >= 0) { "$name must not be negative" }
}

internal fun requireNonNegative(value: Double, name: String) {
    require(value >= 0.0) { "$name must not be negative" }
}
