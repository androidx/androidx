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

package androidx.health.connect.client.exceptions

/**
 * Exception thrown when a single record in a request exceeds the maximum allowed single-record size
 * limit.
 *
 * Callers can recover by reducing the size of (for example, downsampling series samples) or
 * omitting the oversized record before retrying.
 *
 * Note: This exception extends [IllegalArgumentException]. In previous versions of this library,
 * oversized records on Android 14 (API 34) and higher threw [IllegalStateException].
 */
public class RecordTooLargeException(
    message: String? = null,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)
