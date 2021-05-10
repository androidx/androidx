/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.extensions.internal;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.math.BigInteger;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class encapsulating a version with major, minor, patch and description values.
 */
@AutoValue
public abstract class Version implements Comparable<Version> {
    public static final Version VERSION_1_0 = Version.create(1, 0, 0, "");
    public static final Version VERSION_1_1 = Version.create(1, 1, 0, "");

    private static final Pattern VERSION_STRING_PATTERN =
            Pattern.compile("(\\d+)(?:\\.(\\d+))(?:\\.(\\d+))(?:\\-(.+))?");

    /**
     * Parses a string to a version object.
     *
     * @param versionString string in the format "1.2.3" or "1.2.3-Description"
     *                      (major.minor.patch[-description])
     * @return the parsed Version object or <tt>null</tt> if the versionString format is invalid.
     */
    @Nullable
    public static Version parse(@NonNull String versionString) {
        if (TextUtils.isEmpty(versionString)) {
            return null;
        }

        Matcher matcher = VERSION_STRING_PATTERN.matcher(versionString);
        if (!matcher.matches()) {
            return null;
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));
        String description = matcher.group(4) != null ? matcher.group(4) : "";
        return create(major, minor, patch, description);
    }

    /**
     * Creates a new instance of the Version object with the given parameters.
     */
    @NonNull
    public static Version create(int major, int minor, int patch, @NonNull String description) {
        return new AutoValue_Version(major, minor, patch, description);
    }

    /** Prevent subclassing. */
    Version() {
    }

    /**
     * Gets the major version number.
     */
    public abstract int getMajor();

    abstract int getMinor();

    abstract int getPatch();

    abstract String getDescription();

    @NonNull
    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder(getMajor() + "." + getMinor() + "." + getPatch());
        if (!TextUtils.isEmpty(getDescription())) {
            sb.append("-" + getDescription());
        }
        return sb.toString();
    }

    /**
     * To compare the major, minor and patch version with another.
     *
     * @param other The preference to compare to this one.
     * @return 0 if it have the same major minor and patch version; less than 0 if this
     * preference sorts ahead of <var>other</var>; greater than 0 if this preference sorts after
     * <var>other</var>.
     */
    @Override
    public int compareTo(@NonNull Version other) {
        return createBigInteger(this).compareTo(createBigInteger(other));
    }

    /**
     * To compare the major number with the input value.
     */
    public int compareTo(int majorVersion) {
        return compareTo(majorVersion, 0);
    }

    /**
     * To compare the major and minor numbers with the input values.
     */
    public int compareTo(int majorVersion, int minorVersion) {
        if (getMajor() == majorVersion) {
            return Integer.compare(getMinor(), minorVersion);
        }
        return Integer.compare(getMajor(), majorVersion);
    }

    private static BigInteger createBigInteger(Version version) {
        return BigInteger.valueOf(version.getMajor())
                .shiftLeft(32)
                .or(BigInteger.valueOf(version.getMinor()))
                .shiftLeft(32)
                .or(BigInteger.valueOf(version.getPatch()));
    }

    @Override
    public final boolean equals(Object obj) {
        if (!(obj instanceof Version)) {
            return false;
        }

        Version otherVersionObj = (Version) obj;

        // The equals checking ignores the description.
        return Objects.equals(getMajor(), otherVersionObj.getMajor())
                && Objects.equals(getMinor(), otherVersionObj.getMinor())
                && Objects.equals(getPatch(), otherVersionObj.getPatch());
    }

    @Override
    public final int hashCode() {
        // The hash code ignores the description.
        return Objects.hash(getMajor(), getMinor(), getPatch());
    }
}
