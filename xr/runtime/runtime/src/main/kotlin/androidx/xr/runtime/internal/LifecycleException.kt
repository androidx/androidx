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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo

/** Custom class for exceptions that may be thrown by a [LifecycleManager]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
open public class LifecycleException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/** Required permissions have not yet been granted to the application. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class PermissionNotGrantedException(
    public val permissions: List<String> = listOf(),
    cause: Throwable? = null,
) : LifecycleException("Required permission(s) are not granted: $permissions", cause)

/** A [Feature] attempting to be enabled is not supported by the current runtime. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ConfigurationNotSupportedException(
    message: String = "Failed to configure session, requested configuration is not supported.",
    cause: Throwable? = null,
) : LifecycleException(message, cause)

/** The Google Play Services Location Library is not linked. */
@Suppress("MentionsGoogle")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class GooglePlayServicesLocationLibraryNotLinkedException(cause: Throwable? = null) :
    LifecycleException(
        "Failed to configure session, Google Play Services Location Library is not linked.",
        cause,
    )

/**
 * A [Session] was unable to be created due to a required APK being out of date or not installed.
 *
 * @property requiredApk the fully qualified name of the package that is missing or needs to be
 *   updated.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ApkNotInstalledException(public val requiredApk: String) :
    LifecycleException("Failed to create session, $requiredApk installation required.")

/**
 * A [Session] was unable to be created due to the device not supporting a required APK or feature.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class UnsupportedDeviceException() :
    LifecycleException("Failed to create session, device is not supported.")

/**
 * A [Session] was unable to be created due to a required APK waiting for a remote query to confirm
 * support. [Session.create] should be called again after waiting at least 200 ms.
 *
 * @property requiredApk the fully qualified name of the package that is waiting for a remote query
 *   to confirm support.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ApkCheckAvailabilityInProgressException(public val requiredApk: String) :
    LifecycleException(
        "Failed to create session, $requiredApk requires a remote query to confirm support."
    )

/**
 * A [Session] was unable to be created due to the check for a required apk's availability failing.
 *
 * @property requiredApk the fully qualified name of the package that errored confirming
 *   availability.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ApkCheckAvailabilityErrorException(public val requiredApk: String) :
    LifecycleException("Failed to create session, unable to check $requiredApk availability.")

/**
 * A [Session] was unable to be created because a tracker required for a configured feature has not
 * been calibrated.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FaceTrackingNotCalibratedException() :
    LifecycleException(
        "Failed to create session, face tracking is required but has not been calibrated."
    )
