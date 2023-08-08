/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.work;

import android.app.Notification;

import androidx.annotation.NonNull;

/**
 * The information required when a {@link ListenableWorker} runs in the context of a foreground
 * service.
 */
// NOTE: once this file is migrated to Kotlin, corresponding stub in lint rules should be migrated.
// As a result lint checks should start relying on parameter names instead.
public final class ForegroundInfo {

    private final int mNotificationId;
    private final int mForegroundServiceType;
    private final Notification mNotification;

    /**
     * Creates an instance of {@link ForegroundInfo} with a {@link Notification}.
     * <p>
     * On API 29 and above, you can specify a {@code foregroundServiceType} by using the
     * {@link #ForegroundInfo(int, Notification, int)} constructor; otherwise, a default {@code
     * foregroundServiceType} of {@code 0} will be used.
     *
     * @param notificationId The {@link Notification} id
     * @param notification   The {@link Notification} to show when the Worker is running in the
     *                       context of a foreground {@link android.app.Service}
     */
    public ForegroundInfo(int notificationId, @NonNull Notification notification) {
        this(notificationId, notification, 0);
    }

    /**
     * Creates an instance of {@link ForegroundInfo} with a {@link Notification} and foreground
     * {@link android.app.Service} type.
     *
     * For more information look at {@code android.app.Service#startForeground(int,
     * Notification, int)}.
     *
     * @param notificationId        The {@link Notification} id
     * @param notification          The {@link Notification}
     * @param foregroundServiceType The foreground {@link android.content.pm.ServiceInfo} type
     */
    public ForegroundInfo(
            int notificationId,
            @NonNull Notification notification,
            int foregroundServiceType) {
        mNotificationId = notificationId;
        mNotification = notification;
        mForegroundServiceType = foregroundServiceType;
    }

    /**
     * @return The {@link Notification} id to be used
     */
    public int getNotificationId() {
        return mNotificationId;
    }

    /**
     * @return The foreground {@link android.content.pm.ServiceInfo} type
     */
    public int getForegroundServiceType() {
        return mForegroundServiceType;
    }

    /**
     * @return The user visible {@link Notification}
     */
    @NonNull
    public Notification getNotification() {
        return mNotification;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ForegroundInfo that = (ForegroundInfo) o;

        if (mNotificationId != that.mNotificationId) return false;
        if (mForegroundServiceType != that.mForegroundServiceType) return false;
        return mNotification.equals(that.mNotification);
    }

    @Override
    public int hashCode() {
        int result = mNotificationId;
        result = 31 * result + mForegroundServiceType;
        result = 31 * result + mNotification.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ForegroundInfo{");
        sb.append("mNotificationId=").append(mNotificationId);
        sb.append(", mForegroundServiceType=").append(mForegroundServiceType);
        sb.append(", mNotification=").append(mNotification);
        sb.append('}');
        return sb.toString();
    }
}
