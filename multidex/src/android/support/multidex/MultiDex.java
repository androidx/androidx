/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.multidex;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Monkey patches {@link Context#getClassLoader() the application context class
 * loader} in order to load classes from more than one dex file. The primary
 * {@code classes.dex} file necessary for calling this class methods. secondary
 * dex files named classes2.dex, classes".dex... found in the application apk
 * will be added to the classloader after first call to
 * {@link #install(Context)}.
 *
 * <p/>
 * <strong>IMPORTANT:</strong>This library provides compatibility for platforms
 * with API level 14 through 18 (ICS and JB). This library does nothing on newer
 * versions of the platform which provide built-in support for secondary dex
 * files.
 */
public final class MultiDex {

    static final String TAG = "MultiDex";

    private static final String SECONDARY_FOLDER_NAME = "secondary-dexes";

    private static final int SUPPORTED_MULTIDEX_SDK_VERSION = 19;

    private static final int MIN_SDK_VERSION = 14;

    private static final Set<String> installedApk = new HashSet<String>();

    private MultiDex() {}

    /**
     * Patches the application context class loader by appending extra dex files
     * loaded from the application apk. Call this method first thing in your
     * {@code Application#OnCreate}, {@code Instrumentation#OnCreate},
     * {@code BackupAgent#OnCreate}, {@code Service#OnCreate},
     * {@code BroadcastReceiver#onReceive}, {@code Activity#OnCreate} and
     * {@code ContentProvider#OnCreate} .
     *
     * @param context application context.
     * @throws RuntimeException if an error occurred preventing the classloader
     *         extension.
     */
    public static void install(Context context) {
        /* The patched class loader is expected to be a descendant of
         * dalvik.system.BaseDexClassLoader. We modify its
         * dalvik.system.DexPathList pathList field to append additional DEX
         * file entries.
         */
        ClassLoader loader = context.getClassLoader();
        if (loader == null) {
            // Note, the context class loader is null when running Robolectric tests.
            Log.e(TAG,
                    "Context class loader is null. Must be running in test mode. Skip patching.");
            return;
        }

        if (Build.VERSION.SDK_INT < MIN_SDK_VERSION) {
            throw new RuntimeException("Multi dex installation failed. SDK " + Build.VERSION.SDK_INT
                    + " is unsupported. Min SDK version is " + MIN_SDK_VERSION + ".");
        }


        try {
            ApplicationInfo applicationInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);

            synchronized (installedApk) {
                String apkPath = applicationInfo.sourceDir;
                if (installedApk.contains(apkPath)) {
                    return;
                }
                installedApk.add(apkPath);

                if (Build.VERSION.SDK_INT >= SUPPORTED_MULTIDEX_SDK_VERSION) {
                    Log.i(TAG, "SDK " + Build.VERSION.SDK_INT
                            + " supports multi dex files, no need to patch classpath.");
                    return;
                }

                File dexDir = new File(context.getFilesDir(), SECONDARY_FOLDER_NAME);
                List<File> files = MultiDexExtractor.load(context, applicationInfo, dexDir);
                if (files.size() > 0) {
                    V14.install(loader, files, dexDir);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Multidex installation failure", e);
            throw new RuntimeException("Multi dex installation failed (" + e.getMessage() + ").");
        }
    }

    /**
     * Locates a given field anywhere in the class inheritance hierarchy.
     *
     * @param instance an object to search the field into.
     * @param name field name
     * @return a field object
     * @throws NoSuchFieldException if the field cannot be located
     */
    private static Field findField(Object instance, String name) throws NoSuchFieldException {
        for (Class<?> clazz = instance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            try {
                Field field = clazz.getDeclaredField(name);


                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }

                return field;
            } catch (NoSuchFieldException e) {
                // ignore and search next
            }
        }

        throw new NoSuchFieldException("Field " + name + " not found in " + instance.getClass());
    }

    /**
     * Locates a given method anywhere in the class inheritance hierarchy.
     *
     * @param instance an object to search the method into.
     * @param name method name
     * @param parameterTypes method parameter types
     * @return a method object
     * @throws NoSuchMethodException if the method cannot be located
     */
    private static Method findMethod(Object instance, String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        for (Class<?> clazz = instance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            try {
                Method method = clazz.getDeclaredMethod(name, parameterTypes);


                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }

                return method;
            } catch (NoSuchMethodException e) {
                // ignore and search next
            }
        }

        throw new NoSuchMethodException("Method " + name + " with parameters " +
                Arrays.asList(parameterTypes) + " not found in " + instance.getClass());
    }

    /**
     * Installer for platform versions 14, 15, 16, 17 and 18.
     */
    private static final class V14 {

        private static void install(ClassLoader loader, List<File> additionalClassPathEntries,
                File optimizedDirectory)
                        throws IllegalArgumentException, IllegalAccessException,
                        NoSuchFieldException, InvocationTargetException, NoSuchMethodException {
            Field pathListField = findField(loader, "pathList");
            Object dexPathList = pathListField.get(loader);
            extendElements(dexPathList, additionalClassPathEntries, optimizedDirectory);
        }

        /**
         * Appends extra elements created from the given dex or apk archives to the
         * existing {@code dexElements} of the wrapped {@code DexPathList}.
         *
         * @param dexPathList the dalvik.system.DexPathList
         * @param additionalClassPathEntries a list of zip files containing a classes.dex to add to
         * dexPathList.
         * @param optimizedDirectory a directory used by the system to optimize the
         *        dex
         */
        private static void extendElements(Object dexPathList,
                Collection<? extends File> additionalClassPathEntries, File optimizedDirectory)
                        throws NoSuchFieldException, IllegalAccessException,
                        InvocationTargetException, NoSuchMethodException {
            if (additionalClassPathEntries.isEmpty()) {
                return;
            }
            Field dexElementsField = findField(dexPathList, "dexElements");
            Object[] original = (Object[]) dexElementsField.get(dexPathList);
            Object[] extra = makeDexElements(dexPathList,
                    new ArrayList<File>(additionalClassPathEntries), optimizedDirectory);
            Object[] combined = (Object[]) Array.newInstance(
                    original.getClass().getComponentType(), original.length + extra.length);
            System.arraycopy(original, 0, combined, 0, original.length);
            System.arraycopy(extra, 0, combined, original.length, extra.length);
            dexElementsField.set(dexPathList, combined);
        }

        /**
         * A wrapper around
         * {@code private static final dalvik.system.DexPathList#makeDexElements}.
         */
        private static Object[] makeDexElements(
                Object dexPathList, ArrayList<File> files, File optimizedDirectory)
                        throws IllegalAccessException, InvocationTargetException,
                        NoSuchMethodException {
            Method makeDexElements =
                    findMethod(dexPathList, "makeDexElements", ArrayList.class, File.class);

            return (Object[]) makeDexElements.invoke(dexPathList, files, optimizedDirectory);
        }
    }
}
