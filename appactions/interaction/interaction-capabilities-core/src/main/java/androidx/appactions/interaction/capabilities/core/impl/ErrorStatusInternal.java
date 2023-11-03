/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core.impl;

import androidx.annotation.RestrictTo;

/** A class to define exceptions that are reported from dialog capability API. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum ErrorStatusInternal {
    // Unexpected error which doesn't fall into any other error status.
    UNKNOWN_ERROR_STATUS,
    // Exception occurred which is internal to the capabilities library.
    INTERNAL,
    // The current capability session was cancelled, likely because a new request was sent to the
    // capability before the first had time to complete.
    CANCELED,
    // Developer provided callback has timed out.
    TIMEOUT,
    // Invalid data was sent to the capability. This could be a nonsensical request Type or
    // malformed arguments (e.g. wrong data format for a BII argument).
    INVALID_REQUEST,
    // An exception was thrown from a developer-provided callback.
    EXTERNAL_EXCEPTION,
    // Tried to send request to a particular capability session, but that session never started
    // or has already ended.
    SESSION_NOT_FOUND,
}
