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

/** Result of a [Session.create] call. */
public sealed class SessionCreateResult

/**
 * Result of a successful [Session.create] call.
 *
 * @property session the [Session] that was created.
 */
public class SessionCreateSuccess(public val session: Session) : SessionCreateResult()

/**
 * Result of an unsuccessful [Session.create] call. The device has a [requiredApk] that is outdated,
 * was unable to confirm availability, or is not installed.
 *
 * @property requiredApk the fully qualified name of the package that is missing or needs to be
 *   updated.
 */
public class SessionCreateApkRequired(public val requiredApk: String) : SessionCreateResult()

/**
 * Result of an unsuccessful [Session.create] call. The session was not created due to the device
 * not supporting a required APK or feature.
 */
public class SessionCreateUnsupportedDevice() : SessionCreateResult()

/**
 * Result of an unsuccessful [Session.create] call. The session was not created due to an unknown
 * internal error. See the contents of [errorMessage] for more information.
 *
 * @param errorMessage a message supplied by the error that occurred
 */
public class SessionCreateUnknownError(public val errorMessage: String) : SessionCreateResult()

/**
 * Result of an unsuccessful [Session.create] call. The session was not created because the request
 * timed out.
 */
public class SessionCreateTimedOut() : SessionCreateResult()

/** Result of a [Session.configure] call. */
public sealed class SessionConfigureResult

/** Result of a successful [Session.configure] call. */
public class SessionConfigureSuccess() : SessionConfigureResult()

/**
 * Result of an unsuccessful [Session.configure] call. The Google Play Service Location Library is
 * not linked.
 */
@Suppress("MentionsGoogle")
@Deprecated(
    "Use SessionConfigureLibraryNotLinked instead.",
    ReplaceWith(
        "SessionConfigureLibraryNotLinked(\"com.google.android.gms:play-services-location\")"
    ),
)
public class SessionConfigureGooglePlayServicesLocationLibraryNotLinked() : SessionConfigureResult()

/**
 * Result of an unsuccessful [Session.configure] call. A library required to enable a requested
 * feature has not been linked to the application.
 *
 * @param libraryName refers to the missing library dependency
 */
public class SessionConfigureLibraryNotLinked(public val libraryName: String) :
    SessionConfigureResult()

/**
 * Result of an unsuccessful [Session.configure] call. The session could not be configured due to an
 * unknown internal error. See the contents of [errorMessage] for more information.
 *
 * @param errorMessage a message supplied by the error that occurred
 */
public class SessionConfigureUnknownError(public val errorMessage: String) :
    SessionConfigureResult()

/**
 * Result of an unsuccessful [Session.configure] call. Required calibration has not been performed
 * for a requested feature.
 */
public class SessionConfigureCalibrationRequired(
    public val calibrationType: RequiredCalibrationType
) : SessionConfigureResult()
