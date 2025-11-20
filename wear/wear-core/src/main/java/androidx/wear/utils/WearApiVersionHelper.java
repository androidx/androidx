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

package androidx.wear.utils;

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import androidx.annotation.VisibleForTesting;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wear API Version helper for use in determining whether or not the API requirements associated
 * with a given API can be met. Given that Wear API is built incrementally on top of the Android
 * Platform/SDK API, API compatibility is determined both by checking the required Android Platform
 * API level and the Wear specific, incremental API level against that which is present on a device.
 * <br/><br/>
 * Ensuring runtime API compatibility is critical as the failure to do so can result in crashes
 * should you try to call an API which doesn't exist on a device containing an earlier version.
 * <br/><br/>
 * The following is an example of runtime API compatibility/checking with the WearApiVersionHelper:
 *
 * <pre>
 *     public void doFoo() {
 *         // Foo had some new features that were introduced in 33.4 - we need to ensure that users
 *         // on newer devices get the benefits of these features while still ensuring that our app
 *         // works for users on older devices - even if they don't get the benefit of the new
 *         // feature.
 *         if (WearApiVersionHelper.isApiVersionAtLeast(WEAR_TIRAMISU_4) {
 *             // use the new foo features that were introduced in Wear 33.4
 *         } else {
 *             // gracefully handle legacy foo behaviour
 *         }
 *     }
 * </pre>
 */
public final class WearApiVersionHelper {

    private static final String TAG = "WearApiVersionHelper";

    /**
     * Released Wear OS versions
     */
    @StringDef(
            value = {
                    WEAR_TIRAMISU_1,
                    WEAR_TIRAMISU_2,
                    WEAR_TIRAMISU_3,
                    WEAR_TIRAMISU_4,
                    WEAR_UDC_1,
                    WEAR_VIC_1,
                    WEAR_BAKLAVA_0,
            })
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    public @interface WearApiVersionCode {}
    /*
     * Note that there is no WEAR_TIRAMISU_0 as Wear did not have a release that corresponded
     * with the initial platform release of Android API level 33, Tiramisu.
     */

    /** The first Wear API version released on the Android T platform version (API level 33). */
    public static final String WEAR_TIRAMISU_1 = "WEAR_TIRAMISU_1";

    /**
     * The second Wear API version released on the Android T platform version (API level 33).
     */
    public static final String WEAR_TIRAMISU_2 = "WEAR_TIRAMISU_2";

    /**
     * The third Wear API version released on the Android T platform version (API level 33).
     */
    public static final String WEAR_TIRAMISU_3 = "WEAR_TIRAMISU_3";

    /**
     * The fourth Wear API version released on the Android T platform version (API level 33).
     */
    public static final String WEAR_TIRAMISU_4 = "WEAR_TIRAMISU_4";

    /*
     * Note that there will be no WEAR_UDC_0 as Wear does not have a release that corresponds
     * with the initial platform release of Android API level 34, Upside_Down_Cake.
     */

    /** The first Wear API version released on the Android U platform version (API level 34). */
    public static final String WEAR_UDC_1 = "WEAR_UDC_1";

    /*
     * Note that there will be no WEAR_VIC_0 as Wear does not have a release that corresponds
     * with the initial platform release of Android API level 35, Vanilla_Ice_Cream.
     */

    /** The first Wear API version released on the Android VIC platform version (API level 35). */
    public static final String WEAR_VIC_1 = "WEAR_VIC_1";

    /** The first Wear API version released on Android BAKLAVA (API level 36.0). */
    public static final String WEAR_BAKLAVA_0 = "WEAR_BAKLAVA_0";

    private static final String RELEASE_PROP = "ro.cw_build.platform_qpr.version";
    private static final int UNKNOWN_INCREMENTAL_RELEASE = -1;

    /*
     * The current version represents the API version abstraction as it applies to a given device
     * at runtime.
     */
    private static AbstractApiVersion sCurrentApiVersion;

    /*
     * A test version which can be mocked to simulate an API version other than the current/actual
     * version provided by a real device.
     */
    @VisibleForTesting static AbstractApiVersion sTestApiVersion = null;

    static {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Log.w(TAG, "The Wear SDK is not supported prior to "
                    + "WEAR_TIRAMISU_1 (API level 33.1)");
        }
        sCurrentApiVersion = new AbstractApiVersion() {
            @Override
            int getPlatformApiLevel() {
                return Build.VERSION.SDK_INT;
            }

            @Override
            int getIncrementalApiLevel() {
                return getIncrementalReleaseValue();
            }
        };
    }

    /**
     * API version abstraction comprising a platform & incremental API level.
     */
    @VisibleForTesting
    abstract static class AbstractApiVersion implements Comparable<AbstractApiVersion> {

        /**
         * Get the platform API level for this version.
         * @return an integer corresponding to the platform API level (e.g. 33). For
         * unrecognized/invalid versions this will return the max integer value.
         */
        @VisibleForTesting abstract int getPlatformApiLevel();

        /**
         * Get the incremental API level for this version.
         * @return an integer corresponding to the incremental API level (e.g. 2). For
         * unrecognized/invalid versions this will return the max integer value.
         */
        @VisibleForTesting abstract int getIncrementalApiLevel();

        @Override
        public int compareTo(@NonNull AbstractApiVersion o) {
            // major versioning at the platform level.
            if (getPlatformApiLevel() > o.getPlatformApiLevel()) {
                return 1;
            }
            if (getPlatformApiLevel() < o.getPlatformApiLevel()) {
                return -1;
            }
            // minor versioning at the incremental level.
            if (getIncrementalApiLevel() == o.getIncrementalApiLevel()) {
                return 0;
            }
            return getIncrementalApiLevel() > o.getIncrementalApiLevel() ? 1 : -1;
        }
    }

    /*
     * Internal instance of Wear's incremental API version abstraction based on predefined version
     * strings with a format "WEAR_<PLATFORM_RELEASE_NAME>_<INCREMENTAL_RELEASE_NUMBER> where the
     * platform release name and incremental release number are translated into platform and
     * incremental API levels respectively.
     */
    private static class WearApiVersionCompat extends AbstractApiVersion {

        private static final String VERSION_CODE_PATTERN_STRING =
                "WEAR_(\\w+)_(\\d+)";

        private static final String TIRAMISU = "TIRAMISU";
        private static final String UDC = "UDC";
        private static final String VIC = "VIC";
        private static final String BAKLAVA = "BAKLAVA";


        private int mPlatformApiLevel = Integer.MAX_VALUE;
        private int mIncrementalApiLevel = Integer.MAX_VALUE;

        private WearApiVersionCompat(@WearApiVersionCode @NonNull String versionString) {
            if (TextUtils.isEmpty(versionString)) {
                throw new IllegalArgumentException("Non-empty version required.");
            }

            Matcher matcher = Pattern.compile(VERSION_CODE_PATTERN_STRING).matcher(versionString);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid/unrecognized version: "
                        + versionString);
            }

            switch (matcher.group(1)) {
                case TIRAMISU:
                    mPlatformApiLevel = Build.VERSION_CODES.TIRAMISU;
                    break;
                case UDC:
                    mPlatformApiLevel = Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
                    break;
                case VIC:
                    mPlatformApiLevel = 35; // TODO: Build.VERSION_CODES.VANILLA_ICE_CREAM;
                    break;
                case BAKLAVA:
                    mPlatformApiLevel = 36; // TODO: Build.VERSION_CODES.BAKLAVA;
                    break;
            }

            mIncrementalApiLevel = Integer.parseInt(matcher.group(2));
        }

        @Override
        public int getPlatformApiLevel() {
            return mPlatformApiLevel;
        }

        @Override
        public int getIncrementalApiLevel() {
            return mIncrementalApiLevel;
        }
    }

    // static helper - do not instantiate.
    private WearApiVersionHelper() {}

    /**
     * Check if the current API version meets the specified requirements.
     * @param requiredVersion the required version corresponding to Wear OS release versions, as
     *                        defined within the WearApiVersionHelper - for example one of:
     *                        <ul>
     *                          <li>{@link #WEAR_TIRAMISU_1}
     *                          <li>{@link #WEAR_TIRAMISU_2}
     *                          <li>{@link #WEAR_TIRAMISU_3}
     *                          <li>{@link #WEAR_TIRAMISU_4}
     *                          <li>{@link #WEAR_UDC_1}
     *                          <li>{@link #WEAR_VIC_1}
     *                          <li>{@link #WEAR_BAKLAVA_0}
     *                          <li>etc
     *                        </ul>.
     *                        {@code IllegalArgumentException} will result for any other value.
     * @return true if the current API version is equal to or greater than the required version.
     */
    public static boolean isApiVersionAtLeast(@WearApiVersionCode @NonNull String requiredVersion) {
        if (sTestApiVersion != null) {
            return sTestApiVersion.compareTo(new WearApiVersionCompat(requiredVersion)) >= 0;
        } else if (Build.VERSION.SDK_INT < 36) { // TODO update to BAKLAVA when fullsdk updates
            return sCurrentApiVersion.compareTo(new WearApiVersionCompat(requiredVersion)) >= 0;
        } else {
            // As of Wear SDK API 36.0 we add a safe hashtable lookup for new API version constant
            // declarations (type safe as we key simply on string names).
            final com.google.wear.WearApiVersion apiVersion =
                    com.google.wear.Sdk.VERSION_CODES.lookup(requiredVersion);
            return apiVersion != null && com.google.wear.Sdk.isApiVersionAtLeast(apiVersion);
        }
    }

    private static Integer getIncrementalReleaseValue() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @SuppressLint("PrivateApi")
                Class<?> systemProperties = Class.forName("android.os.SystemProperties");
                Method getMethod = systemProperties.getMethod("get", String.class);
                String val = (String) getMethod.invoke(systemProperties, RELEASE_PROP);
                return (val == null) ? UNKNOWN_INCREMENTAL_RELEASE : Integer.parseInt(val);
            } else {
                return com.google.wear.Sdk.VERSION.RELEASE;
            }
        } catch (Exception e) {
            return UNKNOWN_INCREMENTAL_RELEASE;
        }
    }
}
