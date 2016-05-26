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
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;
import android.widget.RemoteViews;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

class NotificationCompatJellybean {
    public static final String TAG = "NotificationCompat";

    // Extras keys used for Jellybean SDK and above.
    static final String EXTRA_LOCAL_ONLY = "android.support.localOnly";
    static final String EXTRA_ACTION_EXTRAS = "android.support.actionExtras";
    static final String EXTRA_REMOTE_INPUTS = "android.support.remoteInputs";
    static final String EXTRA_GROUP_KEY = "android.support.groupKey";
    static final String EXTRA_GROUP_SUMMARY = "android.support.isGroupSummary";
    static final String EXTRA_SORT_KEY = "android.support.sortKey";
    static final String EXTRA_USE_SIDE_CHANNEL = "android.support.useSideChannel";
    static final String EXTRA_ALLOW_GENERATED_REPLIES = "android.support.allowGeneratedReplies";

    // Bundle keys for storing action fields in a bundle
    private static final String KEY_ICON = "icon";
    private static final String KEY_TITLE = "title";
    private static final String KEY_ACTION_INTENT = "actionIntent";
    private static final String KEY_EXTRAS = "extras";
    private static final String KEY_REMOTE_INPUTS = "remoteInputs";
    private static final String KEY_ALLOW_GENERATED_REPLIES = "allowGeneratedReplies";

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
        private final Bundle mExtras;
        private List<Bundle> mActionExtrasList = new ArrayList<Bundle>();
        private RemoteViews mContentView;
        private RemoteViews mBigContentView;

        public Builder(Context context, Notification n,
                CharSequence contentTitle, CharSequence contentText, CharSequence contentInfo,
                RemoteViews tickerView, int number,
                PendingIntent contentIntent, PendingIntent fullScreenIntent, Bitmap largeIcon,
                int progressMax, int progress, boolean progressIndeterminate,
                boolean useChronometer, int priority, CharSequence subText, boolean localOnly,
                Bundle extras, String groupKey, boolean groupSummary, String sortKey,
                RemoteViews contentView, RemoteViews bigContentView) {
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
                .setProgress(progressMax, progress, progressIndeterminate);
            mExtras = new Bundle();
            if (extras != null) {
                mExtras.putAll(extras);
            }
            if (localOnly) {
                mExtras.putBoolean(EXTRA_LOCAL_ONLY, true);
            }
            if (groupKey != null) {
                mExtras.putString(EXTRA_GROUP_KEY, groupKey);
                if (groupSummary) {
                    mExtras.putBoolean(EXTRA_GROUP_SUMMARY, true);
                } else {
                    mExtras.putBoolean(EXTRA_USE_SIDE_CHANNEL, true);
                }
            }
            if (sortKey != null) {
                mExtras.putString(EXTRA_SORT_KEY, sortKey);
            }
            mContentView = contentView;
            mBigContentView = bigContentView;
        }

        @Override
        public void addAction(NotificationCompatBase.Action action) {
            mActionExtrasList.add(writeActionAndGetExtras(b, action));
        }

        @Override
        public Notification.Builder getBuilder() {
            return b;
        }

        public Notification build() {
            Notification notif = b.build();
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
            SparseArray<Bundle> actionExtrasMap = buildActionExtrasMap(mActionExtrasList);
            if (actionExtrasMap != null) {
                // Add the action extras sparse array if any action was added with extras.
                getExtras(notif).putSparseParcelableArray(EXTRA_ACTION_EXTRAS, actionExtrasMap);
            }
            if (mContentView != null) {
                notif.contentView = mContentView;
            }
            if (mBigContentView != null) {
                notif.bigContentView = mBigContentView;
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

    public static NotificationCompatBase.Action readAction(
            NotificationCompatBase.Action.Factory factory,
            RemoteInputCompatBase.RemoteInput.Factory remoteInputFactory, int icon,
            CharSequence title, PendingIntent actionIntent, Bundle extras) {
        RemoteInputCompatBase.RemoteInput[] remoteInputs = null;
        boolean allowGeneratedReplies = false;
        if (extras != null) {
            remoteInputs = RemoteInputCompatJellybean.fromBundleArray(
                    BundleUtil.getBundleArrayFromBundle(extras, EXTRA_REMOTE_INPUTS),
                    remoteInputFactory);
            allowGeneratedReplies = extras.getBoolean(EXTRA_ALLOW_GENERATED_REPLIES);
        }
        return factory.build(icon, title, actionIntent, extras, remoteInputs,
                allowGeneratedReplies);
    }

    public static Bundle writeActionAndGetExtras(
            Notification.Builder builder, NotificationCompatBase.Action action) {
        builder.addAction(action.getIcon(), action.getTitle(), action.getActionIntent());
        Bundle actionExtras = new Bundle(action.getExtras());
        if (action.getRemoteInputs() != null) {
            actionExtras.putParcelableArray(EXTRA_REMOTE_INPUTS,
                    RemoteInputCompatJellybean.toBundleArray(action.getRemoteInputs()));
        }
        actionExtras.putBoolean(EXTRA_ALLOW_GENERATED_REPLIES,
                action.getAllowGeneratedReplies());
        return actionExtras;
    }

    public static int getActionCount(Notification notif) {
        synchronized (sActionsLock) {
            Object[] actionObjects = getActionObjectsLocked(notif);
            return actionObjects != null ? actionObjects.length : 0;
        }
    }

    public static NotificationCompatBase.Action getAction(Notification notif, int actionIndex,
            NotificationCompatBase.Action.Factory factory,
            RemoteInputCompatBase.RemoteInput.Factory remoteInputFactory) {
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
                return readAction(factory, remoteInputFactory,
                        sActionIconField.getInt(actionObject),
                        (CharSequence) sActionTitleField.get(actionObject),
                        (PendingIntent) sActionIntentField.get(actionObject),
                        actionExtras);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Unable to access notification actions", e);
                sActionsAccessFailed = true;
            }
        }
        return null;
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

    public static NotificationCompatBase.Action[] getActionsFromParcelableArrayList(
            ArrayList<Parcelable> parcelables,
            NotificationCompatBase.Action.Factory actionFactory,
            RemoteInputCompatBase.RemoteInput.Factory remoteInputFactory) {
        if (parcelables == null) {
            return null;
        }
        NotificationCompatBase.Action[] actions = actionFactory.newArray(parcelables.size());
        for (int i = 0; i < actions.length; i++) {
            actions[i] = getActionFromBundle((Bundle) parcelables.get(i),
                    actionFactory, remoteInputFactory);
        }
        return actions;
    }

    private static NotificationCompatBase.Action getActionFromBundle(Bundle bundle,
            NotificationCompatBase.Action.Factory actionFactory,
            RemoteInputCompatBase.RemoteInput.Factory remoteInputFactory) {
        return actionFactory.build(
                bundle.getInt(KEY_ICON),
                bundle.getCharSequence(KEY_TITLE),
                bundle.<PendingIntent>getParcelable(KEY_ACTION_INTENT),
                bundle.getBundle(KEY_EXTRAS),
                RemoteInputCompatJellybean.fromBundleArray(
                        BundleUtil.getBundleArrayFromBundle(bundle, KEY_REMOTE_INPUTS),
                        remoteInputFactory), bundle.getBoolean(KEY_ALLOW_GENERATED_REPLIES));
    }

    public static ArrayList<Parcelable> getParcelableArrayListForActions(
            NotificationCompatBase.Action[] actions) {
        if (actions == null) {
            return null;
        }
        ArrayList<Parcelable> parcelables = new ArrayList<Parcelable>(actions.length);
        for (NotificationCompatBase.Action action : actions) {
            parcelables.add(getBundleForAction(action));
        }
        return parcelables;
    }

    private static Bundle getBundleForAction(NotificationCompatBase.Action action) {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_ICON, action.getIcon());
        bundle.putCharSequence(KEY_TITLE, action.getTitle());
        bundle.putParcelable(KEY_ACTION_INTENT, action.getActionIntent());
        bundle.putBundle(KEY_EXTRAS, action.getExtras());
        bundle.putParcelableArray(KEY_REMOTE_INPUTS, RemoteInputCompatJellybean.toBundleArray(
                action.getRemoteInputs()));
        return bundle;
    }

    public static boolean getLocalOnly(Notification notif) {
        return getExtras(notif).getBoolean(EXTRA_LOCAL_ONLY);
    }

    public static String getGroup(Notification n) {
        return getExtras(n).getString(EXTRA_GROUP_KEY);
    }

    public static boolean isGroupSummary(Notification n) {
        return getExtras(n).getBoolean(EXTRA_GROUP_SUMMARY);
    }

    public static String getSortKey(Notification n) {
        return getExtras(n).getString(EXTRA_SORT_KEY);
    }
}
