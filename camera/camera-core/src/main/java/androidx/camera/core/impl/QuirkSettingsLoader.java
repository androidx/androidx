/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.core.impl;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.camera.core.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for loading CameraX quirk settings from the application's metadata.
 *
 * <p>Quirks are used to address specific device or camera behaviors that may impact CameraX
 * functionality. This class provides the means to load quirk settings that have been configured
 * in the app's AndroidManifest.xml.
 *
 * <p>Metadata Format in AndroidManifest.xml
 * <pre>{@code
 * <application>
 *     <service
 *         android:name="androidx.camera.core.impl.QuirkSettingsLoader$MetadataHolderService"
 *         android:exported="false"
 *         android:enabled="false">
 *         <meta-data
 *             android:name="androidx.camera.core.quirks.DEFAULT_QUIRK_ENABLED"
 *             android:value="true" />
 *         <meta-data
 *             android:name="androidx.camera.core.quirks.FORCE_ENABLED"
 *             android:resource="@array/force_enabled_quirks" />
 *         <meta-data
 *             android:name="androidx.camera.core.quirks.FORCE_DISABLED"
 *             android:resource="@array/force_disabled_quirks" />
 *     </service>
 * </application>
 * }</pre>
 * <p>Resource File Format (e.g., res/values/arrays.xml). Each `item` within the `string-array`
 * should be the fully-qualified name of the corresponding {@link Quirk} class.
 * <pre>{@code
 * <resources>
 *     <string-array name="force_enabled_quirks">
 *         <item>androidx.camera.core.internal.compat.quirk.CaptureFailedRetryQuirk</item>
 *         <item>androidx.camera.core.internal.compat.quirk.ImageCaptureRotationOptionQuirk</item>
 *     </string-array>
 *     <string-array name="force_disabled_quirks">
 *         <item>androidx.camera.core.internal.compat.quirk.IncorrectJpegMetadataQuirk</item>
 *         <item>androidx.camera.core.internal.compat.quirk.LargeJpegImageQuirk</item>
 *     </string-array>
 * </resources>
 * }</pre>
 */
public class QuirkSettingsLoader implements Function<Context, QuirkSettings> {
    private static final String TAG = "QuirkSettingsLoader";

    // Metadata keys for quirk settings
    /**
     * Metadata key for specifying whether quirks should be enabled by default when the device
     * natively exhibits them.
     *
     * <p>Accepted values: `true` or `false`. Defaults to `true` if not present.
     */
    public static final String KEY_DEFAULT_QUIRK_ENABLED =
            "androidx.camera.core.quirks.DEFAULT_QUIRK_ENABLED";

    /**
     * Metadata key for referencing a string-array resource containing the fully-qualified names of
     * Quirk classes that should be forcibly enabled, regardless of device characteristics.
     *
     * <p>The resource value should be specified as `@array/your_array_name`.
     */
    public static final String KEY_QUIRK_FORCE_ENABLED =
            "androidx.camera.core.quirks.FORCE_ENABLED";

    /**
     * Metadata key for referencing a string-array resource containing the fully-qualified names of
     * Quirk classes that should be forcibly disabled, regardless of device characteristics.
     *
     * <p>The resource value should be specified as `@array/your_array_name`.
     */
    public static final String KEY_QUIRK_FORCE_DISABLED =
            "androidx.camera.core.quirks.FORCE_DISABLED";

    /**
     * Loads CameraX quirk settings from the metadata in the AndroidManifest.
     *
     * <p>This method retrieves metadata associated with the service {@link MetadataHolderService}
     * in the AndroidManifest. If the metadata exists and contains valid quirk settings, it builds
     * and returns a {@link QuirkSettings} object. Otherwise, it returns {@code null}.
     *
     * @param context The application context.
     * @return A QuirkSettings object containing the loaded settings, or null if the settings
     * could not be loaded.
     */
    @Override
    @Nullable
    public QuirkSettings apply(@NonNull Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            Bundle metadata = packageManager.getServiceInfo(
                    new ComponentName(context, MetadataHolderService.class),
                    PackageManager.GET_META_DATA | PackageManager.MATCH_DISABLED_COMPONENTS)
                    .metaData;
            if (metadata == null) {
                Logger.w(TAG, "No metadata in MetadataHolderService.");
                return null;
            }
            return buildQuirkSettings(context, metadata);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.d(TAG, "QuirkSettings$MetadataHolderService is not found.");
        }
        return null;
    }

    /**
     * Builds a QuirkSettings object from the metadata.
     *
     * @param context  The application context.
     * @param metadata The Bundle containing the metadata.
     * @return A QuirkSettings object constructed from the metadata.
     */
    @NonNull
    private static QuirkSettings buildQuirkSettings(@NonNull Context context,
            @NonNull Bundle metadata) {
        boolean defaultEnabled = metadata.getBoolean(KEY_DEFAULT_QUIRK_ENABLED, true);
        String[] forceEnabled = loadQuirks(context, metadata, KEY_QUIRK_FORCE_ENABLED);
        String[] forceDisabled = loadQuirks(context, metadata, KEY_QUIRK_FORCE_DISABLED);

        Logger.d(TAG, "Loaded quirk settings from metadata:");
        Logger.d(TAG, "  KEY_DEFAULT_QUIRK_ENABLED = " + defaultEnabled);
        Logger.d(TAG, "  KEY_QUIRK_FORCE_ENABLED = " + Arrays.toString(forceEnabled));
        Logger.d(TAG, "  KEY_QUIRK_FORCE_DISABLED = " + Arrays.toString(forceDisabled));

        return new QuirkSettings.Builder()
                .setEnabledWhenDeviceHasQuirk(defaultEnabled)
                .forceEnableQuirks(resolveQuirkNames(forceEnabled))
                .forceDisableQuirks(resolveQuirkNames(forceDisabled))
                .build();
    }

    /**
     * Loads Quirk names from a resource ID specified in the metadata Bundle.
     *
     * <p>If the resource ID is not found or invalid, an empty array is returned.
     */
    @NonNull
    private static String[] loadQuirks(@NonNull Context context, @NonNull Bundle metadata,
            @NonNull String key) {
        if (!metadata.containsKey(key)) {
            return new String[0];
        }
        int resourceId = metadata.getInt(key, -1);
        if (resourceId == -1) {
            Logger.w(TAG, "Resource ID not found for key: " + key);
            return new String[0];
        }
        try {
            return context.getResources().getStringArray(resourceId);
        } catch (Resources.NotFoundException e) {
            Logger.w(TAG, "Quirk class names resource not found: " + resourceId, e);
        }
        return new String[0];
    }

    /** Resolves an array of quirk class names into a set of corresponding Quirk class objects. */
    @NonNull
    private static Set<Class<? extends Quirk>> resolveQuirkNames(@NonNull String[] nameArray) {
        Set<Class<? extends Quirk>> quirkSet = new HashSet<>();
        for (String quirkName : nameArray) {
            Class<? extends Quirk> clazz = resolveQuirkName(quirkName);
            if (clazz != null) {
                quirkSet.add(clazz);
            }
        }
        return quirkSet;
    }

    /** Attempts to resolve and load a Quirk class from its fully qualified name. */
    @SuppressWarnings("unchecked")
    @Nullable
    private static Class<? extends Quirk> resolveQuirkName(@NonNull String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (Quirk.class.isAssignableFrom(clazz)) {
                return (Class<? extends Quirk>) clazz;
            } else {
                Logger.w(TAG, className + " does not implement the Quirk interface.");
            }
        } catch (ClassNotFoundException e) {
            Logger.w(TAG, "Class not found: " + className, e);
        }
        return null;
    }

    /**
     * A placeholder service to avoid adding application-level metadata. The service is only used to
     * expose metadata defined in the library's manifest. It is never invoked.
     *
     * <p>See go/androidx-api-guidelines#resources-manifest for more detail.
     */
    public static class MetadataHolderService extends Service {

        private MetadataHolderService() {
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            throw new UnsupportedOperationException();
        }
    }
}
