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

package androidx.window;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class encapsulating a version with major, minor, patch and description values.
 */
class Version implements Comparable<Version> {
    static final Version UNKNOWN = new Version(0, 0, 0, "");
    static final Version VERSION_0_1 = new Version(0, 1, 0, "");
    static final Version VERSION_1_0 = new Version(1, 0, 0, "");
    static final Version CURRENT = VERSION_1_0;

    private static final String VERSION_PATTERN_STRING =
            "(\\d+)(?:\\.(\\d+))(?:\\.(\\d+))(?:-(.+))?";

    private final int mMajor;
    private final int mMinor;
    private final int mPatch;
    private final String mDescription;
    // Cached BigInteger value of the version.
    private BigInteger mBigInteger;

    private Version(int major, int minor, int patch, String description) {
        mMajor = major;
        mMinor = minor;
        mPatch = patch;
        mDescription = description;
    }

    /**
     * Parses a string to a version object.
     *
     * @param versionString string in the format "1.2.3" or "1.2.3-Description"
     *                      (major.minor.patch[-description])
     * @return the parsed Version object or {@code null}> if the versionString format is invalid.
     */
    static Version parse(String versionString) {
        if (TextUtils.isEmpty(versionString)) {
            return null;
        }

        Matcher matcher = Pattern.compile(VERSION_PATTERN_STRING).matcher(versionString);
        if (!matcher.matches()) {
            return null;
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));
        String description = matcher.group(4) != null ? matcher.group(4) : "";
        return new Version(major, minor, patch, description);
    }

    int getMajor() {
        return mMajor;
    }

    int getMinor() {
        return mMinor;
    }

    int getPatch() {
        return mPatch;
    }

    String getDescription() {
        return mDescription;
    }

    @NonNull
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getMajor())
                .append(".")
                .append(getMinor())
                .append(".")
                .append(getPatch());
        if (!TextUtils.isEmpty(getDescription())) {
            sb.append("-").append(getDescription());
        }
        return sb.toString();
    }

    /**
     * To compare the major, minor and patch version with another.
     *
     * @param other The version to compare to this one.
     * @return 0 if it have the same major minor and patch version; less than 0 if this version
     * sorts ahead of <var>other</var>; greater than 0 if this version sorts after <var>other</var>.
     */
    @Override
    public int compareTo(@NonNull Version other) {
        return toBigInteger().compareTo(other.toBigInteger());
    }

    @NonNull
    private BigInteger toBigInteger() {
        if (mBigInteger == null) {
            mBigInteger = BigInteger.valueOf(mMajor)
                    .shiftLeft(32)
                    .or(BigInteger.valueOf(mMinor))
                    .shiftLeft(32)
                    .or(BigInteger.valueOf(mPatch));
        }
        return mBigInteger;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Version)) {
            return false;
        }

        Version otherVersionObj = (Version) obj;

        // The equals checking ignores the description.
        return mMajor == otherVersionObj.mMajor
                && mMinor == otherVersionObj.mMinor
                && mPatch == otherVersionObj.mPatch;
    }

    @Override
    public int hashCode() {
        // The hash code ignores the description.
        int result = 17;
        result = result * 31 + mMajor;
        result = result * 31 + mMinor;
        result = result * 31 + mPatch;
        return result;
    }
}
