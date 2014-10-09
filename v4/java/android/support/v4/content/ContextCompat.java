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

package android.support.v4.content;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.os.EnvironmentCompat;
import android.util.Log;

import java.io.File;

/**
 * Helper for accessing features in {@link android.content.Context}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public class ContextCompat {
    private static final String TAG = "ContextCompat";

    private static final String DIR_ANDROID = "Android";
    private static final String DIR_DATA = "data";
    private static final String DIR_OBB = "obb";
    private static final String DIR_FILES = "files";
    private static final String DIR_CACHE = "cache";

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
    public static boolean startActivities(Context context, Intent[] intents) {
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
     * See {@link android.content.Context#startActivity(Intent, Bundle)
     * @return true if the underlying API was available and the call was successful, false otherwise
     */
    public static boolean startActivities(Context context, Intent[] intents,
            Bundle options) {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 16) {
            ContextCompatJellybean.startActivities(context, intents, options);
            return true;
        } else if (version >= 11) {
            ContextCompatHoneycomb.startActivities(context, intents);
            return true;
        }
        return false;
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
    public static File[] getObbDirs(Context context) {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 19) {
            return ContextCompatKitKat.getObbDirs(context);
        } else {
            final File single;
            if (version >= 11) {
                single = ContextCompatHoneycomb.getObbDir(context);
            } else {
                single = buildPath(Environment.getExternalStorageDirectory(), DIR_ANDROID, DIR_OBB,
                        context.getPackageName());
            }
            return new File[] { single };
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
     * most available space, as measured by {@link StatFs}.
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
    public static File[] getExternalFilesDirs(Context context, String type) {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 19) {
            return ContextCompatKitKat.getExternalFilesDirs(context, type);
        } else {
            final File single;
            if (version >= 8) {
                single = ContextCompatFroyo.getExternalFilesDir(context, type);
            } else {
                single = buildPath(Environment.getExternalStorageDirectory(), DIR_ANDROID, DIR_DATA,
                        context.getPackageName(), DIR_FILES, type);
            }
            return new File[] { single };
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
     * most available space, as measured by {@link StatFs}.
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
    public static File[] getExternalCacheDirs(Context context) {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 19) {
            return ContextCompatKitKat.getExternalCacheDirs(context);
        } else {
            final File single;
            if (version >= 8) {
                single = ContextCompatFroyo.getExternalCacheDir(context);
            } else {
                single = buildPath(Environment.getExternalStorageDirectory(), DIR_ANDROID, DIR_DATA,
                        context.getPackageName(), DIR_CACHE);
            }
            return new File[] { single };
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
     * Return a drawable object associated with a particular resource ID.
     * <p>
     * Starting in {@link android.os.Build.VERSION_CODES#LOLLIPOP}, the returned
     * drawable will be styled for the specified Context's theme.
     *
     * @param id The desired resource identifier, as generated by the aapt tool.
     *            This integer encodes the package, type, and resource entry.
     *            The value 0 is an invalid identifier.
     * @return Drawable An object that can be used to draw this resource.
     */
    public static final Drawable getDrawable(Context context, int id) {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 21) {
            return ContextCompatApi21.getDrawable(context, id);
        } else {
            return context.getResources().getDrawable(id);
        }
    }

    /**
     * Returns the absolute path to the directory on the filesystem similar to
     * {@link Context#getFilesDir()}.  The difference is that files placed under this
     * directory will be excluded from automatic backup to remote storage on
     * devices running {@link android.os.Build.VERSION_CODES#LOLLIPOP} or later.  See
     * {@link android.app.backup.BackupAgent BackupAgent} for a full discussion
     * of the automatic backup mechanism in Android.
     *
     * <p>No permissions are required to read or write to the returned path, since this
     * path is internal storage.
     *
     * @return The path of the directory holding application files that will not be
     *         automatically backed up to remote storage.
     *
     * @see android.content.Context.getFilesDir
     */
    public final File getNoBackupFilesDir(Context context) {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 21) {
            return ContextCompatApi21.getNoBackupFilesDir(context);
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
    public final File getCodeCacheDir(Context context) {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 21) {
            return ContextCompatApi21.getCodeCacheDir(context);
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
}
