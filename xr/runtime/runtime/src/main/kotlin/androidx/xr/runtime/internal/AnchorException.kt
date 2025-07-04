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

/** Custom class for exceptions related to [Anchor] APIs. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
open public class AnchorException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/** Anchor resource limit was reached when attempting to create an anchor. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class AnchorResourcesExhaustedException(cause: Throwable? = null) :
    AnchorException("Unable to create anchor. Anchor resources exhausted.", cause)

/** Camera is not tracking when attempting to create an anchor. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class AnchorNotTrackingException(cause: Throwable? = null) :
    AnchorException("Unable to create anchor. Camera is not tracking.", cause)

/** Anchor was not loaded from a valid UUID. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class AnchorInvalidUuidException(cause: Throwable? = null) :
    AnchorException("Unable to create anchor. Invalid UUID provided.", cause)

/** Anchor was not created due to an authorization error. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class AnchorNotAuthorizedException(cause: Throwable? = null) :
    AnchorException("Unable to create anchor. Not authorized.", cause)

/** Anchor was not created due to an unsupported location. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class AnchorUnsupportedLocationException(cause: Throwable? = null) :
    AnchorException("Unable to create anchor. Unsupported location.", cause)
