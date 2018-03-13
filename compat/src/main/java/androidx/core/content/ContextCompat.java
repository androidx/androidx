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

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static android.content.Context.ACCOUNT_SERVICE;
import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.APPWIDGET_SERVICE;
import static android.content.Context.APP_OPS_SERVICE;
import static android.content.Context.AUDIO_SERVICE;
import static android.content.Context.BATTERY_SERVICE;
import static android.content.Context.BLUETOOTH_SERVICE;
import static android.content.Context.CAMERA_SERVICE;
import static android.content.Context.CAPTIONING_SERVICE;
import static android.content.Context.CLIPBOARD_SERVICE;
import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.CONSUMER_IR_SERVICE;
import static android.content.Context.DEVICE_POLICY_SERVICE;
import static android.content.Context.DISPLAY_SERVICE;
import static android.content.Context.DOWNLOAD_SERVICE;
import static android.content.Context.DROPBOX_SERVICE;
import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.content.Context.INPUT_SERVICE;
import static android.content.Context.JOB_SCHEDULER_SERVICE;
import static android.content.Context.KEYGUARD_SERVICE;
import static android.content.Context.LAUNCHER_APPS_SERVICE;
import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static android.content.Context.LOCATION_SERVICE;
import static android.content.Context.MEDIA_PROJECTION_SERVICE;
import static android.content.Context.MEDIA_ROUTER_SERVICE;
import static android.content.Context.MEDIA_SESSION_SERVICE;
import static android.content.Context.NFC_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.NSD_SERVICE;
import static android.content.Context.POWER_SERVICE;
import static android.content.Context.PRINT_SERVICE;
import static android.content.Context.RESTRICTIONS_SERVICE;
import static android.content.Context.SEARCH_SERVICE;
import static android.content.Context.SENSOR_SERVICE;
import static android.content.Context.STORAGE_SERVICE;
import static android.content.Context.TELECOM_SERVICE;
import static android.content.Context.TELEPHONY_SERVICE;
import static android.content.Context.TELEPHONY_SUBSCRIPTION_SERVICE;
import static android.content.Context.TEXT_SERVICES_MANAGER_SERVICE;
import static android.content.Context.TV_INPUT_SERVICE;
import static android.content.Context.UI_MODE_SERVICE;
import static android.content.Context.USAGE_STATS_SERVICE;
import static android.content.Context.USB_SERVICE;
import static android.content.Context.USER_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;
import static android.content.Context.WALLPAPER_SERVICE;
import static android.content.Context.WIFI_P2P_SERVICE;
import static android.content.Context.WIFI_SERVICE;
import static android.content.Context.WINDOW_SERVICE;

import android.accessibilityservice.AccessibilityService;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.DownloadManager;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.app.UiModeManager;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.app.job.JobScheduler;
import android.app.usage.UsageStatsManager;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothManager;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.RestrictionsManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.hardware.ConsumerIrManager;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.hardware.usb.UsbManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.media.projection.MediaProjectionManager;
import android.media.session.MediaSessionManager;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.nsd.NsdManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.nfc.NfcManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.DropBoxManager;
import android.os.PowerManager;
import android.os.Process;
import android.os.UserManager;
import android.os.Vibrator;
import android.os.storage.StorageManager;
import android.print.PrintManager;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.accessibility.CaptioningManager;
import android.view.inputmethod.InputMethodManager;
import android.view.textservice.TextServicesManager;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.os.EnvironmentCompat;

import java.io.File;
import java.util.HashMap;

/**
 * Helper for accessing features in {@link android.content.Context}.
 */
public class ContextCompat {
    private static final String TAG = "ContextCompat";

    private static final Object sLock = new Object();

    private static TypedValue sTempValue;

    /**
     * This class should not be instantiated, but the constructor must be
     * visible for the class to be extended (ex. in ActivityCompat).
     */
    protected ContextCompat() {
        // Not publicly instantiable, but may be extended.
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
     * @return true if the underlying API was available and the call was successful, false otherwise
     */
    public static boolean startActivities(@NonNull Context context, @NonNull Intent[] intents) {
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
     * See {@link android.content.Context#startActivity(Intent, android.os.Bundle)}
     * @return true if the underlying API was available and the call was successful, false otherwise
     */
    public static boolean startActivities(@NonNull Context context, @NonNull Intent[] intents,
            @Nullable Bundle options) {
        if (Build.VERSION.SDK_INT >= 16) {
            context.startActivities(intents, options);
        } else {
            context.startActivities(intents);
        }
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
     * @param intent The description of the activity to start.
     * @param options Additional options for how the Activity should be started.
     *                May be null if there are no options. See
     *                {@link ActivityOptionsCompat} for how to build the Bundle
     *                supplied here; there are no supported definitions for
     *                building it manually.
     */
    public static void startActivity(@NonNull Context context, @NonNull Intent intent,
            @Nullable Bundle options) {
        if (Build.VERSION.SDK_INT >= 16) {
            context.startActivity(intent, options);
        } else {
            context.startActivity(intent);
        }
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
    @Nullable
    public static File getDataDir(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 24) {
            return context.getDataDir();
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
     * most available space, as measured by {@link android.os.StatFs}.
     * <p>
     * Starting in {@link android.os.Build.VERSION_CODES#KITKAT}, no permissions
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
     */
    @NonNull
    public static File[] getObbDirs(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 19) {
            return context.getObbDirs();
        } else {
            return new File[] { context.getObbDir() };
        }
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
     * most available space, as measured by {@link android.os.StatFs}.
     * <p>
     * Starting in {@link android.os.Build.VERSION_CODES#KITKAT}, no permissions
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
     */
    @NonNull
    public static File[] getExternalFilesDirs(@NonNull Context context, @Nullable String type) {
        if (Build.VERSION.SDK_INT >= 19) {
            return context.getExternalFilesDirs(type);
        } else {
            return new File[] { context.getExternalFilesDir(type) };
        }
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
     * most available space, as measured by {@link android.os.StatFs}.
     * <p>
     * Starting in {@link android.os.Build.VERSION_CODES#KITKAT}, no permissions
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
     */
    @NonNull
    public static File[] getExternalCacheDirs(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 19) {
            return context.getExternalCacheDirs();
        } else {
            return new File[] { context.getExternalCacheDir() };
        }
    }

    private static File buildPath(File base, String... segments) {
        File cur = base;
        for (String segment : segments) {
            if (cur == null) {
                cur = new File(segment);
            } else if (segment != null) {
                cur = new File(cur, segment);
            }
        }
        return cur;
    }

    /**
     * Returns a drawable object associated with a particular resource ID.
     * <p>
     * Starting in {@link android.os.Build.VERSION_CODES#LOLLIPOP}, the
     * returned drawable will be styled for the specified Context's theme.
     *
     * @param id The desired resource identifier, as generated by the aapt tool.
     *           This integer encodes the package, type, and resource entry.
     *           The value 0 is an invalid identifier.
     * @return Drawable An object that can be used to draw this resource.
     */
    @Nullable
    public static Drawable getDrawable(@NonNull Context context, @DrawableRes int id) {
        if (Build.VERSION.SDK_INT >= 21) {
            return context.getDrawable(id);
        } else if (Build.VERSION.SDK_INT >= 16) {
            return context.getResources().getDrawable(id);
        } else {
            // Prior to JELLY_BEAN, Resources.getDrawable() would not correctly
            // retrieve the final configuration density when the resource ID
            // is a reference another Drawable resource. As a workaround, try
            // to resolve the drawable reference manually.
            final int resolvedId;
            synchronized (sLock) {
                if (sTempValue == null) {
                    sTempValue = new TypedValue();
                }
                context.getResources().getValue(id, sTempValue, true);
                resolvedId = sTempValue.resourceId;
            }
            return context.getResources().getDrawable(resolvedId);
        }
    }

    /**
     * Returns a color state list associated with a particular resource ID.
     * <p>
     * Starting in {@link android.os.Build.VERSION_CODES#M}, the returned
     * color state list will be styled for the specified Context's theme.
     *
     * @param id The desired resource identifier, as generated by the aapt
     *           tool. This integer encodes the package, type, and resource
     *           entry. The value 0 is an invalid identifier.
     * @return A color state list, or {@code null} if the resource could not be
     *         resolved.
     * @throws android.content.res.Resources.NotFoundException if the given ID
     *         does not exist.
     */
    @Nullable
    public static ColorStateList getColorStateList(@NonNull Context context,
            @ColorRes int id) {
        if (Build.VERSION.SDK_INT >= 23) {
            return context.getColorStateList(id);
        } else {
            return context.getResources().getColorStateList(id);
        }
    }

    /**
     * Returns a color associated with a particular resource ID
     * <p>
     * Starting in {@link android.os.Build.VERSION_CODES#M}, the returned
     * color will be styled for the specified Context's theme.
     *
     * @param id The desired resource identifier, as generated by the aapt
     *           tool. This integer encodes the package, type, and resource
     *           entry. The value 0 is an invalid identifier.
     * @return A single color value in the form 0xAARRGGBB.
     * @throws android.content.res.Resources.NotFoundException if the given ID
     *         does not exist.
     */
    @ColorInt
    public static int getColor(@NonNull Context context, @ColorRes int id) {
        if (Build.VERSION.SDK_INT >= 23) {
            return context.getColor(id);
        } else {
            return context.getResources().getColor(id);
        }
    }

    /**
     * Determine whether <em>you</em> have been granted a particular permission.
     *
     * @param permission The name of the permission being checked.
     *
     * @return {@link android.content.pm.PackageManager#PERMISSION_GRANTED} if you have the
     * permission, or {@link android.content.pm.PackageManager#PERMISSION_DENIED} if not.
     *
     * @see android.content.pm.PackageManager#checkPermission(String, String)
     */
    public static int checkSelfPermission(@NonNull Context context, @NonNull String permission) {
        if (permission == null) {
            throw new IllegalArgumentException("permission is null");
        }

        return context.checkPermission(permission, android.os.Process.myPid(), Process.myUid());
    }

    /**
     * Returns the absolute path to the directory on the filesystem similar to
     * {@link Context#getFilesDir()}.  The difference is that files placed under this
     * directory will be excluded from automatic backup to remote storage on
     * devices running {@link android.os.Build.VERSION_CODES#LOLLIPOP} or later.
     *
     * <p>No permissions are required to read or write to the returned path, since this
     * path is internal storage.
     *
     * @return The path of the directory holding application files that will not be
     *         automatically backed up to remote storage.
     *
     * @see android.content.Context#getFilesDir()
     */
    @Nullable
    public static File getNoBackupFilesDir(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 21) {
            return context.getNoBackupFilesDir();
        } else {
            ApplicationInfo appInfo = context.getApplicationInfo();
            return createFilesDir(new File(appInfo.dataDir, "no_backup"));
        }
    }

    /**
     * Returns the absolute path to the application specific cache directory on
     * the filesystem designed for storing cached code. On devices running
     * {@link android.os.Build.VERSION_CODES#LOLLIPOP} or later, the system will delete
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
    public static File getCodeCacheDir(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 21) {
            return context.getCodeCacheDir();
        } else {
            ApplicationInfo appInfo = context.getApplicationInfo();
            return createFilesDir(new File(appInfo.dataDir, "code_cache"));
        }
    }

    private synchronized static File createFilesDir(File file) {
        if (!file.exists()) {
            if (!file.mkdirs()) {
                if (file.exists()) {
                    // spurious failure; probably racing with another process for this app
                    return file;
                }
                Log.w(TAG, "Unable to create files subdir " + file.getPath());
                return null;
            }
        }
        return file;
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
    @Nullable
    public static Context createDeviceProtectedStorageContext(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 24) {
            return context.createDeviceProtectedStorageContext();
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
            return context.isDeviceProtectedStorage();
        } else {
            return false;
        }
    }

    /**
     * startForegroundService() was introduced in O, just call startService
     * for before O.
     *
     * @param context Context to start Service from.
     * @param intent The description of the Service to start.
     *
     * @see Context#startForegeroundService()
     * @see Context#startService()
     */
    public static void startForegroundService(@NonNull Context context, @NonNull Intent intent) {
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(intent);
        } else {
            // Pre-O behavior.
            context.startService(intent);
        }
    }

    /**
     * Return the handle to a system-level service by class.
     *
     * @param context Context to retrieve service from.
     * @param serviceClass The class of the desired service.
     * @return The service or null if the class is not a supported system service.
     *
     * @see Context#getSystemService(Class)
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T getSystemService(@NonNull Context context, @NonNull Class<T> serviceClass) {
        if (Build.VERSION.SDK_INT >= 23) {
            return context.getSystemService(serviceClass);
        }

        String serviceName = getSystemServiceName(context, serviceClass);
        return serviceName != null ? (T) context.getSystemService(serviceName) : null;
    }

    /**
     * Gets the name of the system-level service that is represented by the specified class.
     *
     * @param context Context to retrieve service name from.
     * @param serviceClass The class of the desired service.
     * @return The service name or null if the class is not a supported system service.
     *
     * @see Context#getSystemServiceName(Class)
     */
    @Nullable
    public static String getSystemServiceName(@NonNull Context context,
            @NonNull Class<?> serviceClass) {
        if (Build.VERSION.SDK_INT >= 23) {
            return context.getSystemServiceName(serviceClass);
        }
        return LegacyServiceMapHolder.SERVICES.get(serviceClass);
    }

    /** Nested class provides lazy initialization only when needed. */
    private static final class LegacyServiceMapHolder {
        static final HashMap<Class<?>, String> SERVICES = new HashMap<>();

        static {
            if (Build.VERSION.SDK_INT > 22) {
                SERVICES.put(SubscriptionManager.class, TELEPHONY_SUBSCRIPTION_SERVICE);
                SERVICES.put(UsageStatsManager.class, USAGE_STATS_SERVICE);
            }
            if (Build.VERSION.SDK_INT > 21) {
                SERVICES.put(AppWidgetManager.class, APPWIDGET_SERVICE);
                SERVICES.put(BatteryManager.class, BATTERY_SERVICE);
                SERVICES.put(CameraManager.class, CAMERA_SERVICE);
                SERVICES.put(JobScheduler.class, JOB_SCHEDULER_SERVICE);
                SERVICES.put(LauncherApps.class, LAUNCHER_APPS_SERVICE);
                SERVICES.put(MediaProjectionManager.class, MEDIA_PROJECTION_SERVICE);
                SERVICES.put(MediaSessionManager.class, MEDIA_SESSION_SERVICE);
                SERVICES.put(RestrictionsManager.class, RESTRICTIONS_SERVICE);
                SERVICES.put(TelecomManager.class, TELECOM_SERVICE);
                SERVICES.put(TvInputManager.class, TV_INPUT_SERVICE);
            }
            if (Build.VERSION.SDK_INT > 19) {
                SERVICES.put(AppOpsManager.class, APP_OPS_SERVICE);
                SERVICES.put(CaptioningManager.class, CAPTIONING_SERVICE);
                SERVICES.put(ConsumerIrManager.class, CONSUMER_IR_SERVICE);
                SERVICES.put(PrintManager.class, PRINT_SERVICE);
            }
            if (Build.VERSION.SDK_INT > 18) {
                SERVICES.put(BluetoothManager.class, BLUETOOTH_SERVICE);
            }
            if (Build.VERSION.SDK_INT > 17) {
                SERVICES.put(DisplayManager.class, DISPLAY_SERVICE);
                SERVICES.put(UserManager.class, USER_SERVICE);
            }
            if (Build.VERSION.SDK_INT > 16) {
                SERVICES.put(InputManager.class, INPUT_SERVICE);
                SERVICES.put(MediaRouter.class, MEDIA_ROUTER_SERVICE);
                SERVICES.put(NsdManager.class, NSD_SERVICE);
            }
            SERVICES.put(AccessibilityService.class, ACCESSIBILITY_SERVICE);
            SERVICES.put(AccountManager.class, ACCOUNT_SERVICE);
            SERVICES.put(ActivityManager.class, ACTIVITY_SERVICE);
            SERVICES.put(AlarmManager.class, ALARM_SERVICE);
            SERVICES.put(AudioManager.class, AUDIO_SERVICE);
            SERVICES.put(ClipboardManager.class, CLIPBOARD_SERVICE);
            SERVICES.put(ConnectivityManager.class, CONNECTIVITY_SERVICE);
            SERVICES.put(DevicePolicyManager.class, DEVICE_POLICY_SERVICE);
            SERVICES.put(DownloadManager.class, DOWNLOAD_SERVICE);
            SERVICES.put(DropBoxManager.class, DROPBOX_SERVICE);
            SERVICES.put(InputMethodManager.class, INPUT_METHOD_SERVICE);
            SERVICES.put(KeyguardManager.class, KEYGUARD_SERVICE);
            SERVICES.put(LayoutInflater.class, LAYOUT_INFLATER_SERVICE);
            SERVICES.put(LocationManager.class, LOCATION_SERVICE);
            SERVICES.put(NfcManager.class, NFC_SERVICE);
            SERVICES.put(NotificationManager.class, NOTIFICATION_SERVICE);
            SERVICES.put(PowerManager.class, POWER_SERVICE);
            SERVICES.put(SearchManager.class, SEARCH_SERVICE);
            SERVICES.put(SensorManager.class, SENSOR_SERVICE);
            SERVICES.put(StorageManager.class, STORAGE_SERVICE);
            SERVICES.put(TelephonyManager.class, TELEPHONY_SERVICE);
            SERVICES.put(TextServicesManager.class, TEXT_SERVICES_MANAGER_SERVICE);
            SERVICES.put(UiModeManager.class, UI_MODE_SERVICE);
            SERVICES.put(UsbManager.class, USB_SERVICE);
            SERVICES.put(Vibrator.class, VIBRATOR_SERVICE);
            SERVICES.put(WallpaperManager.class, WALLPAPER_SERVICE);
            SERVICES.put(WifiP2pManager.class, WIFI_P2P_SERVICE);
            SERVICES.put(WifiManager.class, WIFI_SERVICE);
            SERVICES.put(WindowManager.class, WINDOW_SERVICE);
        }
    }
}
