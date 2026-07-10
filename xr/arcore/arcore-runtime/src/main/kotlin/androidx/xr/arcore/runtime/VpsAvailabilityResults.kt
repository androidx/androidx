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

package androidx.xr.arcore.runtime

import androidx.annotation.RestrictTo

/** Result of a [Geospatial.checkVpsAvailability] call. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public sealed class VpsAvailabilityResult

/**
 * Result of a successful [Geospatial.checkVpsAvailability] call. VPS is available at the requested
 * location.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class VpsAvailabilityAvailable() : VpsAvailabilityResult()

/**
 * Result of an unsuccessful [Geospatial.checkVpsAvailability] call. An internal error occurred
 * while determining availability. The app should not attempt to recover from this error. Please see
 * the Android logs for additional information.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class VpsAvailabilityErrorInternal() : VpsAvailabilityResult()

/**
 * Result of an unsuccessful [Geospatial.checkVpsAvailability] call. The external service could not
 * be reached due to a network connection error.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class VpsAvailabilityNetworkError() : VpsAvailabilityResult()

/**
 * Result of an unsuccessful [Geospatial.checkVpsAvailability] call. An authorization error occurred
 * when communicating with the Google Cloud ARCore API.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class VpsAvailabilityNotAuthorized() : VpsAvailabilityResult()

/**
 * Result of an unsuccessful [Geospatial.checkVpsAvailability] call. Too many requests were sent.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class VpsAvailabilityResourceExhausted() : VpsAvailabilityResult()

/**
 * Result of a successful [Geospatial.checkVpsAvailability] call. VPS is not available at the
 * requested location.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class VpsAvailabilityUnavailable() : VpsAvailabilityResult()
