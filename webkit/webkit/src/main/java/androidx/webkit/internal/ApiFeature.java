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

import android.os.Build;

import androidx.annotation.VisibleForTesting;

import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Enum-like class to represent features that are supported by the AndroidX WebView API.
 * <p>
 * Features that have framework support should be represented by the appropriate subclass
 * matching the SDK version where the feature became available, which allows static analysis to
 * verify that calling the feature is safe through the {@link #isSupportedByFramework()} method.
 * <p>
 * To gain this benefit, variables containing {@link ApiFeature} should always be declared as the
 * specific subtype.
 * <p>
 * To add support for a new API version, add a new subclass representing the desired API level.
 * <p>
 * This class should only be instantiated as constants in {@link WebViewFeatureInternal} and is
 * meant to act as enum values for that class.
 */
public abstract class ApiFeature implements ConditionallySupportedFeature {

    // Collection of declared values, populated by the constructor, to allow enum-like
    // iteration over all declared values.
    private static final Set<ApiFeature> sValues = new HashSet<>();

    private final String mPublicFeatureValue;
    private final String mInternalFeatureValue;

    ApiFeature(@NonNull String publicFeatureValue, @NonNull String internalFeatureValue) {
        mPublicFeatureValue = publicFeatureValue;
        mInternalFeatureValue = internalFeatureValue;
        sValues.add(this);
    }

    @Override
    public @NonNull String getPublicFeatureName() {
        return mPublicFeatureValue;
    }

    @Override
    public boolean isSupported() {
        return isSupportedByFramework() || isSupportedByWebView();
    }

    /**
     * Return whether this {@link ApiFeature} is supported by the framework of the
     * current device.
     *
     * <p>Make sure the static type of the object you are calling this method on corresponds to one
     * of the subtypes of {@link ApiFeature} to ensure static analysis of the correct framework
     * level.
     */
    public abstract boolean isSupportedByFramework();

    /**
     * Return whether this {@link ApiFeature} is supported by the current WebView APK.
     */
    public boolean isSupportedByWebView() {
        return BoundaryInterfaceReflectionUtil.containsFeature(LAZY_HOLDER.WEBVIEW_APK_FEATURES,
                mInternalFeatureValue);
    }

    /**
     * Get all instantiated values of this class as if it was an enum.
     */
    public static @NonNull Set<ApiFeature> values() {
        return Collections.unmodifiableSet(sValues);
    }


    @VisibleForTesting
    public static @NonNull Set<String> getWebViewApkFeaturesForTesting() {
        return LAZY_HOLDER.WEBVIEW_APK_FEATURES;
    }

    private static class LAZY_HOLDER {
        static final Set<String> WEBVIEW_APK_FEATURES = new HashSet<>(
                Arrays.asList(WebViewGlueCommunicator.getFactory().getWebViewFeatures()));
    }

    // --- Implement API version specific subclasses below this line ---

    /**
     * Represents a feature that has no framework support.
     */
    public static class NoFramework extends ApiFeature {

        NoFramework(@NonNull String publicFeatureValue, @NonNull String internalFeatureValue) {
            super(publicFeatureValue, internalFeatureValue);
        }

        @Override
        public final boolean isSupportedByFramework() {
            return false;
        }
    }

    /**
     * Represents a feature that was added in M.
     */
    public static class M extends ApiFeature {

        M(@NonNull String publicFeatureValue, @NonNull String internalFeatureValue) {
            super(publicFeatureValue, internalFeatureValue);
        }

        @Override
        public final boolean isSupportedByFramework() {
            return true;
        }
    }

    /**
     * Represents a feature that was added in N.
     */
    public static class N extends ApiFeature {

        N(@NonNull String publicFeatureValue, @NonNull String internalFeatureValue) {
            super(publicFeatureValue, internalFeatureValue);
        }

        @Override
        public final boolean isSupportedByFramework() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
        }
    }

    /**
     * Represents a feature that was added in O.
     */
    public static class O extends ApiFeature {

        O(@NonNull String publicFeatureValue, @NonNull String internalFeatureValue) {
            super(publicFeatureValue, internalFeatureValue);
        }

        @Override
        public final boolean isSupportedByFramework() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
        }
    }

    /**
     * Represents a feature that was added in O_MR1.
     */
    public static class O_MR1 extends ApiFeature {

        O_MR1(@NonNull String publicFeatureValue, @NonNull String internalFeatureValue) {
            super(publicFeatureValue, internalFeatureValue);
        }

        @Override
        public final boolean isSupportedByFramework() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1;
        }
    }

    /**
     * Represents a feature that was added in P.
     */
    public static class P extends ApiFeature {

        P(@NonNull String publicFeatureValue, @NonNull String internalFeatureValue) {
            super(publicFeatureValue, internalFeatureValue);
        }

        @Override
        public final boolean isSupportedByFramework() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
        }
    }

    /**
     * Represents a feature that was added in Q.
     */
    public static class Q extends ApiFeature {

        Q(@NonNull String publicFeatureValue, @NonNull String internalFeatureValue) {
            super(publicFeatureValue, internalFeatureValue);
        }

        @Override
        public final boolean isSupportedByFramework() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
        }
    }

    /**
     * Represents a feature that was added in T.
     */
    public static class T extends ApiFeature {
        T(@NonNull String publicFeatureValue, @NonNull String internalFeatureValue) {
            super(publicFeatureValue, internalFeatureValue);
        }

        @Override
        public final boolean isSupportedByFramework() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
        }
    }
}
