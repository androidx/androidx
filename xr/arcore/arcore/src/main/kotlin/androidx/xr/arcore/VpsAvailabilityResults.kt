/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.arcore

import androidx.xr.arcore.runtime.VpsAvailabilityAvailable as RTVpsAvailabilityAvailable
import androidx.xr.arcore.runtime.VpsAvailabilityErrorInternal as RTVpsAvailabilityErrorInternal
import androidx.xr.arcore.runtime.VpsAvailabilityNetworkError as RTVpsAvailabilityNetworkError
import androidx.xr.arcore.runtime.VpsAvailabilityNotAuthorized as RTVpsAvailabilityNotAuthorized
import androidx.xr.arcore.runtime.VpsAvailabilityResourceExhausted as RTVpsAvailabilityResourceExhausted
import androidx.xr.arcore.runtime.VpsAvailabilityResult as RTVpsAvailabilityResult
import androidx.xr.arcore.runtime.VpsAvailabilityUnavailable as RTVpsAvailabilityUnavailable

/** Result of a [Geospatial.checkVpsAvailability] call. */
public sealed class VpsAvailabilityResult

/**
 * Result of a successful [Geospatial.checkVpsAvailability] call. VPS is available at the requested
 * location.
 */
public class VpsAvailabilityAvailable() : VpsAvailabilityResult() {
    internal fun toRuntimeVpsAvailabilityResult() = RTVpsAvailabilityAvailable()
}

/**
 * Result of an unsuccessful [Geospatial.checkVpsAvailability] call. An internal error occurred
 * while determining availability. The app should not attempt to recover from this error. Please see
 * the Android logs for additional information.
 */
public class VpsAvailabilityErrorInternal() : VpsAvailabilityResult() {
    internal fun toRuntimeVpsAvailabilityResult() = RTVpsAvailabilityErrorInternal()
}

/**
 * Result of an unsuccessful [androidx.xr.arcore.Geospatial.checkVpsAvailability] call. The external
 * service could not be reached due to a network connection error.
 */
public class VpsAvailabilityNetworkError() : VpsAvailabilityResult() {
    internal fun toRuntimeVpsAvailabilityResult() = RTVpsAvailabilityNetworkError()
}

/**
 * Result of an unsuccessful [Geospatial.checkVpsAvailability] call. An authorization error occurred
 * when communicating with the Google Cloud ARCore API.
 */
public class VpsAvailabilityNotAuthorized() : VpsAvailabilityResult() {
    internal fun toRuntimeVpsAvailabilityResult() = RTVpsAvailabilityNotAuthorized()
}

/**
 * Result of an unsuccessful [Geospatial.checkVpsAvailability] call. Too many requests were sent.
 */
public class VpsAvailabilityResourceExhausted() : VpsAvailabilityResult() {
    internal fun toRuntimeVpsAvailabilityResult() = RTVpsAvailabilityResourceExhausted()
}

/**
 * Result of a successful [Geospatial.checkVpsAvailability] call. VPS is not available at the
 * requested location.
 */
public class VpsAvailabilityUnavailable() : VpsAvailabilityResult() {
    internal fun toRuntimeVpsAvailabilityResult() = RTVpsAvailabilityUnavailable()
}

internal fun RTVpsAvailabilityResult.toVpsAvailabilityResult(): VpsAvailabilityResult {
    if (this is RTVpsAvailabilityAvailable) return VpsAvailabilityAvailable()
    if (this is RTVpsAvailabilityErrorInternal) return VpsAvailabilityErrorInternal()
    if (this is RTVpsAvailabilityNetworkError) return VpsAvailabilityNetworkError()
    if (this is RTVpsAvailabilityNotAuthorized) return VpsAvailabilityNotAuthorized()
    if (this is RTVpsAvailabilityResourceExhausted) return VpsAvailabilityResourceExhausted()
    if (this is RTVpsAvailabilityUnavailable) return VpsAvailabilityUnavailable()

    throw IllegalStateException()
}
