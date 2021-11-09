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

package androidx.core.app;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.INotificationSideChannel;

/**
 * Abstract service to receive side channel notifications sent from
 * {@link androidx.core.app.NotificationManagerCompat}.
 *
 * <p>To receive side channel notifications, extend this service and register it in your
 * android manifest with an intent filter for the BIND_NOTIFICATION_SIDE_CHANNEL action.
 * Note: you must also have an enabled
 * {@link android.service.notification.NotificationListenerService} within your package.
 *
 * <p>Example AndroidManifest.xml addition:
 * <pre>
 * &lt;service android:name="com.example.NotificationSideChannelService"&gt;
 *     &lt;intent-filter&gt;
 *         &lt;action android:name="android.support.BIND_NOTIFICATION_SIDE_CHANNEL" /&gt;
 *     &lt;/intent-filter&gt;
 * &lt;/service&gt;</pre>
 *
 */
public abstract class NotificationCompatSideChannelService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        if (intent.getAction().equals(NotificationManagerCompat.ACTION_BIND_SIDE_CHANNEL)) {
            // Block side channel service connections if the current sdk has no need for
            // side channeling.
            if (Build.VERSION.SDK_INT > NotificationManagerCompat.MAX_SIDE_CHANNEL_SDK_VERSION) {
                return null;
            }
            return new NotificationSideChannelStub();
        }
        return null;
    }

    /**
     * Handle a side-channeled notification being posted.
     */
    public abstract void notify(String packageName, int id, String tag, Notification notification);

    /**
     * Handle a side-channelled notification being cancelled.
     */
    public abstract void cancel(String packageName, int id, String tag);

    /**
     * Handle the side-channelled cancelling of all notifications for a package.
     */
    public abstract void cancelAll(String packageName);

    private class NotificationSideChannelStub extends INotificationSideChannel.Stub {
        NotificationSideChannelStub() {
        }

        @Override
        public void notify(String packageName, int id, String tag, Notification notification)
                throws RemoteException {
            checkPermission(getCallingUid(), packageName);
            long idToken = clearCallingIdentity();
            try {
                NotificationCompatSideChannelService.this.notify(packageName, id, tag, notification);
            } finally {
                restoreCallingIdentity(idToken);
            }
        }

        @Override
        public void cancel(String packageName, int id, String tag) throws RemoteException {
            checkPermission(getCallingUid(), packageName);
            long idToken = clearCallingIdentity();
            try {
                NotificationCompatSideChannelService.this.cancel(packageName, id, tag);
            } finally {
                restoreCallingIdentity(idToken);
            }
        }

        @Override
        public void cancelAll(String packageName) {
            checkPermission(getCallingUid(), packageName);
            long idToken = clearCallingIdentity();
            try {
                NotificationCompatSideChannelService.this.cancelAll(packageName);
            } finally {
                restoreCallingIdentity(idToken);
            }
        }
    }

    void checkPermission(int callingUid, String packageName) {
        for (String validPackage : getPackageManager().getPackagesForUid(callingUid)) {
            if (validPackage.equals(packageName)) {
                return;
            }
        }
        throw new SecurityException("NotificationSideChannelService: Uid " + callingUid
                + " is not authorized for package " + packageName);
    }
}
