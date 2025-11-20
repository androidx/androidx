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

package androidx.wear.tiles;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.BiFunction;

/**
 * Base class for holders of protobuf messages that can be parceled to be transferred to the rest of
 * the system.
 *
 * <ul>
 *   <li>Version 1: encodes the version (int) and the proto contents (byte[]).
 *   <li>Version 2: encodes the version (int), the proto contents (byte[]) and a {@link Bundle}.
 * </ul>
 *
 * <p>IMPORTANT: Only use Version 2 if the reader of this Parcelable can also handle V2.
 */
@SuppressWarnings("AndroidApiChecker") // Uses java.util.function.Function
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ProtoParcelable implements Parcelable {
    private final byte[] mContents;
    private final @Nullable Bundle mExtras;
    private final int mVersion;

    static <T extends ProtoParcelable> Creator<T> newCreator(
            Class<T> clazz, TriFunction<byte[], Bundle, Integer, T> creator) {
        return new Creator<T>() {

            @Override
            public T createFromParcel(Parcel source) {
                int version = source.readInt();
                byte[] payload = source.createByteArray();
                Bundle extras = null;
                if (version >= 2) {
                    extras = source.readBundle(getClass().getClassLoader());
                }

                return creator.apply(payload, extras, version);
            }

            @SuppressWarnings("unchecked")
            @Override
            public T[] newArray(int size) {
                return (T[]) Array.newInstance(clazz, size);
            }
        };
    }

    static <T extends ProtoParcelable> Creator<T> newCreator(
            Class<T> clazz, BiFunction<byte[], Integer, T> creator) {
        return newCreator(clazz, (bytes, unused, version) -> creator.apply(bytes, version));
    }

    protected ProtoParcelable(byte @NonNull [] contents, int version) {
        this(contents, null, version);
    }

    protected ProtoParcelable(byte @NonNull [] contents, @Nullable Bundle extras, int version) {
        this.mContents = contents;
        this.mExtras = extras;
        this.mVersion = version;
    }

    /** Get the payload contained within this ProtoParcelable. */
    public byte @NonNull [] getContents() {
        return mContents;
    }

    /** Get the extras {@link Bundle} contained within this ProtoParcelable. */
    public @Nullable Bundle getExtras() {
        return mExtras;
    }

    /**
     * Gets the version of this Parcelable. This can be used to detect what type of data is returned
     * by {@link #getContents()}.
     */
    public int getVersion() {
        return mVersion;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mVersion);
        dest.writeByteArray(mContents);
        if (mVersion >= 2) {
            dest.writeBundle(mExtras);
        }
    }

    @Override
    @SuppressLint("EqualsGetClass")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        // We want to use getClass here, as this class is designed to be subtyped immediately.
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProtoParcelable that = (ProtoParcelable) o;
        return mVersion == that.mVersion
                && Arrays.equals(mContents, that.mContents)
                && bundleEquals(mExtras, that.mExtras);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mVersion, Arrays.hashCode(mContents), bundleHashCode(mExtras));
    }

    /** Represents a function that accepts three arguments and produces a result. */
    interface TriFunction<A, B, C, R2> {
        R2 apply(A var1, B var2, C var3);
    }

    /**
     * Calculates a hashCode for a Bundle by applying {@code Arrays#hashCode} to an array of
     * hashCodes of alternating key, value pairs (i.e. [key0, value0, key1, value1, ...], sorted by
     * key, each calculated using {@code link Object#hashCode} or {@code bundleHashCode}.
     */
    private static int bundleHashCode(@Nullable Bundle b) {
        if (b == null) {
            return 0;
        }

        TreeSet<String> sortedKeySet = new TreeSet<>(b.keySet());
        int[] hashCodes = new int[sortedKeySet.size() * 2];
        int i = 0;
        for (String key : sortedKeySet) {
            hashCodes[i++] = Objects.hashCode(key);
            @SuppressWarnings("deprecation") // Type is checked below.
            Object value = b.get(key);

            if (value instanceof Bundle) {
                hashCodes[i++] = bundleHashCode((Bundle) value);
            } else {
                hashCodes[i++] = Objects.hashCode(value);
            }
        }

        return Arrays.hashCode(hashCodes);
    }

    /**
     * Compares two Bundles and returns true if they are equal.
     *
     * <p>Equality in this case means that both bundles contain the same set of keys and their
     * corresponding values are all equal (using the {@link Object#equals(Object)} method or
     * recursively {@code bundleEquals}).
     */
    private static boolean bundleEquals(@Nullable Bundle a, @Nullable Bundle b) {
        if (a == b) {
            return true;
        }

        if (a == null || b == null) {
            return false;
        }

        if (!a.keySet().equals(b.keySet())) {
            return false;
        }

        for (String key : a.keySet()) {
            @SuppressWarnings("deprecation") // Type is checked below.
            Object aValue = a.get(key);
            @SuppressWarnings("deprecation") // Type is checked below.
            Object bValue = b.get(key);

            if (aValue instanceof Bundle && bValue instanceof Bundle) {
                if (!bundleEquals((Bundle) aValue, (Bundle) bValue)) {
                    return false;
                }
            } else if (!Objects.deepEquals(aValue, bValue)) {
                return false;
            }
        }

        return true;
    }
}
