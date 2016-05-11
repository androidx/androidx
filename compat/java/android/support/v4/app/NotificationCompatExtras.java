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
     * the {@link android.app.Notification#FLAG_LOCAL_ONLY} field before it was available.
     * If possible, use {@link NotificationCompat#getLocalOnly} to access this field.
     */
    public static final String EXTRA_LOCAL_ONLY = NotificationCompatJellybean.EXTRA_LOCAL_ONLY;

    /**
     * Extras key used internally by {@link NotificationCompat} to store the value set
     * by {@link android.app.Notification.Builder#setGroup} before it was available.
     * If possible, use {@link NotificationCompat#getGroup} to access this value.
     */
    public static final String EXTRA_GROUP_KEY = NotificationCompatJellybean.EXTRA_GROUP_KEY;

    /**
     * Extras key used internally by {@link NotificationCompat} to store the value set
     * by {@link android.app.Notification.Builder#setGroupSummary} before it was available.
     * If possible, use {@link NotificationCompat#isGroupSummary} to access this value.
     */
    public static final String EXTRA_GROUP_SUMMARY =
            NotificationCompatJellybean.EXTRA_GROUP_SUMMARY;

    /**
     * Extras key used internally by {@link NotificationCompat} to store the value set
     * by {@link android.app.Notification.Builder#setSortKey} before it was available.
     * If possible, use {@link NotificationCompat#getSortKey} to access this value.
     */
    public static final String EXTRA_SORT_KEY = NotificationCompatJellybean.EXTRA_SORT_KEY;

    /**
     * Extras key used internally by {@link NotificationCompat} to store the value of
     * the {@link android.app.Notification.Action#extras} field before it was available.
     * If possible, use {@link NotificationCompat#getAction} to access this field.
     */
    public static final String EXTRA_ACTION_EXTRAS =
            NotificationCompatJellybean.EXTRA_ACTION_EXTRAS;

    /**
     * Extras key used internally by {@link NotificationCompat} to store the value of
     * the {@link android.app.Notification.Action#getRemoteInputs} before the field
     * was available.
     * If possible, use {@link NotificationCompat.Action#getRemoteInputs to access this field.
     */
    public static final String EXTRA_REMOTE_INPUTS =
            NotificationCompatJellybean.EXTRA_REMOTE_INPUTS;

    private NotificationCompatExtras() {}
}
