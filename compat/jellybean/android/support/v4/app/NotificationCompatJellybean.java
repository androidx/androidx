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
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.util.SparseArray;

import java.lang.reflect.Field;
import java.util.List;

@RequiresApi(16)
class NotificationCompatJellybean {
    public static final String TAG = "NotificationCompat";

    // Extras keys used for Jellybean SDK and above.
    static final String EXTRA_DATA_ONLY_REMOTE_INPUTS = "android.support.dataRemoteInputs";
    static final String EXTRA_ALLOW_GENERATED_REPLIES = "android.support.allowGeneratedReplies";

    // Bundle keys for storing action fields in a bundle
    private static final String KEY_ICON = "icon";
    private static final String KEY_TITLE = "title";
    private static final String KEY_ACTION_INTENT = "actionIntent";
    private static final String KEY_EXTRAS = "extras";
    private static final String KEY_REMOTE_INPUTS = "remoteInputs";
    private static final String KEY_DATA_ONLY_REMOTE_INPUTS = "dataOnlyRemoteInputs";

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
        RemoteInputCompatBase.RemoteInput[] dataOnlyRemoteInputs = null;
        boolean allowGeneratedReplies = false;
        if (extras != null) {
            remoteInputs = RemoteInputCompatJellybean.fromBundleArray(
                    BundleUtil.getBundleArrayFromBundle(extras,
                            NotificationCompatExtras.EXTRA_REMOTE_INPUTS),
                    remoteInputFactory);
            dataOnlyRemoteInputs = RemoteInputCompatJellybean.fromBundleArray(
                    BundleUtil.getBundleArrayFromBundle(extras, EXTRA_DATA_ONLY_REMOTE_INPUTS),
                    remoteInputFactory);
            allowGeneratedReplies = extras.getBoolean(EXTRA_ALLOW_GENERATED_REPLIES);
        }
        return factory.build(icon, title, actionIntent, extras, remoteInputs,
                dataOnlyRemoteInputs, allowGeneratedReplies);
    }

    public static Bundle writeActionAndGetExtras(
            Notification.Builder builder, NotificationCompatBase.Action action) {
        builder.addAction(action.getIcon(), action.getTitle(), action.getActionIntent());
        Bundle actionExtras = new Bundle(action.getExtras());
        if (action.getRemoteInputs() != null) {
            actionExtras.putParcelableArray(NotificationCompatExtras.EXTRA_REMOTE_INPUTS,
                    RemoteInputCompatJellybean.toBundleArray(action.getRemoteInputs()));
        }
        if (action.getDataOnlyRemoteInputs() != null) {
            actionExtras.putParcelableArray(EXTRA_DATA_ONLY_REMOTE_INPUTS,
                    RemoteInputCompatJellybean.toBundleArray(action.getDataOnlyRemoteInputs()));
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
                Object[] actionObjects = getActionObjectsLocked(notif);
                if (actionObjects != null) {
                    Object actionObject = actionObjects[actionIndex];
                    Bundle actionExtras = null;
                    Bundle extras = getExtras(notif);
                    if (extras != null) {
                        SparseArray<Bundle> actionExtrasMap = extras.getSparseParcelableArray(
                                NotificationCompatExtras.EXTRA_ACTION_EXTRAS);
                        if (actionExtrasMap != null) {
                            actionExtras = actionExtrasMap.get(actionIndex);
                        }
                    }
                    return readAction(factory, remoteInputFactory,
                            sActionIconField.getInt(actionObject),
                            (CharSequence) sActionTitleField.get(actionObject),
                            (PendingIntent) sActionIntentField.get(actionObject),
                            actionExtras);
                }
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

    @SuppressWarnings("LiteralClassName")
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

    static NotificationCompatBase.Action getActionFromBundle(Bundle bundle,
            NotificationCompatBase.Action.Factory actionFactory,
            RemoteInputCompatBase.RemoteInput.Factory remoteInputFactory) {
        Bundle extras = bundle.getBundle(KEY_EXTRAS);
        boolean allowGeneratedReplies = false;
        if (extras != null) {
            allowGeneratedReplies = extras.getBoolean(EXTRA_ALLOW_GENERATED_REPLIES, false);
        }
        return actionFactory.build(
                bundle.getInt(KEY_ICON),
                bundle.getCharSequence(KEY_TITLE),
                bundle.<PendingIntent>getParcelable(KEY_ACTION_INTENT),
                bundle.getBundle(KEY_EXTRAS),
                RemoteInputCompatJellybean.fromBundleArray(
                        BundleUtil.getBundleArrayFromBundle(bundle, KEY_REMOTE_INPUTS),
                        remoteInputFactory),
                RemoteInputCompatJellybean.fromBundleArray(
                        BundleUtil.getBundleArrayFromBundle(bundle, KEY_DATA_ONLY_REMOTE_INPUTS),
                        remoteInputFactory),
                allowGeneratedReplies);
    }

    static Bundle getBundleForAction(NotificationCompatBase.Action action) {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_ICON, action.getIcon());
        bundle.putCharSequence(KEY_TITLE, action.getTitle());
        bundle.putParcelable(KEY_ACTION_INTENT, action.getActionIntent());
        Bundle actionExtras;
        if (action.getExtras() != null) {
            actionExtras = new Bundle(action.getExtras());
        } else {
            actionExtras = new Bundle();
        }
        actionExtras.putBoolean(NotificationCompatJellybean.EXTRA_ALLOW_GENERATED_REPLIES,
                action.getAllowGeneratedReplies());
        bundle.putBundle(KEY_EXTRAS, actionExtras);
        bundle.putParcelableArray(KEY_REMOTE_INPUTS, RemoteInputCompatJellybean.toBundleArray(
                action.getRemoteInputs()));
        return bundle;
    }
}
