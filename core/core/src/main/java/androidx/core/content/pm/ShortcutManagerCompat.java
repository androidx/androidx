/**
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.core.content.pm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper for accessing features in {@link android.content.pm.ShortcutManager}.
 */
public class ShortcutManagerCompat {

    /**
     * Include manifest shortcuts in the result.
     *
     * @see #getShortcuts
     */
    public static final int FLAG_MATCH_MANIFEST = 1 << 0;

    /**
     * Include dynamic shortcuts in the result.
     *
     * @see #getShortcuts
     */
    public static final int FLAG_MATCH_DYNAMIC = 1 << 1;

    /**
     * Include pinned shortcuts in the result.
     *
     * @see #getShortcuts
     */
    public static final int FLAG_MATCH_PINNED = 1 << 2;

    /**
     * Include cached shortcuts in the result.
     *
     * @see #getShortcuts
     */
    public static final int FLAG_MATCH_CACHED = 1 << 3;

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(flag = true, value = {
            FLAG_MATCH_MANIFEST,
            FLAG_MATCH_DYNAMIC,
            FLAG_MATCH_PINNED,
            FLAG_MATCH_CACHED,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ShortcutMatchFlags {}

    @VisibleForTesting static final String ACTION_INSTALL_SHORTCUT =
            "com.android.launcher.action.INSTALL_SHORTCUT";
    @VisibleForTesting static final String INSTALL_SHORTCUT_PERMISSION =
            "com.android.launcher.permission.INSTALL_SHORTCUT";

    /**
     * Key to get the shortcut ID from extras of a share intent.
     *
     * When user selects a direct share item from ShareSheet, the app will receive a share intent
     * which includes the ID of the corresponding shortcut in the extras field.
     */
    public static final String EXTRA_SHORTCUT_ID = "android.intent.extra.shortcut.ID";

    /**
     * ShortcutInfoCompatSaver instance that provides APIs to persist shortcuts locally.
     *
     * Will be instantiated by reflection to load an implementation from another module if possible.
     * If fails to load an implementation via reflection, will use the default implementation which
     * is no-op to avoid unnecessary disk I/O.
     */
    private static volatile ShortcutInfoCompatSaver<?> sShortcutInfoCompatSaver = null;

    private ShortcutManagerCompat() {
        /* Hide constructor */
    }

    /**
     * @return {@code true} if the launcher supports {@link #requestPinShortcut},
     * {@code false} otherwise
     */
    public static boolean isRequestPinShortcutSupported(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            return context.getSystemService(ShortcutManager.class).isRequestPinShortcutSupported();
        }

        if (ContextCompat.checkSelfPermission(context, INSTALL_SHORTCUT_PERMISSION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        for (ResolveInfo info : context.getPackageManager().queryBroadcastReceivers(
                new Intent(ACTION_INSTALL_SHORTCUT), 0)) {
            String permission = info.activityInfo.permission;
            if (TextUtils.isEmpty(permission) || INSTALL_SHORTCUT_PERMISSION.equals(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Request to create a pinned shortcut.
     * <p>On API <= 25 it creates a legacy shortcut with the provided icon, label and intent. For
     * newer APIs it will create a {@link android.content.pm.ShortcutInfo} object which can be
     * updated by the app.
     *
     * <p>Use {@link android.app.PendingIntent#getIntentSender()} to create a {@link IntentSender}.
     *
     * @param shortcut new shortcut to pin
     * @param callback if not null, this intent will be sent when the shortcut is pinned
     *
     * @return {@code true} if the launcher supports this feature
     *
     * @see #isRequestPinShortcutSupported
     * @see IntentSender
     * @see android.app.PendingIntent#getIntentSender()
     */
    public static boolean requestPinShortcut(@NonNull final Context context,
            @NonNull ShortcutInfoCompat shortcut, @Nullable final IntentSender callback) {
        if (Build.VERSION.SDK_INT >= 26) {
            return context.getSystemService(ShortcutManager.class).requestPinShortcut(
                    shortcut.toShortcutInfo(), callback);
        }

        if (!isRequestPinShortcutSupported(context)) {
            return false;
        }
        Intent intent = shortcut.addToIntent(new Intent(ACTION_INSTALL_SHORTCUT));

        // If the callback is null, just send the broadcast
        if (callback == null) {
            context.sendBroadcast(intent);
            return true;
        }

        // Otherwise send the callback when the intent has successfully been dispatched.
        context.sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    callback.sendIntent(context, 0, null, null, null);
                } catch (IntentSender.SendIntentException e) {
                    // Ignore
                }
            }
        }, null, Activity.RESULT_OK, null, null);
        return true;
    }

    /**
     * Returns an Intent which can be used by the launcher to pin shortcut.
     * <p>This should be used by an Activity to set result in response to
     * {@link Intent#ACTION_CREATE_SHORTCUT}.
     *
     * @param shortcut new shortcut to pin
     * @return the intent that should be set as the result for the calling activity
     *
     * @see Intent#ACTION_CREATE_SHORTCUT
     */
    @NonNull
    public static Intent createShortcutResultIntent(@NonNull Context context,
            @NonNull ShortcutInfoCompat shortcut) {
        Intent result = null;
        if (Build.VERSION.SDK_INT >= 26) {
            result = context.getSystemService(ShortcutManager.class)
                    .createShortcutResultIntent(shortcut.toShortcutInfo());
        }
        if (result == null) {
            result = new Intent();
        }
        return shortcut.addToIntent(result);
    }

    /**
     * Returns {@link ShortcutInfoCompat}s that match {@code matchFlags}.
     *
     * @param matchFlags result includes shortcuts matching this flags. Any combination of:
     * <ul>
     *     <li>{@link #FLAG_MATCH_MANIFEST}
     *     <li>{@link #FLAG_MATCH_DYNAMIC}
     *     <li>{@link #FLAG_MATCH_PINNED}
     *     <li>{@link #FLAG_MATCH_CACHED}
     * </ul>
     *
     * Compatibility behavior:
     * <ul>
     *      <li>API 30 and above, this method matches platform behavior.
     *      <li>API 25 through 29, this method aggregates the result from corresponding platform
     *                   api.
     *      <li>API 24 and earlier, this method can only returns dynamic shortcut. Calling this
     *                   method with other flag will be ignored.
     * </ul>
     *
     * @return list of {@link ShortcutInfoCompat}s that match the flag.
     *
     * <p>At least one of the {@code MATCH} flags should be set. Otherwise no shortcuts will be
     * returned.
     *
     * @throws IllegalStateException when the user is locked.
     */
    @NonNull
    public static List<ShortcutInfoCompat> getShortcuts(@NonNull final Context context,
            @ShortcutMatchFlags int matchFlags) {
        if (Build.VERSION.SDK_INT >= 30) {
            final List<ShortcutInfo> shortcuts =
                    context.getSystemService(ShortcutManager.class).getShortcuts(matchFlags);
            return ShortcutInfoCompat.fromShortcuts(context, shortcuts);
        } else if (Build.VERSION.SDK_INT >= 25) {
            final ShortcutManager manager = context.getSystemService(ShortcutManager.class);
            final List<ShortcutInfo> shortcuts = new ArrayList<>();
            if ((matchFlags & FLAG_MATCH_MANIFEST) != 0) {
                shortcuts.addAll(manager.getManifestShortcuts());
            }
            if ((matchFlags & FLAG_MATCH_DYNAMIC) != 0) {
                shortcuts.addAll(manager.getDynamicShortcuts());
            }
            if ((matchFlags & FLAG_MATCH_PINNED) != 0) {
                shortcuts.addAll(manager.getPinnedShortcuts());
            }
            return ShortcutInfoCompat.fromShortcuts(context, shortcuts);
        }
        if ((matchFlags & FLAG_MATCH_DYNAMIC) != 0) {
            try {
                return getShortcutInfoSaverInstance(context).getShortcuts();
            } catch (Exception e) {
                // Ignore
            }
        }
        return Collections.emptyList();
    }

    /**
     * Publish the list of dynamic shortcuts. If there are already dynamic or pinned shortcuts with
     * the same IDs, each mutable shortcut is updated.
     *
     * <p>This API will be rate-limited.
     *
     * @return {@code true} if the call has succeeded. {@code false} if the call fails or is
     * rate-limited.
     *
     * @throws IllegalArgumentException if {@link #getMaxShortcutCountPerActivity(Context)} is
     * exceeded, or when trying to update immutable shortcuts.
     */
    public static boolean addDynamicShortcuts(@NonNull Context context,
            @NonNull List<ShortcutInfoCompat> shortcutInfoList) {
        if (Build.VERSION.SDK_INT >= 25) {
            ArrayList<ShortcutInfo> shortcuts = new ArrayList<>();
            for (ShortcutInfoCompat item : shortcutInfoList) {
                shortcuts.add(item.toShortcutInfo());
            }
            if (!context.getSystemService(ShortcutManager.class).addDynamicShortcuts(shortcuts)) {
                return false;
            }
        }

        getShortcutInfoSaverInstance(context).addShortcuts(shortcutInfoList);
        return true;
    }

    /**
     * @return The maximum number of static and dynamic shortcuts that each launcher icon
     * can have at a time.
     */
    public static int getMaxShortcutCountPerActivity(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 25) {
            return context.getSystemService(ShortcutManager.class).getMaxShortcutCountPerActivity();
        }

        // TODO: decide on this limit when ShortcutManager is not available.
        return 0;
    }

    /**
     * Return all dynamic shortcuts from the caller app.
     *
     * <p>This API is intended to be used for examining what shortcuts are currently published.
     * Re-publishing returned {@link ShortcutInfo}s via APIs such as
     * {@link #addDynamicShortcuts(Context, List)} may cause loss of information such as icons.
     */
    @NonNull
    public static List<ShortcutInfoCompat> getDynamicShortcuts(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 25) {
            List<ShortcutInfo> shortcuts = context.getSystemService(
                    ShortcutManager.class).getDynamicShortcuts();
            List<ShortcutInfoCompat> compats = new ArrayList<>(shortcuts.size());
            for (ShortcutInfo item : shortcuts) {
                compats.add(new ShortcutInfoCompat.Builder(context, item).build());
            }
            return compats;
        }

        try {
            return getShortcutInfoSaverInstance(context).getShortcuts();
        } catch (Exception e) {
            /* Do nothing */
        }

        return new ArrayList<>();
    }

    /**
     * Update all existing shortcuts with the same IDs. Target shortcuts may be pinned and/or
     * dynamic, but they must not be immutable.
     *
     * <p>This API will be rate-limited.
     *
     * @return {@code true} if the call has succeeded. {@code false} if the call fails or is
     * rate-limited.
     *
     * @throws IllegalArgumentException If trying to update immutable shortcuts.
     */
    public static boolean updateShortcuts(@NonNull Context context,
            @NonNull List<ShortcutInfoCompat> shortcutInfoList) {
        if (Build.VERSION.SDK_INT >= 25) {
            ArrayList<ShortcutInfo> shortcuts = new ArrayList<>();
            for (ShortcutInfoCompat item : shortcutInfoList) {
                shortcuts.add(item.toShortcutInfo());
            }
            if (!context.getSystemService(ShortcutManager.class).updateShortcuts(shortcuts)) {
                return false;
            }
        }

        getShortcutInfoSaverInstance(context).addShortcuts(shortcutInfoList);
        return true;
    }

    /**
     * Disable pinned shortcuts, showing the user a custom error message when they try to select
     * the disabled shortcuts.
     * For more details, read
     * <a href="/guide/topics/ui/shortcuts/managing-shortcuts.html#disable-shortcuts">
     * Disable shortcuts</a>.
     *
     * Compatibility behavior:
     * <ul>
     *      <li>API 25 and above, this method matches platform behavior.
     *      <li>API 24 and earlier, this method behalves the same as {@link #removeDynamicShortcuts}
     * </ul>
     *
     * @throws IllegalArgumentException If trying to disable immutable shortcuts.
     *
     * @throws IllegalStateException when the user is locked.
     */
    public static void disableShortcuts(@NonNull final Context context,
            @NonNull final List<String> shortcutIds, @Nullable final CharSequence disabledMessage) {
        if (Build.VERSION.SDK_INT >= 25) {
            context.getSystemService(ShortcutManager.class)
                    .disableShortcuts(shortcutIds, disabledMessage);
        }

        getShortcutInfoSaverInstance(context).removeShortcuts(shortcutIds);
    }

    /**
     * Re-enable pinned shortcuts that were previously disabled.  If the target shortcuts
     * are already enabled, this method does nothing.
     *
     * Compatibility behavior:
     * <ul>
     *      <li>API 25 and above, this method matches platform behavior.
     *      <li>API 24 and earlier, this method behalves the same as {@link #addDynamicShortcuts}
     * </ul>
     *
     * @throws IllegalArgumentException If trying to enable immutable shortcuts.
     *
     * @throws IllegalStateException when the user is locked.
     */
    public static void enableShortcuts(@NonNull final Context context,
            @NonNull final List<ShortcutInfoCompat> shortcutInfoList) {
        if (Build.VERSION.SDK_INT >= 25) {
            final ArrayList<String> shortcutIds = new ArrayList<>(shortcutInfoList.size());
            for (ShortcutInfoCompat shortcut : shortcutInfoList) {
                shortcutIds.add(shortcut.mId);
            }
            context.getSystemService(ShortcutManager.class).enableShortcuts(shortcutIds);
        }

        getShortcutInfoSaverInstance(context).addShortcuts(shortcutInfoList);
    }

    /**
     * Delete dynamic shortcuts by ID.
     */
    public static void removeDynamicShortcuts(@NonNull Context context,
            @NonNull List<String> shortcutIds) {
        if (Build.VERSION.SDK_INT >= 25) {
            context.getSystemService(ShortcutManager.class).removeDynamicShortcuts(shortcutIds);
        }

        getShortcutInfoSaverInstance(context).removeShortcuts(shortcutIds);
    }

    /**
     * Delete all dynamic shortcuts from the caller app.
     */
    public static void removeAllDynamicShortcuts(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 25) {
            context.getSystemService(ShortcutManager.class).removeAllDynamicShortcuts();
        }

        getShortcutInfoSaverInstance(context).removeAllShortcuts();
    }

    /**
     * Delete long lived shortcuts by ID.
     *
     * Compatibility behavior:
     * <ul>
     *      <li>API 30 and above, this method matches platform behavior.
     *      <li>API 29 and earlier, this method behalves the same as {@link #removeDynamicShortcuts}
     * </ul>
     *
     * @throws IllegalStateException when the user is locked.
     */
    public static void removeLongLivedShortcuts(@NonNull final Context context,
            @NonNull final List<String> shortcutIds) {
        if (Build.VERSION.SDK_INT < 30) {
            removeDynamicShortcuts(context, shortcutIds);
            return;
        }

        context.getSystemService(ShortcutManager.class).removeLongLivedShortcuts(shortcutIds);
        getShortcutInfoSaverInstance(context).removeShortcuts(shortcutIds);
    }

    @VisibleForTesting
    static void setShortcutInfoCompatSaver(final ShortcutInfoCompatSaver<Void> saver) {
        sShortcutInfoCompatSaver = saver;
    }

    private static ShortcutInfoCompatSaver<?> getShortcutInfoSaverInstance(Context context) {
        if (sShortcutInfoCompatSaver == null) {
            if (Build.VERSION.SDK_INT >= 23) {
                try {
                    ClassLoader loader = ShortcutManagerCompat.class.getClassLoader();
                    Class<?> saver = Class.forName(
                            "androidx.sharetarget.ShortcutInfoCompatSaverImpl", false, loader);
                    Method getInstanceMethod = saver.getMethod("getInstance", Context.class);
                    sShortcutInfoCompatSaver = (ShortcutInfoCompatSaver) getInstanceMethod.invoke(
                            null, context);
                } catch (Exception e) { /* Do nothing */ }
            }

            if (sShortcutInfoCompatSaver == null) {
                // Implementation not available. Instantiate to the default no-op impl.
                sShortcutInfoCompatSaver = new ShortcutInfoCompatSaver.NoopImpl();
            }
        }
        return sShortcutInfoCompatSaver;
    }
}
