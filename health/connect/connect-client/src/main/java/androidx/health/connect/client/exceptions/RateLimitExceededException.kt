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
 * Exception thrown when the calling application has exceeded its API call or rolling memory rate
 * limit quota.
 *
 * When this exception is thrown, callers should avoid immediate retries, apply exponential backoff,
 * and retry the request later after the rate limit quota has replenished.
 */
public class RateLimitExceededException(
    message: String? = null,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
