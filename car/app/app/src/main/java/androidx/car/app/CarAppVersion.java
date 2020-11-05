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

import static androidx.car.app.CarAppVersion.ReleaseSuffix.RELEASE_SUFFIX_BETA;
import static androidx.car.app.CarAppVersion.ReleaseSuffix.RELEASE_SUFFIX_EAP;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * A versioning class used for compatibility checks between the host and client.
 *
 * <p>The version scheme follows semantic versioning and is defined as:
 *
 * <pre>major.minor.patch[-releasesSuffix.build]<pre>
 *
 * where:
 *
 * <ul>
 *   <li>major version differences denote binary incompatibility (e.g. API removal)
 *   <li>minor version differences denote compatible API changes (e.g. API additional/deprecation)
 *   <li>patch version differences denote non-API altering internal changes (e.g. bug fixes)
 *   <li>releaseSuffix is{@code null} for stable versions but otherwise reserved for special-purpose
 *       EAP builds and one-off public betas. For cases where a release suffix is provided, the
 *       appended build number is used to differentiate versions within the same suffix category.
 *       (e.g. 1.0.0-eap.1 vs 1.0.0-eap.2).
 * </ul>
 */
public class CarAppVersion {
    private static final String MAIN_VERSION_FORMAT = "%d.%d.%d";
    private static final String SUFFIX_VERSION_FORMAT = "-%s.%d";

    public static final CarAppVersion INSTANCE =
            CarAppVersion.create(1, 0, 0, RELEASE_SUFFIX_BETA, 1);

    /** Min version of the SDK which supports handshake completed binder call. */
    public static final CarAppVersion HANDSHAKE_MIN_VERSION =
            CarAppVersion.create(1, 0, 0, RELEASE_SUFFIX_BETA, 1);

    /** Different types of supported version suffixes. */
    public enum ReleaseSuffix {
        RELEASE_SUFFIX_EAP("eap"),
        RELEASE_SUFFIX_BETA("beta");

        private final String mValue;

        ReleaseSuffix(String value) {
            this.mValue = value;
        }

        /** Creates a {@link ReleaseSuffix} from the string standard representation as described
         * in the {@link #toString()} method */
        @NonNull
        public static ReleaseSuffix fromString(@NonNull String value) {
            switch (value) {
                case "eap":
                    return RELEASE_SUFFIX_EAP;
                case "beta":
                    return RELEASE_SUFFIX_BETA;
                default:
                    throw new IllegalArgumentException(value + " is not a valid release suffix");
            }
        }

        @NonNull
        @Override
        public String toString() {
            return mValue;
        }
    }

    private final int mMajor;
    private final int mMinor;
    private final int mPatch;
    @Nullable
    private final ReleaseSuffix mReleaseSuffix;
    private final int mBuild;

    /** Creates a {@link CarAppVersion} without a release suffix. (e.g. 1.0.0) */
    @NonNull
    static CarAppVersion create(int major, int minor, int patch) {
        return new CarAppVersion(major, minor, patch, null, 0);
    }

    /**
     * Creates a {@link CarAppVersion} with a release suffix and build number. (e.g. 1.0.0-eap.0)
     *
     * <p>Note that if {@code releaseSuffix} is {@code null}, then {@code build} is ignored.
     */
    @NonNull
    static CarAppVersion create(
            int major, int minor, int patch, ReleaseSuffix releaseSuffix, int build) {
        return new CarAppVersion(major, minor, patch, releaseSuffix, build);
    }

    /**
     * Creates a {@link CarAppVersion} instance based on its string representation.
     *
     * <p>The string should be of the format major.minor.patch(-releaseSuffix.build). If the
     * string is malformed or {@code null}, {@code null} will be returned.
     */
    @Nullable
    public static CarAppVersion of(@NonNull String versionString) throws MalformedVersionException {
        return parseVersionString(versionString);
    }

    private static CarAppVersion parseVersionString(String versionString)
            throws MalformedVersionException {
        String[] versionSplit = versionString.split("-", -1);
        if (versionSplit.length > 2) {
            throw new MalformedVersionException(
                    "Malformed version string (more than 1 \"-\" detected): " + versionString);
        }

        String mainVersion = versionSplit[0];
        String[] mainVersionSplit = mainVersion.split("\\.", -1);

        // Main version should be formatted as major.minor.patch
        if (mainVersionSplit.length != 3) {
            throw new MalformedVersionException(
                    "Malformed version string (invalid main version format): " + versionString);
        }

        int major;
        int minor;
        int patch;
        ReleaseSuffix releaseSuffix = null;
        int build = 0;

        try {
            major = Integer.parseInt(mainVersionSplit[0]);
            minor = Integer.parseInt(mainVersionSplit[1]);
            patch = Integer.parseInt(mainVersionSplit[2]);

            String suffixVersion = versionSplit.length > 1 ? versionSplit[1] : null;
            if (suffixVersion != null) {
                String[] suffixVersionSplit = suffixVersion.split("\\.", -1);

                // Release suffix should be formatted as releaseSuffix.build
                if (suffixVersionSplit.length != 2) {
                    throw new MalformedVersionException(
                            "Malformed version string (invalid suffix version format): "
                                    + versionString);
                }

                try {
                    releaseSuffix = ReleaseSuffix.fromString(suffixVersionSplit[0]);
                } catch (IllegalArgumentException e) {
                    throw new MalformedVersionException(
                            "Malformed version string (unsupported suffix): " + versionString, e);
                }
                build = Integer.parseInt(suffixVersionSplit[1]);
            }

            return new CarAppVersion(major, minor, patch, releaseSuffix, build);
        } catch (NumberFormatException exception) {
            throw new MalformedVersionException(
                    "Malformed version string (unsupported characters): " + versionString,
                    exception);
        }
    }

    private CarAppVersion(
            int major, int minor, int patch, @Nullable ReleaseSuffix releaseSuffix, int build) {
        this.mMajor = major;
        this.mMinor = minor;
        this.mPatch = patch;
        this.mReleaseSuffix = releaseSuffix;
        this.mBuild = build;
    }

    /** Returns the human-readable format of this version. */
    @NonNull
    @Override
    public String toString() {
        String versionString = String.format(Locale.US, MAIN_VERSION_FORMAT, mMajor, mMinor,
                mPatch);
        if (mReleaseSuffix != null) {
            versionString += String.format(Locale.US, SUFFIX_VERSION_FORMAT, mReleaseSuffix,
                    mBuild);
        }

        return versionString;
    }

    /**
     * Checks whether this {@link CarAppVersion} is greater than or equal to {@code other}, which is
     * used to determine compatibility. Returns true if so, false otherwise.
     *
     * <p>The rules of comparison are as follow:
     *
     * <ul>
     *   <li>If either versions are suffixed with {@link ReleaseSuffix#RELEASE_SUFFIX_EAP}, the
     *   version strings have to be exact match to be considered compatible.
     *   <li>The major version has to be greater or equal to be considered compatible.
     *   <li>If major versions are equal, the minor version has to be greater or equal to be
     *   considered compatible.
     *   <li>If major.minor versions are equal, the patch version has to be greater or equal to
     *   be considered compatible.
     *   <li>If the major.minor.patch versions are equal, for stable versions release suffix
     *   equals {@code null}, the instance is considered compatible with all
     *   {@link ReleaseSuffix#RELEASE_SUFFIX_BETA} versions; for
     *   {@link ReleaseSuffix#RELEASE_SUFFIX_BETA}, the instance is considered compatible iff the
     *   other is a {@link ReleaseSuffix#RELEASE_SUFFIX_BETA} version and that the build is
     *   greater or equal to the other version.
     * </ul>
     */
    public boolean isGreaterOrEqualTo(@NonNull CarAppVersion other) {
        // For EAP versions, we require an exact match.
        if (mReleaseSuffix == RELEASE_SUFFIX_EAP || other.mReleaseSuffix == RELEASE_SUFFIX_EAP) {
            return mMajor == other.mMajor
                    && mMinor == other.mMinor
                    && mPatch == other.mPatch
                    && mReleaseSuffix == other.mReleaseSuffix
                    && mBuild == other.mBuild;
        }

        int result = Integer.compare(mMajor, other.mMajor);
        // Major version differs, return.
        if (result != 0) {
            return result == 1;
        }

        // Minor version differs, return.
        result = Integer.compare(mMinor, other.mMinor);
        if (result != 0) {
            return result == 1;
        }

        // Patch version differs, return.
        result = Integer.compare(mPatch, other.mPatch);
        if (result != 0) {
            return result == 1;
        }

        if (mReleaseSuffix == null) {
            // A stable version (is considered compatible to any beta versions, the suffix
            // version is
            // ignored in those cases.
            return true;
        } else if (mReleaseSuffix == RELEASE_SUFFIX_BETA) {
            // Compatible if beta build is greater than other's beta build.
            if (other.mReleaseSuffix == RELEASE_SUFFIX_BETA) {
                return mBuild >= other.mBuild;
            } else {
                // Beta version is incompatible with stable version.
                return false;
            }
        } else {
            throw new IllegalStateException("Invalid release suffix: " + mReleaseSuffix);
        }
    }
}
