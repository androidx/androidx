/*
 * Copyright (C) 2012 The Android Open Source Project
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
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.widget.RemoteViews;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

class NotificationCompatJellybean {
    public static final String TAG = "NotificationCompat";

    /** Extras key used for Jellybean SDK and above. */
    static final String EXTRA_LOCAL_ONLY = "android.support.localOnly";

    /** Extras key used for Jellybean SDK and above. */
    static final String EXTRA_ACTION_EXTRAS = "android.support.actionExtras";

    private static final Object sExtrasLock = new Object();
    private static Field sExtrasField;
    private static boolean sExtrasFieldAccessFailed;

    private static final Object sActionsLock = new Object();
    private static Class<?> sActionClass;
    private static Field sActionsField;
    private static Field sActionIconField;
    private static Field sActionTitleField;
    private static Field sActionIntentField;
    private static boolean sActionsAccessFailed;

    public static class Builder implements NotificationBuilderWithBuilderAccessor,
            NotificationBuilderWithActions {
        private Notification.Builder b;
        private final boolean mLocalOnly;
        private final Bundle mExtras;
        private List<Bundle> mActionExtrasList = new ArrayList<Bundle>();

        public Builder(Context context, Notification n,
                CharSequence contentTitle, CharSequence contentText, CharSequence contentInfo,
                RemoteViews tickerView, int number,
                PendingIntent contentIntent, PendingIntent fullScreenIntent, Bitmap largeIcon,
                int mProgressMax, int mProgress, boolean mProgressIndeterminate,
                boolean useChronometer, int priority, CharSequence subText, boolean localOnly,
                Bundle extras) {
            b = new Notification.Builder(context)
                .setWhen(n.when)
                .setSmallIcon(n.icon, n.iconLevel)
                .setContent(n.contentView)
                .setTicker(n.tickerText, tickerView)
                .setSound(n.sound, n.audioStreamType)
                .setVibrate(n.vibrate)
                .setLights(n.ledARGB, n.ledOnMS, n.ledOffMS)
                .setOngoing((n.flags & Notification.FLAG_ONGOING_EVENT) != 0)
                .setOnlyAlertOnce((n.flags & Notification.FLAG_ONLY_ALERT_ONCE) != 0)
                .setAutoCancel((n.flags & Notification.FLAG_AUTO_CANCEL) != 0)
                .setDefaults(n.defaults)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSubText(subText)
                .setContentInfo(contentInfo)
                .setContentIntent(contentIntent)
                .setDeleteIntent(n.deleteIntent)
                .setFullScreenIntent(fullScreenIntent,
                        (n.flags & Notification.FLAG_HIGH_PRIORITY) != 0)
                .setLargeIcon(largeIcon)
                .setNumber(number)
                .setUsesChronometer(useChronometer)
                .setPriority(priority)
                .setProgress(mProgressMax, mProgress, mProgressIndeterminate);
            mLocalOnly = localOnly;
            mExtras = extras;
        }

        @Override
        public void addAction(int icon, CharSequence title, PendingIntent intent, Bundle extras) {
            b.addAction(icon, title, intent);
            mActionExtrasList.add(extras);
        }

        @Override
        public Notification.Builder getBuilder() {
            return b;
        }

        public Notification build() {
            Notification notif = b.build();
            if (mExtras != null) {
                // Merge in developer provided extras, but let the values already set
                // for keys take precedence.
                Bundle extras = getExtras(notif);
                Bundle mergeBundle = new Bundle(mExtras);
                for (String key : mExtras.keySet()) {
                    if (extras.containsKey(key)) {
                        mergeBundle.remove(key);
                    }
                }
                extras.putAll(mergeBundle);
            }
            if (mLocalOnly) {
                getExtras(notif).putBoolean(EXTRA_LOCAL_ONLY, true);
            }
            SparseArray<Bundle> actionExtrasMap = buildActionExtrasMap(mActionExtrasList);
            if (actionExtrasMap != null) {
                // Add the action extras sparse array if any action was added with extras.
                getExtras(notif).putSparseParcelableArray(EXTRA_ACTION_EXTRAS, actionExtrasMap);
            }
            return notif;
        }
    }

    public static void addBigTextStyle(NotificationBuilderWithBuilderAccessor b,
            CharSequence bigContentTitle, boolean useSummary,
            CharSequence summaryText, CharSequence bigText) {
        Notification.BigTextStyle style = new Notification.BigTextStyle(b.getBuilder())
            .setBigContentTitle(bigContentTitle)
            .bigText(bigText);
        if (useSummary) {
            style.setSummaryText(summaryText);
        }
    }

    public static void addBigPictureStyle(NotificationBuilderWithBuilderAccessor b,
            CharSequence bigContentTitle, boolean useSummary,
            CharSequence summaryText, Bitmap bigPicture, Bitmap bigLargeIcon,
            boolean bigLargeIconSet) {
        Notification.BigPictureStyle style = new Notification.BigPictureStyle(b.getBuilder())
            .setBigContentTitle(bigContentTitle)
            .bigPicture(bigPicture);
        if (bigLargeIconSet) {
            style.bigLargeIcon(bigLargeIcon);
        }
        if (useSummary) {
            style.setSummaryText(summaryText);
        }
    }

    public static void addInboxStyle(NotificationBuilderWithBuilderAccessor b,
            CharSequence bigContentTitle, boolean useSummary,
            CharSequence summaryText, ArrayList<CharSequence> texts) {
        Notification.InboxStyle style = new Notification.InboxStyle(b.getBuilder())
            .setBigContentTitle(bigContentTitle);
        if (useSummary) {
            style.setSummaryText(summaryText);
        }
        for (CharSequence text: texts) {
            style.addLine(text);
        }
    }

    /** Return an SparseArray for action extras or null if none was needed. */
    public static SparseArray<Bundle> buildActionExtrasMap(List<Bundle> actionExtrasList) {
        SparseArray<Bundle> actionExtrasMap = null;
        for (int i = 0, count = actionExtrasList.size(); i < count; i++) {
            Bundle actionExtras = actionExtrasList.get(i);
            if (actionExtras != null) {
                if (actionExtrasMap == null) {
                    actionExtrasMap = new SparseArray<Bundle>();
                }
                actionExtrasMap.put(i, actionExtras);
            }
        }
        return actionExtrasMap;
    }

    /**
     * Get the extras Bundle from a notification using reflection. Extras were present in
     * Jellybean notifications, but the field was private until KitKat.
     */
    public static Bundle getExtras(Notification notif) {
        synchronized (sExtrasLock) {
            if (sExtrasFieldAccessFailed) {
                return null;
            }
            try {
                if (sExtrasField == null) {
                    Field extrasField = Notification.class.getDeclaredField("extras");
                    if (!Bundle.class.isAssignableFrom(extrasField.getType())) {
                        Log.e(TAG, "Notification.extras field is not of type Bundle");
                        sExtrasFieldAccessFailed = true;
                        return null;
                    }
                    extrasField.setAccessible(true);
                    sExtrasField = extrasField;
                }
                Bundle extras = (Bundle) sExtrasField.get(notif);
                if (extras == null) {
                    extras = new Bundle();
                    sExtrasField.set(notif, extras);
                }
                return extras;
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Unable to access notification extras", e);
            } catch (NoSuchFieldException e) {
                Log.e(TAG, "Unable to access notification extras", e);
            }
            sExtrasFieldAccessFailed = true;
            return null;
        }
    }

    public static int getActionCount(Notification notif) {
        synchronized (sActionsLock) {
            Object[] actionObjects = getActionObjectsLocked(notif);
            return actionObjects != null ? actionObjects.length : 0;
        }
    }

    public static void getAction(Notification notif, int actionIndex,
            NotificationActionHolder holder) {
        synchronized (sActionsLock) {
            try {
                Object actionObject = getActionObjectsLocked(notif)[actionIndex];
                Bundle actionExtras = null;
                Bundle extras = getExtras(notif);
                if (extras != null) {
                    SparseArray<Bundle> actionExtrasMap = extras.getSparseParcelableArray(
                            EXTRA_ACTION_EXTRAS);
                    if (actionExtrasMap != null) {
                        actionExtras = actionExtrasMap.get(actionIndex);
                    }
                }
                holder.set(sActionIconField.getInt(actionObject),
                        (CharSequence) sActionTitleField.get(actionObject),
                        (PendingIntent) sActionIntentField.get(actionObject),
                        actionExtras);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Unable to access notification actions", e);
                sActionsAccessFailed = true;
            }
        }
    }

    private static Object[] getActionObjectsLocked(Notification notif) {
        synchronized (sActionsLock) {
            if (!ensureActionReflectionReadyLocked()) {
                return null;
            }
            try {
                return (Object[]) sActionsField.get(notif);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Unable to access notification actions", e);
                sActionsAccessFailed = true;
                return null;
            }
        }
    }

    private static boolean ensureActionReflectionReadyLocked() {
        if (sActionsAccessFailed) {
            return false;
        }
        try {
            if (sActionsField == null) {
                sActionClass = Class.forName("android.app.Notification$Action");
                sActionIconField = sActionClass.getDeclaredField("icon");
                sActionTitleField = sActionClass.getDeclaredField("title");
                sActionIntentField = sActionClass.getDeclaredField("actionIntent");
                sActionsField = Notification.class.getDeclaredField("actions");
                sActionsField.setAccessible(true);
            }
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Unable to access notification actions", e);
            sActionsAccessFailed = true;
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "Unable to access notification actions", e);
            sActionsAccessFailed = true;
        }
        return !sActionsAccessFailed;
    }

    public static boolean getLocalOnly(Notification notif) {
        return getExtras(notif).getBoolean(EXTRA_LOCAL_ONLY);
    }
}
