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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compatibility library for NotificationManager with fallbacks for older platforms.
 *
 * <p>To use this class, call the static function {@link #from} to get a
 * {@link NotificationManagerCompat} object, and then call one of its
 * methods to post or cancel notifications.
 */
public class NotificationManagerCompat {
    private static final String TAG = "NotifManCompat";

    /**
     * Notification extras key: if set to true, the posted notification should use
     * the side channel for delivery instead of using notification manager.
     */
    public static final String EXTRA_USE_SIDE_CHANNEL =
            NotificationCompatJellybean.EXTRA_USE_SIDE_CHANNEL;

    /**
     * Intent action to register for on a service to receive side channel
     * notifications. The listening service must be in the same package as an enabled
     * {@link android.service.notification.NotificationListenerService}.
     */
    public static final String ACTION_BIND_SIDE_CHANNEL =
            "android.support.BIND_NOTIFICATION_SIDE_CHANNEL";

    /**
     * Maximum sdk build version which needs support for side channeled notifications.
     * Currently the only needed use is for side channeling group children before KITKAT_WATCH.
     */
    static final int MAX_SIDE_CHANNEL_SDK_VERSION = 19;

    /** Base time delay for a side channel listener queue retry. */
    private static final int SIDE_CHANNEL_RETRY_BASE_INTERVAL_MS = 1000;
    /** Maximum retries for a side channel listener before dropping tasks. */
    private static final int SIDE_CHANNEL_RETRY_MAX_COUNT = 6;
    /** Hidden field Settings.Secure.ENABLED_NOTIFICATION_LISTENERS */
    private static final String SETTING_ENABLED_NOTIFICATION_LISTENERS =
            "enabled_notification_listeners";
    private static final int SIDE_CHANNEL_BIND_FLAGS;

    /** Cache of enabled notification listener components */
    private static final Object sEnabledNotificationListenersLock = new Object();
    /** Guarded by {@link #sEnabledNotificationListenersLock} */
    private static String sEnabledNotificationListeners;
    /** Guarded by {@link #sEnabledNotificationListenersLock} */
    private static Set<String> sEnabledNotificationListenerPackages = new HashSet<String>();

    private final Context mContext;
    private final NotificationManager mNotificationManager;
    /** Lock for mutable static fields */
    private static final Object sLock = new Object();
    /** Guarded by {@link #sLock} */
    private static SideChannelManager sSideChannelManager;

    /** Get a {@link NotificationManagerCompat} instance for a provided context. */
    public static NotificationManagerCompat from(Context context) {
        return new NotificationManagerCompat(context);
    }

    private NotificationManagerCompat(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager) mContext.getSystemService(
                Context.NOTIFICATION_SERVICE);
    }

    private static final Impl IMPL;

    interface Impl {
        void cancelNotification(NotificationManager notificationManager, String tag, int id);

        void postNotification(NotificationManager notificationManager, String tag, int id,
                Notification notification);

        int getSideChannelBindFlags();
    }

    static class ImplBase implements Impl {
        @Override
        public void cancelNotification(NotificationManager notificationManager, String tag,
                int id) {
            notificationManager.cancel(id);
        }

        @Override
        public void postNotification(NotificationManager notificationManager, String tag, int id,
                Notification notification) {
            notificationManager.notify(id, notification);
        }

        @Override
        public int getSideChannelBindFlags() {
            return Service.BIND_AUTO_CREATE;
        }
    }

    static class ImplEclair extends ImplBase {
        @Override
        public void cancelNotification(NotificationManager notificationManager, String tag,
                int id) {
            NotificationManagerCompatEclair.cancelNotification(notificationManager, tag, id);
        }

        @Override
        public void postNotification(NotificationManager notificationManager, String tag, int id,
                Notification notification) {
            NotificationManagerCompatEclair.postNotification(notificationManager, tag, id,
                    notification);
        }
    }

    static class ImplIceCreamSandwich extends ImplEclair {
        @Override
        public int getSideChannelBindFlags() {
            return NotificationManagerCompatIceCreamSandwich.SIDE_CHANNEL_BIND_FLAGS;
        }
    }

    static {
        if (Build.VERSION.SDK_INT >= 14) {
            IMPL = new ImplIceCreamSandwich();
        } else if (Build.VERSION.SDK_INT >= 5) {
            IMPL = new ImplEclair();
        } else {
            IMPL = new ImplBase();
        }
        SIDE_CHANNEL_BIND_FLAGS = IMPL.getSideChannelBindFlags();
    }

    /**
     * Cancel a previously shown notification.
     * @param id the ID of the notification
     */
    public void cancel(int id) {
        cancel(null, id);
    }

    /**
     * Cancel a previously shown notification.
     * @param tag the string identifier of the notification.
     * @param id the ID of the notification
     */
    public void cancel(String tag, int id) {
        IMPL.cancelNotification(mNotificationManager, tag, id);
        if (Build.VERSION.SDK_INT <= MAX_SIDE_CHANNEL_SDK_VERSION) {
            pushSideChannelQueue(new CancelTask(mContext.getPackageName(), id, tag));
        }
    }

    /** Cancel all previously shown notifications. */
    public void cancelAll() {
        mNotificationManager.cancelAll();
        if (Build.VERSION.SDK_INT <= MAX_SIDE_CHANNEL_SDK_VERSION) {
            pushSideChannelQueue(new CancelTask(mContext.getPackageName()));
        }
    }

    /**
     * Post a notification to be shown in the status bar, stream, etc.
     * @param id the ID of the notification
     * @param notification the notification to post to the system
     */
    public void notify(int id, Notification notification) {
        notify(null, id, notification);
    }

    /**
     * Post a notification to be shown in the status bar, stream, etc.
     * @param tag the string identifier for a notification. Can be {@code null}.
     * @param id the ID of the notification. The pair (tag, id) must be unique within your app.
     * @param notification the notification to post to the system
    */
    public void notify(String tag, int id, Notification notification) {
        if (useSideChannelForNotification(notification)) {
            pushSideChannelQueue(new NotifyTask(mContext.getPackageName(), id, tag, notification));
            // Cancel this notification in notification manager if it just transitioned to being
            // side channelled.
            IMPL.cancelNotification(mNotificationManager, tag, id);
        } else {
            IMPL.postNotification(mNotificationManager, tag, id, notification);
        }
    }

    /**
     * Get the set of packages that have an enabled notification listener component within them.
     */
    public static Set<String> getEnabledListenerPackages(Context context) {
        final String enabledNotificationListeners = Settings.Secure.getString(
                context.getContentResolver(),
                SETTING_ENABLED_NOTIFICATION_LISTENERS);
        // Parse the string again if it is different from the last time this method was called.
        if (enabledNotificationListeners != null
                && !enabledNotificationListeners.equals(sEnabledNotificationListeners)) {
            final String[] components = enabledNotificationListeners.split(":");
            Set<String> packageNames = new HashSet<String>(components.length);
            for (String component : components) {
                ComponentName componentName = ComponentName.unflattenFromString(component);
                if (componentName != null) {
                    packageNames.add(componentName.getPackageName());
                }
            }
            synchronized (sEnabledNotificationListenersLock) {
                sEnabledNotificationListenerPackages = packageNames;
                sEnabledNotificationListeners = enabledNotificationListeners;
            }
        }
        return sEnabledNotificationListenerPackages;
    }

    /**
     * Returns true if this notification should use the side channel for delivery.
     */
    private static boolean useSideChannelForNotification(Notification notification) {
        Bundle extras = NotificationCompat.getExtras(notification);
        return extras != null && extras.getBoolean(EXTRA_USE_SIDE_CHANNEL);
    }

    /**
     * Push a notification task for distribution to notification side channels.
     */
    private void pushSideChannelQueue(Task task) {
        synchronized (sLock) {
            if (sSideChannelManager == null) {
                sSideChannelManager = new SideChannelManager(mContext.getApplicationContext());
            }
        }
        sSideChannelManager.queueTask(task);
    }

    /**
     * Helper class to manage a queue of pending tasks to send to notification side channel
     * listeners.
     */
    private static class SideChannelManager implements Handler.Callback, ServiceConnection {
        private static final int MSG_QUEUE_TASK = 0;
        private static final int MSG_SERVICE_CONNECTED = 1;
        private static final int MSG_SERVICE_DISCONNECTED = 2;
        private static final int MSG_RETRY_LISTENER_QUEUE = 3;

        private static final String KEY_BINDER = "binder";

        private final Context mContext;
        private final HandlerThread mHandlerThread;
        private final Handler mHandler;
        private final Map<ComponentName, ListenerRecord> mRecordMap =
                new HashMap<ComponentName, ListenerRecord>();
        private Set<String> mCachedEnabledPackages = new HashSet<String>();

        public SideChannelManager(Context context) {
            mContext = context;
            mHandlerThread = new HandlerThread("NotificationManagerCompat");
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper(), this);
        }

        /**
         * Queue a new task to be sent to all listeners. This function can be called
         * from any thread.
         */
        public void queueTask(Task task) {
            mHandler.obtainMessage(MSG_QUEUE_TASK, task).sendToTarget();
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_QUEUE_TASK:
                    handleQueueTask((Task) msg.obj);
                    return true;
                case MSG_SERVICE_CONNECTED:
                    ServiceConnectedEvent event = (ServiceConnectedEvent) msg.obj;
                    handleServiceConnected(event.componentName, event.iBinder);
                    return true;
                case MSG_SERVICE_DISCONNECTED:
                    handleServiceDisconnected((ComponentName) msg.obj);
                    return true;
                case MSG_RETRY_LISTENER_QUEUE:
                    handleRetryListenerQueue((ComponentName) msg.obj);
                    return true;
            }
            return false;
        }

        private void handleQueueTask(Task task) {
            updateListenerMap();
            for (ListenerRecord record : mRecordMap.values()) {
                record.taskQueue.add(task);
                processListenerQueue(record);
            }
        }

        private void handleServiceConnected(ComponentName componentName, IBinder iBinder) {
            ListenerRecord record = mRecordMap.get(componentName);
            if (record != null) {
                record.service = INotificationSideChannel.Stub.asInterface(iBinder);
                record.retryCount = 0;
                processListenerQueue(record);
            }
        }

        private void handleServiceDisconnected(ComponentName componentName) {
            ListenerRecord record = mRecordMap.get(componentName);
            if (record != null) {
                ensureServiceUnbound(record);
            }
        }

        private void handleRetryListenerQueue(ComponentName componentName) {
            ListenerRecord record = mRecordMap.get(componentName);
            if (record != null) {
                processListenerQueue(record);
            }
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Connected to service " + componentName);
            }
            mHandler.obtainMessage(MSG_SERVICE_CONNECTED,
                    new ServiceConnectedEvent(componentName, iBinder))
                    .sendToTarget();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Disconnected from service " + componentName);
            }
            mHandler.obtainMessage(MSG_SERVICE_DISCONNECTED, componentName).sendToTarget();
        }

        /**
         * Check the current list of enabled listener packages and update the records map
         * accordingly.
         */
        private void updateListenerMap() {
            Set<String> enabledPackages = getEnabledListenerPackages(mContext);
            if (enabledPackages.equals(mCachedEnabledPackages)) {
                // Short-circuit when the list of enabled packages has not changed.
                return;
            }
            mCachedEnabledPackages = enabledPackages;
            List<ResolveInfo> resolveInfos = mContext.getPackageManager().queryIntentServices(
                    new Intent().setAction(ACTION_BIND_SIDE_CHANNEL), PackageManager.GET_SERVICES);
            Set<ComponentName> enabledComponents = new HashSet<ComponentName>();
            for (ResolveInfo resolveInfo : resolveInfos) {
                if (!enabledPackages.contains(resolveInfo.serviceInfo.packageName)) {
                    continue;
                }
                ComponentName componentName = new ComponentName(
                        resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
                if (resolveInfo.serviceInfo.permission != null) {
                    Log.w(TAG, "Permission present on component " + componentName
                            + ", not adding listener record.");
                    continue;
                }
                enabledComponents.add(componentName);
            }
            // Ensure all enabled components have a record in the listener map.
            for (ComponentName componentName : enabledComponents) {
                if (!mRecordMap.containsKey(componentName)) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Adding listener record for " + componentName);
                    }
                    mRecordMap.put(componentName, new ListenerRecord(componentName));
                }
            }
            // Remove listener records that are no longer for enabled components.
            Iterator<Map.Entry<ComponentName, ListenerRecord>> it =
                    mRecordMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<ComponentName, ListenerRecord> entry = it.next();
                if (!enabledComponents.contains(entry.getKey())) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Removing listener record for " + entry.getKey());
                    }
                    ensureServiceUnbound(entry.getValue());
                    it.remove();
                }
            }
        }

        /**
         * Ensure we are already attempting to bind to a service, or start a new binding if not.
         * @return Whether the service bind attempt was successful.
         */
        private boolean ensureServiceBound(ListenerRecord record) {
            if (record.bound) {
                return true;
            }
            Intent intent = new Intent(ACTION_BIND_SIDE_CHANNEL).setComponent(record.componentName);
            record.bound = mContext.bindService(intent, this, SIDE_CHANNEL_BIND_FLAGS);
            if (record.bound) {
                record.retryCount = 0;
            } else {
                Log.w(TAG, "Unable to bind to listener " + record.componentName);
                mContext.unbindService(this);
            }
            return record.bound;
        }

        /**
         * Ensure we have unbound from a service.
         */
        private void ensureServiceUnbound(ListenerRecord record) {
            if (record.bound) {
                mContext.unbindService(this);
                record.bound = false;
            }
            record.service = null;
        }

        /**
         * Schedule a delayed retry to communicate with a listener service.
         * After a maximum number of attempts (with exponential back-off), start
         * dropping pending tasks for this listener.
         */
        private void scheduleListenerRetry(ListenerRecord record) {
            if (mHandler.hasMessages(MSG_RETRY_LISTENER_QUEUE, record.componentName)) {
                return;
            }
            record.retryCount++;
            if (record.retryCount > SIDE_CHANNEL_RETRY_MAX_COUNT) {
                Log.w(TAG, "Giving up on delivering " + record.taskQueue.size() + " tasks to "
                        + record.componentName + " after " + record.retryCount + " retries");
                record.taskQueue.clear();
                return;
            }
            int delayMs = SIDE_CHANNEL_RETRY_BASE_INTERVAL_MS * (1 << (record.retryCount - 1));
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Scheduling retry for " + delayMs + " ms");
            }
            Message msg = mHandler.obtainMessage(MSG_RETRY_LISTENER_QUEUE, record.componentName);
            mHandler.sendMessageDelayed(msg, delayMs);
        }

        /**
         * Perform a processing step for a listener. First check the bind state, then attempt
         * to flush the task queue, and if an error is encountered, schedule a retry.
         */
        private void processListenerQueue(ListenerRecord record) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Processing component " + record.componentName + ", "
                        + record.taskQueue.size() + " queued tasks");
            }
            if (record.taskQueue.isEmpty()) {
                return;
            }
            if (!ensureServiceBound(record) || record.service == null) {
                // Ensure bind has started and that a service interface is ready to use.
                scheduleListenerRetry(record);
                return;
            }
            // Attempt to flush all items in the task queue.
            while (true) {
                Task task = record.taskQueue.peek();
                if (task == null) {
                    break;
                }
                try {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Sending task " + task);
                    }
                    task.send(record.service);
                    record.taskQueue.remove();
                } catch (DeadObjectException e) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Remote service has died: " + record.componentName);
                    }
                    break;
                } catch (RemoteException e) {
                    Log.w(TAG, "RemoteException communicating with " + record.componentName, e);
                    break;
                }
            }
            if (!record.taskQueue.isEmpty()) {
                // Some tasks were not sent, meaning an error was encountered, schedule a retry.
                scheduleListenerRetry(record);
            }
        }

        /** A per-side-channel-service listener state record */
        private static class ListenerRecord {
            public final ComponentName componentName;
            /** Whether the service is currently bound to. */
            public boolean bound = false;
            /** The service stub provided by onServiceConnected */
            public INotificationSideChannel service;
            /** Queue of pending tasks to send to this listener service */
            public LinkedList<Task> taskQueue = new LinkedList<Task>();
            /** Number of retries attempted while connecting to this listener service */
            public int retryCount = 0;

            public ListenerRecord(ComponentName componentName) {
                this.componentName = componentName;
            }
        }
    }

    private static class ServiceConnectedEvent {
        final ComponentName componentName;
        final IBinder iBinder;

        public ServiceConnectedEvent(ComponentName componentName,
                final IBinder iBinder) {
            this.componentName = componentName;
            this.iBinder = iBinder;
        }
    }

    private interface Task {
        public void send(INotificationSideChannel service) throws RemoteException;
    }

    private static class NotifyTask implements Task {
        final String packageName;
        final int id;
        final String tag;
        final Notification notif;

        public NotifyTask(String packageName, int id, String tag, Notification notif) {
            this.packageName = packageName;
            this.id = id;
            this.tag = tag;
            this.notif = notif;
        }

        @Override
        public void send(INotificationSideChannel service) throws RemoteException {
            service.notify(packageName, id, tag, notif);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("NotifyTask[");
            sb.append("packageName:").append(packageName);
            sb.append(", id:").append(id);
            sb.append(", tag:").append(tag);
            sb.append("]");
            return sb.toString();
        }
    }

    private static class CancelTask implements Task {
        final String packageName;
        final int id;
        final String tag;
        final boolean all;

        public CancelTask(String packageName) {
            this.packageName = packageName;
            this.id = 0;
            this.tag = null;
            this.all = true;
        }

        public CancelTask(String packageName, int id, String tag) {
            this.packageName = packageName;
            this.id = id;
            this.tag = tag;
            this.all = false;
        }

        @Override
        public void send(INotificationSideChannel service) throws RemoteException {
            if (all) {
                service.cancelAll(packageName);
            } else {
                service.cancel(packageName, id, tag);
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("CancelTask[");
            sb.append("packageName:").append(packageName);
            sb.append(", id:").append(id);
            sb.append(", tag:").append(tag);
            sb.append(", all:").append(all);
            sb.append("]");
            return sb.toString();
        }
    }
}
