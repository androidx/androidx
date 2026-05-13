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

/** Anchor was not loaded from a valid UUID. */
public class AnchorInvalidUuidException(
    message: String = "Unable to create anchor. Invalid UUID provided.",
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/** Anchor was not created due to an authorization error. */
public class AnchorNotAuthorizedException(
    message: String = "Unable to create anchor. Not authorized.",
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/** Anchor was not created because the provided location is unsupported. */
public class AnchorUnsupportedLocationException(
    message: String = "Unable to create anchor. Unsupported location.",
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/** Anchor was not created due to a runtime failure. */
public class AnchorRuntimeFailureException(
    message: String = "Unable to create anchor due to failure in the runtime.",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
