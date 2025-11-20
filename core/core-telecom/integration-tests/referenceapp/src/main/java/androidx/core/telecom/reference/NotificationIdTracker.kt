/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.telecom.reference

import java.util.concurrent.atomic.AtomicInteger

// IMPORTANT: Starts at 1 because Android's startForeground() service API requires a
// non-zero notification ID. Using ID 0 for the notification associated with
// startForeground is invalid and will cause issues (e.g., the service may not
// actually enter the foreground state properly or the notification might not display).
internal val nextNotificationIdGenerator = AtomicInteger(1)

/**
 * Gets the next sequential, thread-safe ID suitable for use as a Notification ID, especially with
 * startForeground(). Starts from 1.
 */
fun getNextNotificationId(): Int {
    return nextNotificationIdGenerator.getAndIncrement()
}
