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

package androidx.core.content;

import static android.content.Context.WINDOW_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DisplayContext;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.app.LocaleManagerCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.EnvironmentCompat;
import androidx.core.os.ExecutorCompat;
import androidx.core.os.LocaleListCompat;
import androidx.core.util.ObjectsCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * Helper for accessing features in {@link Context}.
 */
@SuppressLint("PrivateConstructorForUtilityClass") // Already launched with public constructor
public class ContextCompat {
    private static final String TAG = "ContextCompat";

    /**
     * This class should not be instantiated, but the constructor must be
     * visible for the class to be extended (ex. in ActivityCompat).
     */
    protected ContextCompat() {
        // Not publicly instantiable, but may be extended.
    }

    private static final String DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION_SUFFIX =
            ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION";


    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(flag = true, value = {
            RECEIVER_VISIBLE_TO_INSTANT_APPS, RECEIVER_EXPORTED, RECEIVER_NOT_EXPORTED,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RegisterReceiverFlags {}
    /**
     * Flag for {@link #registerReceiver}: The receiver can receive broadcasts from Instant Apps.
     */
    public static final int RECEIVER_VISIBLE_TO_INSTANT_APPS = 0x1;

    /**
     * Flag for {@link #registerReceiver}: The receiver can receive broadcasts from other Apps.
     * Has the same behavior as marking a statically registered receiver with "exported=true"
     */
    public static final int RECEIVER_EXPORTED = 0x2;

    /**
     * Flag for {@link #registerReceiver}: The receiver cannot receive broadcasts from other Apps.
     * Has the same behavior as marking a statically registered receiver with "exported=false"
     */
    public static final int RECEIVER_NOT_EXPORTED = 0x4;

    /**
     * Start a set of activities as a synthesized task stack, if able.
     *
     * <p>In API level 11 (Android 3.0/Honeycomb) the recommended conventions for
     * app navigation using the back key changed. The back key's behavior is local
     * to the current task and does not capture navigation across different tasks.
     * Navigating across tasks and easily reaching the previous task is accomplished
     * through the "recents" UI, accessible through the software-provided Recents key
     * on the navigation or system bar. On devices with the older hardware button configuration
     * the recents UI can be accessed with a long press on the Home key.</p>
     *
     * <p>When crossing from one task stack to another post-Android 3.0,
     * the application should synthesize a back stack/history for the new task so that
     * the user may navigate out of the new task and back to the Launcher by repeated
     * presses of the back key. Back key presses should not navigate across task stacks.</p>
     *
     * <p>startActivities provides a mechanism for constructing a synthetic task stack of
     * multiple activities. If the underlying API is not available on the system this method
     * will return false.</p>
     *
     * @param context Start activities using this activity as the starting context
     * @param intents Array of intents defining the activities that will be started. The element
     *                length-1 will correspond to the top activity on the resulting task stack.
     * @return true if the underlying API was available and the call was successful, false otherwise
     */
    public static boolean startActivities(@NonNull Context context, Intent @NonNull [] intents) {
        return startActivities(context, intents, null);
    }

    /**
     * Start a set of activities as a synthesized task stack, if able.
     *
     * <p>In API level 11 (Android 3.0/Honeycomb) the recommended conventions for
     * app navigation using the back key changed. The back key's behavior is local
     * to the current task and does not capture navigation across different tasks.
     * Navigating across tasks and easily reaching the previous task is accomplished
     * through the "recents" UI, accessible through the software-provided Recents key
     * on the navigation or system bar. On devices with the older hardware button configuration
     * the recents UI can be accessed with a long press on the Home key.</p>
     *
     * <p>When crossing from one task stack to another post-Android 3.0,
     * the application should synthesize a back stack/history for the new task so that
     * the user may navigate out of the new task and back to the Launcher by repeated
     * presses of the back key. Back key presses should not navigate across task stacks.</p>
     *
     * <p>startActivities provides a mechanism for constructing a synthetic task stack of
     * multiple activities. If the underlying API is not available on the system this method
     * will return false.</p>
     *
     * @param context Start activities using this activity as the starting context
     * @param intents Array of intents defining the activities that will be started. The element
     *                length-1 will correspond to the top activity on the resulting task stack.
     * @param options Additional options for how the Activity should be started.
     *                See {@link Context#startActivity(Intent, Bundle)}
     * @return true if the underlying API was available and the call was successful, false otherwise
     */
    public static boolean startActivities(@NonNull Context context, Intent @NonNull [] intents,
            @Nullable Bundle options) {
        context.startActivities(intents, options);
        return true;
    }

    /**
     * Start an activity with additional launch information, if able.
     *
     * <p>In Android 4.1+ additional options were introduced to allow for more
     * control on activity launch animations. Applications can use this method
     * along with {@link ActivityOptionsCompat} to use these animations when
     * available. When run on versions of the platform where this feature does
     * not exist the activity will be launched normally.</p>
     *
     * @param context Context to launch activity from.
     * @param intent  The description of the activity to start.
     * @param options Additional options for how the Activity should be started.
     *                May be null if there are no options. See
     *                {@link ActivityOptionsCompat} for how to build the Bundle
     *                supplied here; there are no supported definitions for
     *                building it manually.
     * @deprecated Call {@link Context#startActivity()} directly.
     */
    @Deprecated
    @androidx.annotation.ReplaceWith(expression = "context.startActivity(intent, options)")
    public static void startActivity(@NonNull Context context, @NonNull Intent intent,
            @Nullable Bundle options) {
        context.startActivity(intent, options);
    }

    /**
     * Returns the absolute path to the directory on the filesystem where all
     * private files belonging to this app are stored. Apps should not use this
     * path directly; they should instead use {@link Context#getFilesDir()},
     * {@link Context#getCacheDir()}, {@link Context#getDir(String, int)}, or
     * other storage APIs on {@link Context}.
     * <p>
     * The returned path may change over time if the calling app is moved to an
     * adopted storage device, so only relative paths should be persisted.
     * <p>
     * No additional permissions are required for the calling app to read or
     * write files under the returned path.
     *
     * @see ApplicationInfo#dataDir
     */
    public static @Nullable File getDataDir(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 24) {
            return Api24Impl.getDataDir(context);
        } else {
            final String dataDir = context.getApplicationInfo().dataDir;
            return dataDir != null ? new File(dataDir) : null;
        }
    }

    /**
     * Returns absolute paths to application-specific directories on all
     * external storage devices where the application's OBB files (if there are
     * any) can be found. Note if the application does not have any OBB files,
     * these directories may not exist.
     * <p>
     * This is like {@link Context#getFilesDir()} in that these files will be
     * deleted when the application is uninstalled, however there are some
     * important differences:
     * <ul>
     * <li>External files are not always available: they will disappear if the
     * user mounts the external storage on a computer or removes it.
     * <li>There is no security enforced with these files.
     * </ul>
     * <p>
     * External storage devices returned here are considered a permanent part of
     * the device, including both emulated external storage and physical media
     * slots, such as SD cards in a battery compartment. The returned paths do
     * not include transient devices, such as USB flash drives.
     * <p>
     * An application may store data on any or all of the returned devices. For
     * example, an app may choose to store large files on the device with the
     * most available space, as measured by {@link StatFs}.
     * <p>
     * Starting in {@link Build.VERSION_CODES#KITKAT}, no permissions
     * are required to write to the returned paths; they're always accessible to
     * the calling app. Before then,
     * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE} is required to
     * write. Write access outside of these paths on secondary external storage
     * devices is not available. To request external storage access in a
     * backwards compatible way, consider using {@code android:maxSdkVersion}
     * like this:
     *
     * <pre class="prettyprint">&lt;uses-permission
     *     android:name="android.permission.WRITE_EXTERNAL_STORAGE"
     *     android:maxSdkVersion="18" /&gt;</pre>
     * <p>
     * The first path returned is the same as {@link Context#getObbDir()}.
     * Returned paths may be {@code null} if a storage device is unavailable.
     *
     * @see Context#getObbDir()
     * @see EnvironmentCompat#getStorageState(File)
     * @deprecated Call {@link Context#getObbDirs()} directly.
     */
    @Deprecated
    @androidx.annotation.ReplaceWith(expression = "context.getObbDirs()")
    public static File @NonNull [] getObbDirs(@NonNull Context context) {
        return context.getObbDirs();
    }

    /**
     * Returns absolute paths to application-specific directories on all
     * external storage devices where the application can place persistent files
     * it owns. These files are internal to the application, and not typically
     * visible to the user as media.
     * <p>
     * This is like {@link Context#getFilesDir()} in that these files will be
     * deleted when the application is uninstalled, however there are some
     * important differences:
     * <ul>
     * <li>External files are not always available: they will disappear if the
     * user mounts the external storage on a computer or removes it.
     * <li>There is no security enforced with these files.
     * </ul>
     * <p>
     * External storage devices returned here are considered a permanent part of
     * the device, including both emulated external storage and physical media
     * slots, such as SD cards in a battery compartment. The returned paths do
     * not include transient devices, such as USB flash drives.
     * <p>
     * An application may store data on any or all of the returned devices. For
     * example, an app may choose to store large files on the device with the
     * most available space, as measured by {@link StatFs}.
     * <p>
     * Starting in {@link Build.VERSION_CODES#KITKAT}, no permissions
     * are required to write to the returned paths; they're always accessible to
     * the calling app. Before then,
     * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE} is required to
     * write. Write access outside of these paths on secondary external storage
     * devices is not available. To request external storage access in a
     * backwards compatible way, consider using {@code android:maxSdkVersion}
     * like this:
     *
     * <pre class="prettyprint">&lt;uses-permission
     *     android:name="android.permission.WRITE_EXTERNAL_STORAGE"
     *     android:maxSdkVersion="18" /&gt;</pre>
     * <p>
     * The first path returned is the same as
     * {@link Context#getExternalFilesDir(String)}. Returned paths may be
     * {@code null} if a storage device is unavailable.
     *
     * @see Context#getExternalFilesDir(String)
     * @see EnvironmentCompat#getStorageState(File)
     * @deprecated Call {@link Context#getExternalFilesDirs()} directly.
     */
    @Deprecated
    @androidx.annotation.ReplaceWith(expression = "context.getExternalFilesDirs(type)")
    public static File @NonNull [] getExternalFilesDirs(@NonNull Context context,
            @Nullable String type) {
        return context.getExternalFilesDirs(type);
    }

    /**
     * Returns absolute paths to application-specific directories on all
     * external storage devices where the application can place cache files it
     * owns. These files are internal to the application, and not typically
     * visible to the user as media.
     * <p>
     * This is like {@link Context#getCacheDir()} in that these files will be
     * deleted when the application is uninstalled, however there are some
     * important differences:
     * <ul>
     * <li>External files are not always available: they will disappear if the
     * user mounts the external storage on a computer or removes it.
     * <li>There is no security enforced with these files.
     * </ul>
     * <p>
     * External storage devices returned here are considered a permanent part of
     * the device, including both emulated external storage and physical media
     * slots, such as SD cards in a battery compartment. The returned paths do
     * not include transient devices, such as USB flash drives.
     * <p>
     * An application may store data on any or all of the returned devices. For
     * example, an app may choose to store large files on the device with the
     * most available space, as measured by {@link StatFs}.
     * <p>
     * Starting in {@link Build.VERSION_CODES#KITKAT}, no permissions
     * are required to write to the returned paths; they're always accessible to
     * the calling app. Before then,
     * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE} is required to
     * write. Write access outside of these paths on secondary external storage
     * devices is not available. To request external storage access in a
     * backwards compatible way, consider using {@code android:maxSdkVersion}
     * like this:
     *
     * <pre class="prettyprint">&lt;uses-permission
     *     android:name="android.permission.WRITE_EXTERNAL_STORAGE"
     *     android:maxSdkVersion="18" /&gt;</pre>
     * <p>
     * The first path returned is the same as
     * {@link Context#getExternalCacheDir()}. Returned paths may be {@code null}
     * if a storage device is unavailable.
     *
     * @see Context#getExternalCacheDir()
     * @see EnvironmentCompat#getStorageState(File)
     * @deprecated Call {@link Context#getExternalCacheDirs()} directly.
     */
    @Deprecated
    @androidx.annotation.ReplaceWith(expression = "context.getExternalCacheDirs()")
    public static File @NonNull [] getExternalCacheDirs(@NonNull Context context) {
        return context.getExternalCacheDirs();
    }

    /**
     * Returns a drawable object associated with a particular resource ID.
     * <p>
     * Starting in {@link Build.VERSION_CODES#LOLLIPOP}, the
     * returned drawable will be styled for the specified Context's theme.
     *
     * @param context context to use for getting the drawable.
     * @param id The desired resource identifier, as generated by the aapt tool.
     *           This integer encodes the package, type, and resource entry.
     *           The value 0 is an invalid identifier.
     * @return Drawable An object that can be used to draw this resource.
     */
    @SuppressWarnings("deprecation")
    public static @Nullable Drawable getDrawable(@NonNull Context context, @DrawableRes int id) {
        return context.getDrawable(id);
    }

    /**
     * Returns a color state list associated with a particular resource ID.
     * <p>
     * Starting in {@link Build.VERSION_CODES#M}, the returned
     * color state list will be styled for the specified Context's theme.
     *
     * @param context context to use for getting the color state list.
     * @param id The desired resource identifier, as generated by the aapt
     *           tool. This integer encodes the package, type, and resource
     *           entry. The value 0 is an invalid identifier.
     * @return A color state list, or {@code null} if the resource could not be
     * resolved.
     * @throws android.content.res.Resources.NotFoundException if the given ID
     *         does not exist.
     */
    public static @Nullable ColorStateList getColorStateList(@NonNull Context context,
            @ColorRes int id) {
        return ResourcesCompat.getColorStateList(context.getResources(), id, context.getTheme());
    }

    /**
     * Returns a color associated with a particular resource ID
     * <p>
     * Starting in {@link Build.VERSION_CODES#M}, the returned
     * color will be styled for the specified Context's theme.
     *
     * @param context context to use for getting the color.
     * @param id The desired resource identifier, as generated by the aapt
     *           tool. This integer encodes the package, type, and resource
     *           entry. The value 0 is an invalid identifier.
     * @return A single color value in the form 0xAARRGGBB.
     * @throws android.content.res.Resources.NotFoundException if the given ID
     *         does not exist.
     */
    @ColorInt
    public static int getColor(@NonNull Context context, @ColorRes int id) {
        return context.getColor(id);
    }

    /**
     * Determine whether <em>you</em> have been granted a particular permission.
     *
     * @param context context for which to check the permission.
     * @param permission The name of the permission being checked.
     * @return {@link PackageManager#PERMISSION_GRANTED} if you have the
     * permission, or {@link PackageManager#PERMISSION_DENIED} if not.
     * @see PackageManager#checkPermission(String, String)
     */
    public static int checkSelfPermission(@NonNull Context context, @NonNull String permission) {
        ObjectsCompat.requireNonNull(permission, "permission must be non-null");
        if (Build.VERSION.SDK_INT < 33
                && TextUtils.equals(android.Manifest.permission.POST_NOTIFICATIONS, permission)) {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
                    ? PackageManager.PERMISSION_GRANTED
                    : PackageManager.PERMISSION_DENIED;
        }
        return context.checkPermission(permission, Process.myPid(), Process.myUid());
    }

    /**
     * Returns the absolute path to the directory on the filesystem similar to
     * {@link Context#getFilesDir()}.  The difference is that files placed under this
     * directory will be excluded from automatic backup to remote storage on
     * devices running {@link Build.VERSION_CODES#LOLLIPOP} or later.
     *
     * <p>No permissions are required to read or write to the returned path, since this
     * path is internal storage.
     *
     * @return The path of the directory holding application files that will not be
     * automatically backed up to remote storage.
     * @see Context#getFilesDir()
     */
    public static @Nullable File getNoBackupFilesDir(@NonNull Context context) {
        return context.getNoBackupFilesDir();
    }

    /**
     * Returns the absolute path to the application specific cache directory on
     * the filesystem designed for storing cached code. On devices running
     * {@link Build.VERSION_CODES#LOLLIPOP} or later, the system will delete
     * any files stored in this location both when your specific application is
     * upgraded, and when the entire platform is upgraded.
     * <p>
     * This location is optimal for storing compiled or optimized code generated
     * by your application at runtime.
     * <p>
     * Apps require no extra permissions to read or write to the returned path,
     * since this path lives in their private storage.
     *
     * @return The path of the directory holding application code cache files.
     */
    public static @NonNull File getCodeCacheDir(@NonNull Context context) {
        return context.getCodeCacheDir();
    }

    /**
     * Return a new Context object for the current Context but whose storage
     * APIs are backed by device-protected storage.
     * <p>
     * On devices with direct boot, data stored in this location is encrypted
     * with a key tied to the physical device, and it can be accessed
     * immediately after the device has booted successfully, both
     * <em>before and after</em> the user has authenticated with their
     * credentials (such as a lock pattern or PIN).
     * <p>
     * Because device-protected data is available without user authentication,
     * you should carefully limit the data you store using this Context. For
     * example, storing sensitive authentication tokens or passwords in the
     * device-protected area is strongly discouraged.
     * <p>
     * If the underlying device does not have the ability to store
     * device-protected and credential-protected data using different keys, then
     * both storage areas will become available at the same time. They remain as
     * two distinct storage locations on disk, and only the window of
     * availability changes.
     * <p>
     * Each call to this method returns a new instance of a Context object;
     * Context objects are not shared, however common state (ClassLoader, other
     * Resources for the same configuration) may be so the Context itself can be
     * fairly lightweight.
     * <p>
     * Prior to API 24 this method returns
     * {@code null}, since device-protected storage is not available.
     *
     * @see ContextCompat#isDeviceProtectedStorage(Context)
     */
    public static @Nullable Context createDeviceProtectedStorageContext(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 24) {
            return Api24Impl.createDeviceProtectedStorageContext(context);
        } else {
            return null;
        }
    }

    /**
     * Indicates if the storage APIs of this Context are backed by
     * device-encrypted storage.
     *
     * @see ContextCompat#createDeviceProtectedStorageContext(Context)
     */
    public static boolean isDeviceProtectedStorage(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 24) {
            return Api24Impl.isDeviceProtectedStorage(context);
        } else {
            return false;
        }
    }

    /**
     * Return an {@link Executor} that will run enqueued tasks on the main
     * thread associated with this context. This is the thread used to dispatch
     * calls to application components (activities, services, etc).
     */
    public static @NonNull Executor getMainExecutor(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 28) {
            return Api28Impl.getMainExecutor(context);
        }
        return ExecutorCompat.create(new Handler(context.getMainLooper()));
    }

    /**
     * startForegroundService() was introduced in O, just call startService
     * for before O.
     *
     * @param context Context to start Service from.
     * @param intent  The description of the Service to start.
     * @see Context#startForegroundService(Intent)
     * @see Context#startService(Intent)
     */
    public static void startForegroundService(@NonNull Context context, @NonNull Intent intent) {
        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.startForegroundService(context, intent);
        } else {
            // Pre-O behavior.
            context.startService(intent);
        }
    }

    /**
     * Get the display this context is associated with or the
     * {@link Display#DEFAULT_DISPLAY default display} as the fallback if the context is not
     * associated with any {@link Display}.
     * <p>
     * Applications must use this method with {@link Activity} or a context associated with a
     * {@link Display} via {@link Context#createDisplayContext(Display)} or
     * {@link Context#createWindowContext(Display, int, Bundle)}, or the reported {@link Display}
     * instance is not reliable. </p>
     *
     * @param context Context to obtain the associated display
     * @return The display associated with the Context or the default display if the context
     * doesn't associated with any display.
     */
    public static @NonNull Display getDisplayOrDefault(@DisplayContext @NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 30) {
            return Api30Impl.getDisplayOrDefault(context);
        } else {
            final WindowManager windowManager =
                    (WindowManager) context.getSystemService(WINDOW_SERVICE);
            return windowManager.getDefaultDisplay();
        }
    }

    /**
     * Return the handle to a system-level service by class.
     *
     * @param context      Context to retrieve service from.
     * @param serviceClass The class of the desired service.
     * @return The service or null if the class is not a supported system service.
     * @see Context#getSystemService(Class)
     */
    public static <T> @Nullable T getSystemService(@NonNull Context context,
            @NonNull Class<T> serviceClass) {
        return context.getSystemService(serviceClass);
    }

    /**
     * Register a broadcast receiver.
     *
     * @param context  Context to retrieve service from.
     * @param receiver The BroadcastReceiver to handle the broadcast.
     * @param filter   Selects the Intent broadcasts to be received.
     * @param flags    If this receiver is listening for broadcasts sent from other apps—even other
     *                 apps that you own—use the {@link #RECEIVER_EXPORTED} flag. If instead this 
     *                 receiver is listening only for broadcasts sent by your
     *                 app, or from the system UID, use the {@link #RECEIVER_NOT_EXPORTED} flag.
     * @return The first sticky intent found that matches <var>filter</var>,
     * or null if there are none.
     * @see Context#registerReceiver(BroadcastReceiver, IntentFilter, int)
     * @see https://developer.android.com/develop/background-work/background-tasks/broadcasts#context-registered-receivers
     */
    public static @Nullable Intent registerReceiver(@NonNull Context context,
            @Nullable BroadcastReceiver receiver, @NonNull IntentFilter filter,
            @RegisterReceiverFlags int flags) {
        return registerReceiver(context, receiver, filter, null, null, flags);
    }

    /**
     * Register a broadcast receiver.
     *
     * @param context             Context to retrieve service from.
     * @param receiver            The BroadcastReceiver to handle the broadcast.
     * @param filter              Selects the Intent broadcasts to be received.
     * @param broadcastPermission String naming a permission that a broadcaster must hold in
     *                            order to send and Intent to you. If null, no permission is
     *                            required.
     * @param scheduler           Handler identifying the thread will receive the Intent. If
     *                            null, the main thread of the process will be used.
     * @param flags               If this receiver is listening for broadcasts sent from other
     *                            apps—even other apps that you own—use the
     *                            {@link #RECEIVER_EXPORTED} flag. If instead this receiver is
     *                            listening only for broadcasts sent by your app, or from the
     *                            system UID, use the {@link #RECEIVER_NOT_EXPORTED} flag.
     * @return The first sticky intent found that matches <var>filter</var>,
     * or null if there are none.
     * @see Context#registerReceiver(BroadcastReceiver, IntentFilter, String, Handler, int)
     * @see https://developer.android.com/develop/background-work/background-tasks/broadcasts#context-registered-receivers
     */
    public static @Nullable Intent registerReceiver(@NonNull Context context,
            @Nullable BroadcastReceiver receiver, @NonNull IntentFilter filter,
            @Nullable String broadcastPermission,
            @Nullable Handler scheduler, @RegisterReceiverFlags int flags) {
        if (((flags & RECEIVER_VISIBLE_TO_INSTANT_APPS) != 0) && ((flags & RECEIVER_NOT_EXPORTED)
                != 0)) {
            throw new IllegalArgumentException("Cannot specify both "
                    + "RECEIVER_VISIBLE_TO_INSTANT_APPS and RECEIVER_NOT_EXPORTED");
        }

        if ((flags & RECEIVER_VISIBLE_TO_INSTANT_APPS) != 0) {
            flags |= RECEIVER_EXPORTED;
        }

        if (((flags & RECEIVER_EXPORTED) == 0) && ((flags & RECEIVER_NOT_EXPORTED) == 0)) {
            throw new IllegalArgumentException("One of either RECEIVER_EXPORTED or "
                    + "RECEIVER_NOT_EXPORTED is required");
        }

        if (((flags & RECEIVER_EXPORTED) != 0) && ((flags & RECEIVER_NOT_EXPORTED) != 0)) {
            throw new IllegalArgumentException("Cannot specify both RECEIVER_EXPORTED and "
                    + "RECEIVER_NOT_EXPORTED");
        }

        if (Build.VERSION.SDK_INT >= 33) {
            return Api33Impl.registerReceiver(context, receiver, filter, broadcastPermission,
                    scheduler, flags);
        }
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.registerReceiver(context, receiver, filter, broadcastPermission,
                    scheduler, flags);
        }
        if (((flags & RECEIVER_NOT_EXPORTED) != 0) && (broadcastPermission == null)) {
            String permission = obtainAndCheckReceiverPermission(context);
            return context.registerReceiver(receiver, filter, permission, scheduler /* handler */);
        }
        return context.registerReceiver(receiver, filter, broadcastPermission,
                scheduler);
    }

    /**
     * Gets the name of the system-level service that is represented by the specified class.
     *
     * @param context      Context to retrieve service name from.
     * @param serviceClass The class of the desired service.
     * @return The service name or null if the class is not a supported system service.
     * @see Context#getSystemServiceName(Class)
     */
    public static @Nullable String getSystemServiceName(@NonNull Context context,
            @NonNull Class<?> serviceClass) {
        return context.getSystemServiceName(serviceClass);
    }

    /**
     * Gets the resource string that also respects the per-app locales. If developers set the
     * per-app locales via
     * {@link androidx.appcompat.app.AppCompatDelegate#setApplicationLocales(LocaleListCompat)},
     * this API returns localized strings even if the context is not
     * {@link androidx.appcompat.app.AppCompatActivity}.
     *
     * <p>
     * Compatibility behavior:
     * <ul>
     *     <li>API 17 and above, this method return the localized string that respects per-app
     *     locales.</li>
     *     <li>API 16 and earlier, this method directly return the result of
     *     {@link Context#getString(int)}</li>
     * </ul>
     * </p>
     */
    public static @NonNull String getString(@NonNull Context context, int resId) {
        return getContextForLanguage(context).getString(resId);
    }

    /**
     * Gets the context which respects the per-app locales locale. This API is specifically for
     * developers who set the per-app locales via
     * {@link androidx.appcompat.app.AppCompatDelegate#setApplicationLocales(LocaleListCompat)},
     * but who needs to use the context out of {@link androidx.appcompat.app.AppCompatActivity}
     * scope.
     *
     * <p>The developers can override the returned context in Application's
     * {@link android.content.ContextWrapper#attachBaseContext(Context)}, so that developers can
     * get the localized string via application's context.</p>
     *
     * <p>
     * Compatibility behavior:
     * <ul>
     *     <li>API 17 and above, the locale in the context returned by this method will respect the
     *     the per-app locale.</li>
     *     <li>API 16 and earlier, this method directly return the {@link Context}</li>
     * </ul>
     * </p>
     */
    public static @NonNull Context getContextForLanguage(@NonNull Context context) {
        LocaleListCompat locales = LocaleManagerCompat.getApplicationLocales(context);

        // The Android framework supports per-app locales on API 33, so we assume the
        // configuration has been updated after API 32.
        if (Build.VERSION.SDK_INT <= 32) {
            if (!locales.isEmpty()) {
                Configuration newConfig = new Configuration(
                        context.getResources().getConfiguration());
                ConfigurationCompat.setLocales(newConfig, locales);
                return context.createConfigurationContext(newConfig);
            }
        }
        return context;
    }

    /**
     * Attribution can be used in complex apps to logically separate parts of the app. E.g. a
     * blogging app might also have a instant messaging app built in. In this case two separate tags
     * can for used each sub-feature.
     * <p>
     * Compatibility behavior:
     * <ul>
     *     <li>API 30 and above, returns the attribution tag or {@code null}
     *     <li>API 29 and earlier, returns {@code null}
     * </ul>
     *
     * @return the attribution tag this context is for or {@code null} if this is the default.
     */
    public static @Nullable String getAttributionTag(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 30) {
            return Api30Impl.getAttributionTag(context);
        }

        return null;
    }

    /**
     * Return a new Context object for the current Context but attribute to a different tag.
     * In complex apps attribution tagging can be used to distinguish between separate logical
     * parts.
     * <p>
     * Compatibility behavior:
     * <ul>
     *     <li>API 30 and above, returns a new Context object with the specified attribution tag
     *     <li>API 29 and earlier, returns the original {@code context} with no attribution tag
     * </ul>
     *
     * @param context The current context.
     * @param attributionTag The tag or {@code null} to create a context for the default.
     * @return A {@link Context} that is tagged for the new attribution
     * @see #getAttributionTag(Context)
     */
    public static @NonNull Context createAttributionContext(@NonNull Context context,
            @Nullable String attributionTag) {
        if (Build.VERSION.SDK_INT >= 30) {
            return Api30Impl.createAttributionContext(context, attributionTag);
        }

        return context;
    }

    /**
     * Gets the name of the permission required to unexport receivers on pre Tiramisu versions of
     * Android, and then asserts that the app registering the receiver also has that permission
     * so it can receiver its own broadcasts.
     *
     * @param obj Context to check the permission in.
     * @return The name of the permission
     */
    static String obtainAndCheckReceiverPermission(Context obj) {
        String permission = obj.getApplicationContext().getPackageName()
                + DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION_SUFFIX;

        if (PermissionChecker.checkSelfPermission(obj, permission)
                != PermissionChecker.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= 29) {
                permission =
                        obj.getOpPackageName() + DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION_SUFFIX;
                if (PermissionChecker.checkSelfPermission(obj, permission)
                        == PermissionChecker.PERMISSION_GRANTED) {
                    return permission;
                }
            }
            throw new RuntimeException("Permission " + permission + " is required by your "
                    + "application to receive broadcasts, please add it to your manifest");
        }
        return permission;
    }

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
            // This class is not instantiable.
        }

        static File getDataDir(Context obj) {
            return obj.getDataDir();
        }

        static Context createDeviceProtectedStorageContext(Context obj) {
            return obj.createDeviceProtectedStorageContext();
        }

        static boolean isDeviceProtectedStorage(Context obj) {
            return obj.isDeviceProtectedStorage();
        }
    }

    @RequiresApi(26)
    static class Api26Impl {
        private Api26Impl() {
            // This class is not instantiable.
        }

        static Intent registerReceiver(Context obj, @Nullable BroadcastReceiver receiver,
                IntentFilter filter, String broadcastPermission, Handler scheduler, int flags) {
            if ((flags & RECEIVER_NOT_EXPORTED) != 0 && broadcastPermission == null) {
                String permission = obtainAndCheckReceiverPermission(obj);
                // receivers that are not exported should also not be visible to instant apps
                return obj.registerReceiver(receiver, filter, permission, scheduler);
            }
            flags &= Context.RECEIVER_VISIBLE_TO_INSTANT_APPS;
            return obj.registerReceiver(receiver, filter, broadcastPermission, scheduler, flags);
        }

        @SuppressWarnings("UnusedReturnValue")
        static ComponentName startForegroundService(Context obj, Intent service) {
            return obj.startForegroundService(service);
        }
    }

    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() {
            // This class is not instantiable.
        }

        static Executor getMainExecutor(Context obj) {
            return obj.getMainExecutor();
        }
    }

    @RequiresApi(30)
    static class Api30Impl {
        private Api30Impl() {
            // This class is not instantiable.
        }

        static String getAttributionTag(Context obj) {
            return obj.getAttributionTag();
        }

        static Display getDisplayOrDefault(Context obj) {
            try {
                return obj.getDisplay();
            } catch (UnsupportedOperationException e) {
                // Provide a fallback display if the context is not associated with any display.
                Log.w(TAG, "The context:" + obj + " is not associated with any display. Return a "
                        + "fallback display instead.");
                return obj.getSystemService(DisplayManager.class)
                        .getDisplay(Display.DEFAULT_DISPLAY);
            }
        }

        static @NonNull Context createAttributionContext(@NonNull Context context,
                @Nullable String attributionTag) {
            return context.createAttributionContext(attributionTag);
        }
    }

    @RequiresApi(33)
    static class Api33Impl {
        private Api33Impl() {
            // This class is not instantiable
        }

        static Intent registerReceiver(Context obj, @Nullable BroadcastReceiver receiver,
                IntentFilter filter, String broadcastPermission, Handler scheduler, int flags) {
            return obj.registerReceiver(receiver, filter, broadcastPermission, scheduler, flags);
        }
    }
}
