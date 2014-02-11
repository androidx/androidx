/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v4.app;

/**
 * Well-known extras used by {@link NotificationCompat} for backwards compatibility.
 */
public final class NotificationCompatExtras {
    /**
     * Extras key used internally by {@link NotificationCompat} to store the value of
     * the {@code Notification.FLAG_LOCAL_ONLY} field before it was available.
     * If possible, use {@link NotificationCompat#getLocalOnly} instead.
     */
    public static final String EXTRA_LOCAL_ONLY = NotificationCompatJellybean.EXTRA_LOCAL_ONLY;

    private NotificationCompatExtras() {}
}
