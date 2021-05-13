/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app;

import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.versioning.CarAppApiLevel;
import androidx.car.app.versioning.CarAppApiLevels;

/**
 * Container class for information about the app the host is connected to.
 *
 * <p>Hosts will use this information to provide the right level of compatibility, based on the
 * application's minimum and maximum API level and its own set of supported API levels.
 *
 * <p>The application minimum API level is defined in the application's manifest using the
 * following declaration.
 *
 * <pre>{@code
 * <manifest ...>
 *   <application ...>
 *     <meta-data
 *         android:name="androidx.car.app.minCarApiLevel"
 *         android:value="1" />
 *     ...
 *   </application>
 * </manifest>
 * }</pre>
 *
 * @see CarContext#getCarAppApiLevel()
 */
@CarProtocol
public final class AppInfo {
    // TODO(b/174803562): Automatically update the this version using Gradle
    private static final String LIBRARY_VERSION = "1.1.0-alpha01";

    /**
     * Application meta-data tag used to define the minimum Car App API level this application is
     * able to handle.
     *
     * <p>If not specified, the library assumes the application can only handle the Car App API
     * level designed by {@link CarAppApiLevels#getLatest()} at the time of compiling.
     *
     * @see CarContext#getCarAppApiLevel()
     */
    public static final String MIN_API_LEVEL_METADATA_KEY = "androidx.car.app.minCarApiLevel";

    @Keep
    @Nullable
    private final String mLibraryVersion;
    @Keep
    @CarAppApiLevel
    private final int mMinCarAppApiLevel;
    @Keep
    @CarAppApiLevel
    private final int mLatestCarAppApiLevel;

    /**
     * Creates an instance of {@link AppInfo} based on the input {@link Context}.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    @NonNull
    public static AppInfo create(@NonNull Context context) {
        @CarAppApiLevel
        int minApiLevel = retrieveMinCarAppApiLevel(context);
        if (minApiLevel < CarAppApiLevels.getOldest()
                || minApiLevel > CarAppApiLevels.getLatest()) {
            throw new IllegalArgumentException("Min API level (" + MIN_API_LEVEL_METADATA_KEY
                    + "=" + minApiLevel + ") is out of range (" + CarAppApiLevels.getOldest() + "-"
                    + CarAppApiLevels.getLatest() + ")");
        }
        return new AppInfo(minApiLevel, CarAppApiLevels.getLatest(), LIBRARY_VERSION);
    }

    /**
     * Creates an instance of {@link AppInfo} with the provided version information.
     *
     * @param minCarAppApiLevel    the minimal API level that can work with an app built with
     *                             the library
     * @param latestCarAppApiLevel the latest API level the library supports
     * @param libraryVersion       the library artifact version
     */
    @VisibleForTesting
    public AppInfo(@CarAppApiLevel int minCarAppApiLevel, @CarAppApiLevel int latestCarAppApiLevel,
            @NonNull String libraryVersion) {
        mMinCarAppApiLevel = minCarAppApiLevel;
        mLibraryVersion = libraryVersion;
        mLatestCarAppApiLevel = latestCarAppApiLevel;
    }

    // Used for serialization
    private AppInfo() {
        mMinCarAppApiLevel = 0;
        mLibraryVersion = null;
        mLatestCarAppApiLevel = 0;
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY)
    @VisibleForTesting
    @CarAppApiLevel
    public static int retrieveMinCarAppApiLevel(@NonNull Context context) {
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(),
                    PackageManager.GET_META_DATA);
            int apiLevel = applicationInfo.metaData != null
                                   ? applicationInfo.metaData.getInt(
                    MIN_API_LEVEL_METADATA_KEY, CarAppApiLevels.UNKNOWN)
                                   : CarAppApiLevels.UNKNOWN;
            if (apiLevel == CarAppApiLevels.UNKNOWN) {
                throw new IllegalArgumentException("Min API level not declared in manifest ("
                    + MIN_API_LEVEL_METADATA_KEY + ")");
            }
            return apiLevel;
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException("Unable to read min API level from manifest");
        }
    }

    /**
     * String representation of library version. This version string is opaque and not meant to
     * be parsed.
     */
    @NonNull
    public String getLibraryDisplayVersion() {
        return requireNonNull(mLibraryVersion);
    }

    @CarAppApiLevel
    public int getMinCarAppApiLevel() {
        return mMinCarAppApiLevel;
    }

    @CarAppApiLevel
    public int getLatestCarAppApiLevel() {
        return mLatestCarAppApiLevel;
    }

    @Override
    public String toString() {
        return "Library version: [" + getLibraryDisplayVersion() + "] Min Car Api Level: ["
                + getMinCarAppApiLevel() + "] Latest Car App Api Level: ["
                + getLatestCarAppApiLevel() + "]";
    }
}
