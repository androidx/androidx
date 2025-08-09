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

import static androidx.core.graphics.drawable.IconCompat.TYPE_URI;
import static androidx.core.graphics.drawable.IconCompat.TYPE_URI_ADAPTIVE_BITMAP;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

    private static final int DEFAULT_MAX_ICON_DIMENSION_DP = 96;
    private static final int DEFAULT_MAX_ICON_DIMENSION_LOWRAM_DP = 48;

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

    /**
     * Will be instantiated by reflection to load an implementation from another module if
     * possible. Modules can declare the class to be instantiated using the meta-data in their
     * Manifest.
     *
     * If fails to load an implementation via reflection, will use the default implementation which
     * is no-op.
     */
    private static volatile List<ShortcutInfoChangeListener> sShortcutInfoChangeListeners = null;

    private static final String SHORTCUT_LISTENER_INTENT_FILTER_ACTION = "androidx.core.content.pm"
            + ".SHORTCUT_LISTENER";
    private static final String SHORTCUT_LISTENER_META_DATA_KEY = "androidx.core.content.pm"
            + ".shortcut_listener_impl";

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
     * @param context context to use for the request.
     * @param shortcut new shortcut to pin
     * @param callback if not null, this intent will be sent when the shortcut is pinned
     *
     * @return {@code true} if the launcher supports this feature
     *
     * @see #isRequestPinShortcutSupported
     * @see IntentSender
     * @see android.app.PendingIntent#getIntentSender()
     */
    public static boolean requestPinShortcut(final @NonNull Context context,
            @NonNull ShortcutInfoCompat shortcut, final @Nullable IntentSender callback) {
        if (Build.VERSION.SDK_INT <= 32
                && shortcut.isExcludedFromSurfaces(ShortcutInfoCompat.SURFACE_LAUNCHER)) {
            // A shortcut that is not frequently used cannot be pinned to WorkSpace.
            return false;
        }
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
     * @param context context to use for the intent.
     * @param shortcut new shortcut to pin
     * @return the intent that should be set as the result for the calling activity
     *
     * @see Intent#ACTION_CREATE_SHORTCUT
     */
    public static @NonNull Intent createShortcutResultIntent(@NonNull Context context,
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
     * @param context context to use for the shortcuts.
     * @return list of {@link ShortcutInfoCompat}s that match the flag.
     *
     * <p>At least one of the {@code MATCH} flags should be set. Otherwise no shortcuts will be
     * returned.
     *
     * @throws IllegalStateException when the user is locked.
     */
    public static @NonNull List<ShortcutInfoCompat> getShortcuts(final @NonNull Context context,
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
     * <p>On API <= 31 Any shortcuts that are marked as excluded from launcher will not be passed
     * to the {@link ShortcutManager}, but they might still be available to assistant and other
     * surfaces through alternative means.
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
        final List<ShortcutInfoCompat> clone = removeShortcutsExcludedFromSurface(
                shortcutInfoList, ShortcutInfoCompat.SURFACE_LAUNCHER);
        if (Build.VERSION.SDK_INT <= 29) {
            convertUriIconsToBitmapIcons(context, clone);
        }
        if (Build.VERSION.SDK_INT >= 25) {
            ArrayList<ShortcutInfo> shortcuts = new ArrayList<>();
            for (ShortcutInfoCompat item : clone) {
                shortcuts.add(item.toShortcutInfo());
            }
            if (!context.getSystemService(ShortcutManager.class).addDynamicShortcuts(shortcuts)) {
                return false;
            }
        }

        getShortcutInfoSaverInstance(context).addShortcuts(clone);
        for (ShortcutInfoChangeListener listener : getShortcutInfoListeners(context)) {
            listener.onShortcutAdded(shortcutInfoList);
        }
        return true;
    }

    /**
     * @return The maximum number of static and dynamic shortcuts that each launcher icon
     * can have at a time.
     */
    public static int getMaxShortcutCountPerActivity(@NonNull Context context) {
        Preconditions.checkNotNull(context);
        if (Build.VERSION.SDK_INT >= 25) {
            return context.getSystemService(ShortcutManager.class).getMaxShortcutCountPerActivity();
        }

        return 5;
    }

    /**
     * Return {@code true} when rate-limiting is active for the caller app.
     *
     * <p>For details, see <a href="/guide/topics/ui/shortcuts/managing-shortcuts#rate-limiting">
     * Rate limiting</a>.
     *
     * @throws IllegalStateException when the user is locked.
     */
    public static boolean isRateLimitingActive(final @NonNull Context context) {
        Preconditions.checkNotNull(context);
        if (Build.VERSION.SDK_INT >= 25) {
            return context.getSystemService(ShortcutManager.class).isRateLimitingActive();
        }

        return getShortcuts(context, FLAG_MATCH_MANIFEST | FLAG_MATCH_DYNAMIC).size()
                == getMaxShortcutCountPerActivity(context);
    }

    /**
     * Return the max width for icons, in pixels.
     *
     * <p> Note that this method returns max width of icon's visible part. Hence, it does not take
     * into account the inset introduced by {@link android.graphics.drawable.AdaptiveIconDrawable}.
     * To calculate bitmap image to function as
     * {@link android.graphics.drawable.AdaptiveIconDrawable}, multiply
     * 1 + 2 * {@link android.graphics.drawable.AdaptiveIconDrawable#getExtraInsetFraction()} to
     * the returned size.
     */
    public static int getIconMaxWidth(final @NonNull Context context) {
        Preconditions.checkNotNull(context);
        if (Build.VERSION.SDK_INT >= 25) {
            return context.getSystemService(ShortcutManager.class).getIconMaxWidth();
        }
        return getIconDimensionInternal(context, true);
    }

    /**
     * Return the max height for icons, in pixels.
     */
    public static int getIconMaxHeight(final @NonNull Context context) {
        Preconditions.checkNotNull(context);
        if (Build.VERSION.SDK_INT >= 25) {
            return context.getSystemService(ShortcutManager.class).getIconMaxHeight();
        }
        return getIconDimensionInternal(context, false);
    }

    /**
     * Apps that publish shortcuts should call this method whenever the user
     * selects the shortcut containing the given ID or when the user completes
     * an action in the app that is equivalent to selecting the shortcut.
     * For more details, read about
     * <a href="/guide/topics/ui/shortcuts/managing-shortcuts.html#track-usage">
     * tracking shortcut usage</a>.
     *
     * <p>The information is accessible via {@link android.app.usage.UsageStatsManager#queryEvents}
     * Typically, launcher apps use this information to build a prediction model
     * so that they can promote the shortcuts that are likely to be used at the moment.
     *
     * @throws IllegalStateException when the user is locked.
     *
     * <p>This method is not supported on devices running SDK < 25 since the platform class will
     * not be available.
     */
    public static void reportShortcutUsed(final @NonNull Context context,
            final @NonNull String shortcutId) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(shortcutId);
        if (Build.VERSION.SDK_INT >= 25) {
            context.getSystemService(ShortcutManager.class).reportShortcutUsed(shortcutId);
        }

        for (ShortcutInfoChangeListener listener : getShortcutInfoListeners(context)) {
            listener.onShortcutUsageReported(Collections.singletonList(shortcutId));
        }
    }

    /**
     * Publish the list of shortcuts.  All existing dynamic shortcuts from the caller app
     * will be replaced.  If there are already pinned shortcuts with the same IDs,
     * the mutable pinned shortcuts are updated.
     * <p>On API <= 31 Any shortcuts that are marked as excluded from launcher will not be passed
     * to the {@link ShortcutManager}, but they might still be available to assistant and other
     * surfaces through alternative means.
     *
     * <p>This API will be rate-limited.
     *
     * Compatibility behavior:
     * <ul>
     *      <li>API 25 and above, this method matches platform behavior.
     *      <li>API 24 and earlier, this method is equivalent of calling
     *      {@link #removeAllDynamicShortcuts} and {@link #addDynamicShortcuts} consecutively.
     * </ul>
     *
     * @return {@code true} if the call has succeeded. {@code false} if the call is rate-limited.
     *
     * @throws IllegalArgumentException if {@link #getMaxShortcutCountPerActivity} is exceeded,
     * or when trying to update immutable shortcuts.
     *
     * @throws IllegalStateException when the user is locked.
     */
    public static boolean setDynamicShortcuts(final @NonNull Context context,
            final @NonNull List<ShortcutInfoCompat> shortcutInfoList) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(shortcutInfoList);
        final List<ShortcutInfoCompat> clone = removeShortcutsExcludedFromSurface(
                shortcutInfoList, ShortcutInfoCompat.SURFACE_LAUNCHER);
        if (Build.VERSION.SDK_INT >= 25) {
            List<ShortcutInfo> shortcuts = new ArrayList<>(clone.size());
            for (ShortcutInfoCompat compat : clone) {
                shortcuts.add(compat.toShortcutInfo());
            }
            if (!context.getSystemService(ShortcutManager.class).setDynamicShortcuts(shortcuts)) {
                return false;
            }
        }
        getShortcutInfoSaverInstance(context).removeAllShortcuts();
        getShortcutInfoSaverInstance(context).addShortcuts(clone);

        for (ShortcutInfoChangeListener listener : getShortcutInfoListeners(context)) {
            listener.onAllShortcutsRemoved();
            listener.onShortcutAdded(shortcutInfoList);
        }
        return true;
    }

    /**
     * Return all dynamic shortcuts from the caller app.
     *
     * <p>This API is intended to be used for examining what shortcuts are currently published.
     * Re-publishing returned {@link ShortcutInfo}s via APIs such as
     * {@link #addDynamicShortcuts(Context, List)} may cause loss of information such as icons.
     */
    public static @NonNull List<ShortcutInfoCompat> getDynamicShortcuts(@NonNull Context context) {
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
     * <p>On API <= 31 Any shortcuts that are marked as excluded from launcher will not be passed
     * to the {@link ShortcutManager}, but they might still be available to assistant and other
     * surfaces through alternative means.
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
        final List<ShortcutInfoCompat> clone = removeShortcutsExcludedFromSurface(
                shortcutInfoList, ShortcutInfoCompat.SURFACE_LAUNCHER);
        if (Build.VERSION.SDK_INT <= 29) {
            convertUriIconsToBitmapIcons(context, clone);
        }
        if (Build.VERSION.SDK_INT >= 25) {
            ArrayList<ShortcutInfo> shortcuts = new ArrayList<>();
            for (ShortcutInfoCompat item : clone) {
                shortcuts.add(item.toShortcutInfo());
            }
            if (!context.getSystemService(ShortcutManager.class).updateShortcuts(shortcuts)) {
                return false;
            }
        }

        getShortcutInfoSaverInstance(context).addShortcuts(clone);
        for (ShortcutInfoChangeListener listener : getShortcutInfoListeners(context)) {
            listener.onShortcutUpdated(shortcutInfoList);
        }
        return true;
    }

    @VisibleForTesting
    static boolean convertUriIconToBitmapIcon(final @NonNull Context context,
            final @NonNull ShortcutInfoCompat info) {
        if (info.mIcon == null) {
            return false;
        }
        final int type = info.mIcon.mType;
        if (type != TYPE_URI_ADAPTIVE_BITMAP && type != TYPE_URI) {
            return true;
        }
        InputStream is = info.mIcon.getUriInputStream(context);
        if (is == null) {
            return false;
        }
        final Bitmap bitmap = BitmapFactory.decodeStream(is);
        if (bitmap == null) {
            return false;
        }
        info.mIcon = (type == TYPE_URI_ADAPTIVE_BITMAP)
                ? IconCompat.createWithAdaptiveBitmap(bitmap)
                : IconCompat.createWithBitmap(bitmap);
        return true;
    }

    @VisibleForTesting
    static void convertUriIconsToBitmapIcons(final @NonNull Context context,
            final @NonNull List<ShortcutInfoCompat> shortcutInfoList) {
        final List<ShortcutInfoCompat> shortcuts = new ArrayList<>(shortcutInfoList);
        for (ShortcutInfoCompat info : shortcuts) {
            if (!convertUriIconToBitmapIcon(context, info)) {
                shortcutInfoList.remove(info);
            }
        }
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
     *      <li>API 24 and earlier, this method behaves the same as {@link #removeDynamicShortcuts}
     * </ul>
     *
     * @throws IllegalArgumentException If trying to disable immutable shortcuts.
     *
     * @throws IllegalStateException when the user is locked.
     */
    public static void disableShortcuts(final @NonNull Context context,
            final @NonNull List<String> shortcutIds, final @Nullable CharSequence disabledMessage) {
        if (Build.VERSION.SDK_INT >= 25) {
            context.getSystemService(ShortcutManager.class)
                    .disableShortcuts(shortcutIds, disabledMessage);
        }

        getShortcutInfoSaverInstance(context).removeShortcuts(shortcutIds);
        for (ShortcutInfoChangeListener listener : getShortcutInfoListeners(context)) {
            listener.onShortcutRemoved(shortcutIds);
        }
    }

    /**
     * Re-enable pinned shortcuts that were previously disabled.  If the target shortcuts
     * are already enabled, this method does nothing.
     * <p>In API 31 and below any shortcuts that are marked as excluded from launcher will be
     * ignored.
     *
     * Compatibility behavior:
     * <ul>
     *      <li>API 25 and above, this method matches platform behavior.
     *      <li>API 24 and earlier, this method behaves the same as {@link #addDynamicShortcuts}
     * </ul>
     *
     * @throws IllegalArgumentException If trying to enable immutable shortcuts.
     *
     * @throws IllegalStateException when the user is locked.
     */
    public static void enableShortcuts(final @NonNull Context context,
            final @NonNull List<ShortcutInfoCompat> shortcutInfoList) {
        final List<ShortcutInfoCompat> clone = removeShortcutsExcludedFromSurface(
                shortcutInfoList, ShortcutInfoCompat.SURFACE_LAUNCHER);
        if (Build.VERSION.SDK_INT >= 25) {
            final ArrayList<String> shortcutIds = new ArrayList<>(shortcutInfoList.size());
            for (ShortcutInfoCompat shortcut : clone) {
                shortcutIds.add(shortcut.mId);
            }
            context.getSystemService(ShortcutManager.class).enableShortcuts(shortcutIds);
        }

        getShortcutInfoSaverInstance(context).addShortcuts(clone);
        for (ShortcutInfoChangeListener listener : getShortcutInfoListeners(context)) {
            listener.onShortcutAdded(shortcutInfoList);
        }
    }

    /**
     * Delete dynamic shortcuts by ID. Note that if a shortcut is set as long-lived, it may still
     * be available in the system as a cached shortcut even after being removed from the list of
     * dynamic shortcuts.
     *
     * @see #removeLongLivedShortcuts
     */
    public static void removeDynamicShortcuts(@NonNull Context context,
            @NonNull List<String> shortcutIds) {
        if (Build.VERSION.SDK_INT >= 25) {
            context.getSystemService(ShortcutManager.class).removeDynamicShortcuts(shortcutIds);
        }

        getShortcutInfoSaverInstance(context).removeShortcuts(shortcutIds);
        for (ShortcutInfoChangeListener listener : getShortcutInfoListeners(context)) {
            listener.onShortcutRemoved(shortcutIds);
        }
    }

    /**
     * Delete all dynamic shortcuts from the caller app. Note that if a shortcut is set as
     * long-lived, it may still be available in the system as a cached shortcut even after being
     * removed from the list of dynamic shortcuts.
     *
     * @see #removeLongLivedShortcuts
     */
    public static void removeAllDynamicShortcuts(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 25) {
            context.getSystemService(ShortcutManager.class).removeAllDynamicShortcuts();
        }

        getShortcutInfoSaverInstance(context).removeAllShortcuts();
        for (ShortcutInfoChangeListener listener : getShortcutInfoListeners(context)) {
            listener.onAllShortcutsRemoved();
        }
    }

    /**
     * Delete long lived shortcuts by ID.
     *
     * Compatibility behavior:
     * <ul>
     *      <li>API 30 and above, this method matches platform behavior.
     *      <li>API 29 and earlier, this method behaves the same as {@link #removeDynamicShortcuts}
     * </ul>
     *
     * @throws IllegalStateException when the user is locked.
     */
    public static void removeLongLivedShortcuts(final @NonNull Context context,
            final @NonNull List<String> shortcutIds) {
        if (Build.VERSION.SDK_INT < 30) {
            removeDynamicShortcuts(context, shortcutIds);
            return;
        }

        context.getSystemService(ShortcutManager.class).removeLongLivedShortcuts(shortcutIds);
        getShortcutInfoSaverInstance(context).removeShortcuts(shortcutIds);
        for (ShortcutInfoChangeListener listener : getShortcutInfoListeners(context)) {
            listener.onShortcutRemoved(shortcutIds);
        }
    }

    /**
     * Publish a single dynamic shortcut. If there are already dynamic or pinned shortcuts with the
     * same ID, each mutable shortcut is updated.
     *
     * <p>This method is useful when posting notifications which are tagged with shortcut IDs; In
     * order to make sure shortcuts exist and are up-to-date, without the need to explicitly handle
     * the shortcut count limit.
     * @see androidx.core.app.NotificationManagerCompat#notify(int, android.app.Notification)
     * @see androidx.core.app.NotificationCompat.Builder#setShortcutId(String)
     *
     * <p>If {@link #getMaxShortcutCountPerActivity} is already reached, an existing shortcut with
     * the lowest rank will be removed to add space for the new shortcut.
     *
     * <p>If the rank of the shortcut is not explicitly set, it will be set to zero, and shortcut
     * will be added to the top of the list.
     *
     * Compatibility behavior:
     * <ul>
     *      <li>API 30 and above, this method matches platform behavior.
     *      <li>API 25 to 29, this api is simulated by
     *      {@link ShortcutManager#addDynamicShortcuts(List)} and
     *      {@link ShortcutManager#removeDynamicShortcuts(List)} and thus will be rate-limited.
     *      <li>API 24 and earlier, this method uses internal implementation and matches platform
     *      behavior.
     * </ul>
     *
     * @return {@code true} if the call has succeeded. {@code false} if the call fails or is
     * rate-limited.
     *
     * @throws IllegalArgumentException if trying to update an immutable shortcut.
     *
     * @throws IllegalStateException when the user is locked.
     */
    public static boolean pushDynamicShortcut(final @NonNull Context context,
            final @NonNull ShortcutInfoCompat shortcut) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(shortcut);

        if (Build.VERSION.SDK_INT <= 32
                && shortcut.isExcludedFromSurfaces(ShortcutInfoCompat.SURFACE_LAUNCHER)) {
            for (ShortcutInfoChangeListener listener : getShortcutInfoListeners(context)) {
                listener.onShortcutAdded(Collections.singletonList(shortcut));
            }
            return true;
        }
        int maxShortcutCount = getMaxShortcutCountPerActivity(context);
        if (maxShortcutCount == 0) {
            return false;
        }
        if (Build.VERSION.SDK_INT <= 29) {
            convertUriIconToBitmapIcon(context, shortcut);
        }
        if (Build.VERSION.SDK_INT >= 30) {
            context.getSystemService(ShortcutManager.class).pushDynamicShortcut(
                    shortcut.toShortcutInfo());
        } else if (Build.VERSION.SDK_INT >= 25) {
            final ShortcutManager sm = context.getSystemService(ShortcutManager.class);
            if (sm.isRateLimitingActive()) {
                return false;
            }
            final List<ShortcutInfo> shortcuts = sm.getDynamicShortcuts();
            if (shortcuts.size() >= maxShortcutCount) {
                sm.removeDynamicShortcuts(Arrays.asList(
                        Api25Impl.getShortcutInfoWithLowestRank(shortcuts)));
            }
            sm.addDynamicShortcuts(Arrays.asList(shortcut.toShortcutInfo()));
        }
        final ShortcutInfoCompatSaver<?> saver = getShortcutInfoSaverInstance(context);
        try {
            final List<ShortcutInfoCompat> oldShortcuts = saver.getShortcuts();
            if (oldShortcuts.size() >= maxShortcutCount) {
                saver.removeShortcuts(Arrays.asList(
                        getShortcutInfoCompatWithLowestRank(oldShortcuts)));
            }
            saver.addShortcuts(Arrays.asList(shortcut));
            return true;
        } catch (Exception e) {
            // Ignore
        } finally {
            for (ShortcutInfoChangeListener listener : getShortcutInfoListeners(context)) {
                listener.onShortcutAdded(Collections.singletonList(shortcut));
            }
            reportShortcutUsed(context, shortcut.getId());
        }
        return false;
    }

    private static String getShortcutInfoCompatWithLowestRank(
            final @NonNull List<ShortcutInfoCompat> shortcuts) {
        int rank = -1;
        String target = null;
        for (ShortcutInfoCompat s : shortcuts) {
            if (s.getRank() > rank) {
                target = s.getId();
                rank = s.getRank();
            }
        }
        return target;
    }

    @VisibleForTesting
    static void setShortcutInfoCompatSaver(final ShortcutInfoCompatSaver<Void> saver) {
        sShortcutInfoCompatSaver = saver;
    }

    @VisibleForTesting
    static void setShortcutInfoChangeListeners(final List<ShortcutInfoChangeListener> listeners) {
        sShortcutInfoChangeListeners = listeners;
    }

    @VisibleForTesting
    static List<ShortcutInfoChangeListener> getShortcutInfoChangeListeners() {
        return sShortcutInfoChangeListeners;
    }

    private static int getIconDimensionInternal(final @NonNull Context context,
            final boolean isHorizontal) {
        final ActivityManager am = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);
        final boolean isLowRamDevice = am == null || am.isLowRamDevice();
        final int iconDimensionDp = Math.max(1, isLowRamDevice
                ? DEFAULT_MAX_ICON_DIMENSION_LOWRAM_DP : DEFAULT_MAX_ICON_DIMENSION_DP);
        final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float density = (isHorizontal ? displayMetrics.xdpi : displayMetrics.ydpi)
                / DisplayMetrics.DENSITY_MEDIUM;
        return (int) (iconDimensionDp * density);
    }

    private static ShortcutInfoCompatSaver<?> getShortcutInfoSaverInstance(Context context) {
        if (sShortcutInfoCompatSaver == null) {
            try {
                ClassLoader loader = ShortcutManagerCompat.class.getClassLoader();
                Class<?> saver = Class.forName(
                        "androidx.sharetarget.ShortcutInfoCompatSaverImpl", false, loader);
                Method getInstanceMethod = saver.getMethod("getInstance", Context.class);
                sShortcutInfoCompatSaver = (ShortcutInfoCompatSaver) getInstanceMethod.invoke(
                        null, context);
            } catch (Exception e) { /* Do nothing */ }

            if (sShortcutInfoCompatSaver == null) {
                // Implementation not available. Instantiate to the default no-op impl.
                sShortcutInfoCompatSaver = new ShortcutInfoCompatSaver.NoopImpl();
            }
        }
        return sShortcutInfoCompatSaver;
    }

    @SuppressWarnings("deprecation")
    private static List<ShortcutInfoChangeListener> getShortcutInfoListeners(Context context) {
        if (sShortcutInfoChangeListeners == null) {
            List<ShortcutInfoChangeListener> result = new ArrayList<>();
            PackageManager packageManager = context.getPackageManager();
            Intent activityIntent = new Intent(SHORTCUT_LISTENER_INTENT_FILTER_ACTION);
            activityIntent.setPackage(context.getPackageName());

            List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(
                    activityIntent, PackageManager.GET_META_DATA);

            for (ResolveInfo resolveInfo : resolveInfos) {
                ActivityInfo activityInfo = resolveInfo.activityInfo;
                if (activityInfo == null) {
                    continue;
                }
                Bundle metaData = activityInfo.metaData;
                if (metaData == null) {
                    continue;
                }
                String shortcutListenerImplName =
                        metaData.getString(SHORTCUT_LISTENER_META_DATA_KEY);
                if (shortcutListenerImplName == null) {
                    continue;
                }
                try {
                    ClassLoader loader = ShortcutManagerCompat.class.getClassLoader();
                    Class<?> listener = Class.forName(shortcutListenerImplName, false, loader);
                    Method getInstanceMethod = listener.getMethod("getInstance", Context.class);
                    result.add((ShortcutInfoChangeListener)
                            getInstanceMethod.invoke(null, context));
                } catch (Exception e) { /* Do nothing */ }
            }

            // Make sure the listeners are not already added while the loop is running.
            if (sShortcutInfoChangeListeners == null) {
                sShortcutInfoChangeListeners = result;
            }
        }
        return sShortcutInfoChangeListeners;
    }

    private static @NonNull List<ShortcutInfoCompat> removeShortcutsExcludedFromSurface(
            final @NonNull List<ShortcutInfoCompat> shortcuts, final int surfaces) {
        Objects.requireNonNull(shortcuts);
        if (Build.VERSION.SDK_INT > 32) return shortcuts;
        final List<ShortcutInfoCompat> clone = new ArrayList<>(shortcuts);
        for (ShortcutInfoCompat si: shortcuts) {
            if (si.isExcludedFromSurfaces(surfaces)) {
                clone.remove(si);
            }
        }
        return clone;
    }

    @RequiresApi(25)
    private static class Api25Impl {
        static String getShortcutInfoWithLowestRank(final @NonNull List<ShortcutInfo> shortcuts) {
            int rank = -1;
            String target = null;
            for (ShortcutInfo s : shortcuts) {
                if (s.getRank() > rank) {
                    target = s.getId();
                    rank = s.getRank();
                }
            }
            return target;
        }
    }
}
