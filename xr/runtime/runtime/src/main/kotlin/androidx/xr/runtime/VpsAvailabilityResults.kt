/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.runtime

import androidx.annotation.RestrictTo

/** Result of a [Earth.checkVpsAvailability] call. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public sealed class VpsAvailabilityResult

/**
 * Result of a successful [Earth.checkVpsAvailability] call. Vps is available at the requested
 * location.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class VpsAvailabilityAvailable() : VpsAvailabilityResult()

/**
 * Result of an unsuccessful [Earth.checkVpsAvailability] call. An internal error occurred while
 * determining availability.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class VpsAvailabilityErrorInternal() : VpsAvailabilityResult()

/**
 * Result of an unsuccessful [Earth.checkVpsAvailability] call. The external service could not be
 * reached due to a network connection error..
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class VpsAvailabilityNetworkError() : VpsAvailabilityResult()

/**
 * Result of an unsuccessful [Earth.checkVpsAvailability] call. An authorization error occurred when
 * communicating with the Google Cloud ARCore API.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class VpsAvailabilityNotAuthorized() : VpsAvailabilityResult()

/** Result of an unsuccessful [Earth.checkVpsAvailability] call. Too many requests were sent. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class VpsAvailabilityResourceExhausted() : VpsAvailabilityResult()

/**
 * Result of a successful [Earth.checkVpsAvailability] call. VPS is not available at the requested
 * location.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class VpsAvailabilityUnavailable() : VpsAvailabilityResult()
