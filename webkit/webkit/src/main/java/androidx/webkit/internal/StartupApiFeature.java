/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.webkit.internal;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.webkit.WebViewCompat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Enum-like class to represent startup features that are supported by the AndroidX WebView API.
 * <p>
 * Features that have framework support should be represented by the appropriate subclass
 * matching the SDK version where the feature became available, which allows static analysis to
 * verify that calling the feature is safe through the {@link #isSupportedByFramework()} method.
 * <p>
 * To gain this benefit, variables containing {@link StartupApiFeature} should always be declared as
 * the specific subtype.
 * <p>
 * To add support for a new API version, add a new subclass representing the desired API level.
 * <p>
 * This class should only be instantiated as constants in {@link WebViewFeatureInternal} and is
 * meant to act as enum values for that class.
 * <p>
 * Startup API feature checks if a feature is supported in WebView by looking for metadata entries
 * in the WebView manifest. Calling this method does not lead to WebView being loaded into the
 * calling process.
 */
public abstract class StartupApiFeature {
    // Collection of declared values, populated by the constructor, to allow enum-like
    // iteration over all declared values.

    private static final Set<StartupApiFeature> sValues = new HashSet<>();
    // Do not change value, it is used by external AndroidManifest.xml files
    @VisibleForTesting
    public static final String METADATA_HOLDER_SERVICE_NAME = "org.chromium.android_webview"
            + ".services.StartupFeatureMetadataHolder";

    private final String mPublicFeatureValue;
    private final String mInternalFeatureValue;

    StartupApiFeature(@NonNull String publicFeatureValue, @NonNull String internalFeatureValue) {
        mPublicFeatureValue = publicFeatureValue;
        mInternalFeatureValue = internalFeatureValue;
        sValues.add(this);
    }

    @NonNull
    public String getPublicFeatureName() {
        return mPublicFeatureValue;
    }

    /**
     * Return whether this feature is supported.
     */
    public boolean isSupported(@NonNull Context context) {
        return isSupportedByFramework() || isSupportedByWebView(context);
    }


    /**
     * Return whether this {@link StartupApiFeature} is supported by the framework of the
     * current device.
     *
     * <p>Make sure the static type of the object you are calling this method on corresponds to one
     * of the subtypes of {@link StartupApiFeature} to ensure static analysis of the correct
     * framework level.
     */
    public abstract boolean isSupportedByFramework();

    /**
     * Return whether this {@link StartupApiFeature} is supported by the current WebView APK.
     *
     * <p>It checks if a feature is supported in WebView by looking for metadata entries
     * in the WebView manifest. Calling this method does not lead to WebView being loaded into the
     * calling process.
     */
    public boolean isSupportedByWebView(@NonNull Context context) {
        Bundle bundle = getMetaDataFromWebViewManifestOrNull(context);
        if (bundle == null) {
            return false;
        }
        return bundle.containsKey(mInternalFeatureValue);
    }

    /**
     * Get all instantiated values of this class as if it was an enum.
     */
    @NonNull
    public static Set<StartupApiFeature> values() {
        return Collections.unmodifiableSet(sValues);
    }

    private static @Nullable Bundle getMetaDataFromWebViewManifestOrNull(@NonNull Context context) {
        PackageInfo systemWebViewPackage = WebViewCompat.getCurrentWebViewPackage(context);
        if (systemWebViewPackage == null) {
            return null;
        }
        ComponentName compName =
                new ComponentName(systemWebViewPackage.packageName, METADATA_HOLDER_SERVICE_NAME);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PackageManager.ComponentInfoFlags flags =
                    ApiHelperForTiramisu.of(PackageManager.MATCH_DISABLED_COMPONENTS
                            | PackageManager.GET_META_DATA);
            try {
                return ApiHelperForTiramisu.getServiceInfo(context.getPackageManager(), compName,
                        flags)
                        .metaData;
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        } else {
            int flags = PackageManager.GET_META_DATA;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                flags |= PackageManager.MATCH_DISABLED_COMPONENTS;
            }
            try {

                return getServiceInfo(context, compName, flags)
                        .metaData;
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }
    }

    // To suppress deprecation warnings even when it is never called in API versions where it is
    // not deprecated.
    @SuppressWarnings("deprecation")
    private static ServiceInfo getServiceInfo(@NonNull Context context, ComponentName compName,
            int flags) throws PackageManager.NameNotFoundException {
        assert Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU;
        return context.getPackageManager()
                .getServiceInfo(compName, flags);
    }

    // --- Implement API version specific subclasses below this line ---

    /**
     * Represents a feature that was added in P.
     */
    public static class P extends StartupApiFeature {
        P(@NonNull String publicFeatureValue, @NonNull String internalFeatureValue) {
            super(publicFeatureValue, internalFeatureValue);
        }

        @Override
        public final boolean isSupportedByFramework() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
        }
    }

    /**
     * Represents a feature that is not supported by framework.
     */
    public static class NoFramework extends StartupApiFeature {
        NoFramework(@NonNull String publicFeatureValue, @NonNull String internalFeatureValue) {
            super(publicFeatureValue, internalFeatureValue);
        }

        @Override
        public final boolean isSupportedByFramework() {
            return false;
        }
    }
}
