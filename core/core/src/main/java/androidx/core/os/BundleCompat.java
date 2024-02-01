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

package androidx.core.os;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.SparseArray;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Helper for accessing features in {@link Bundle}.
 */
public final class BundleCompat {
    private BundleCompat() {
        /* Hide constructor */
    }

    /**
     * Returns the value associated with the given key or {@code null} if:
     * <ul>
     *     <li>No mapping of the desired type exists for the given key.
     *     <li>A {@code null} value is explicitly associated with the key.
     *     <li>The object is not of type {@code clazz}.
     * </ul>
     * <p>
     * <b>Note: </b> if the expected value is not a class provided by the Android platform, you
     * must call {@link Bundle#setClassLoader(ClassLoader)} with the proper {@link ClassLoader}
     * first. Otherwise, this method might throw an exception or return {@code null}.
     * <p>
     * Compatibility behavior:
     * <ul>
     *     <li>SDK 34 and above, this method matches platform behavior.
     *     <li>SDK 33 and below, the object type is checked after deserialization.
     * </ul>
     *
     *
     * @param in The bundle to retrieve from.
     * @param key a String, or {@code null}
     * @param clazz The type of the object expected
     * @return a Parcelable value, or {@code null}
     */
    @Nullable
    @SuppressWarnings({"deprecation", "unchecked"})
    public static <T> T getParcelable(@NonNull Bundle in, @Nullable String key,
            @NonNull Class<T> clazz) {
        // Even though API was introduced in 33, we use 34 as 33 is bugged in some scenarios.
        if (Build.VERSION.SDK_INT >= 34) {
            return Api33Impl.getParcelable(in, key, clazz);
        } else {
            T parcelable = in.getParcelable(key);
            return clazz.isInstance(parcelable) ? parcelable : null;
        }
    }

    /**
     * Returns the value associated with the given key, or {@code null} if:
     * <ul>
     *     <li>No mapping of the desired type exists for the given key.
     *     <li>A {@code null} value is explicitly associated with the key.
     *     <li>The object is not of type {@code clazz}.
     * </ul>
     * <p>
     * <b>Note: </b> if the expected value is not a class provided by the Android platform, you
     * must call {@link Bundle#setClassLoader(ClassLoader)} with the proper {@link ClassLoader}
     * first. Otherwise, this method might throw an exception or return {@code null}.
     * <p>
     * Compatibility behavior:
     * <ul>
     *     <li>SDK 34 and above, this method matches platform behavior.
     *     <li>SDK 33 and below, this method will not check the array elements' types.
     * </ul>
     *
     * @param in The bundle to retrieve from.
     * @param key a String, or {@code null}
     * @param clazz The type of the items inside the array. This is only verified when unparceling.
     * @return a Parcelable[] value, or {@code null}
     */
    @Nullable
    @SuppressWarnings({"deprecation"})
    @SuppressLint({"ArrayReturn", "NullableCollection"})
    public static Parcelable[] getParcelableArray(@NonNull Bundle in, @Nullable String key,
            @NonNull Class<? extends Parcelable> clazz) {
        // Even though API was introduced in 33, we use 34 as 33 is bugged in some scenarios.
        if (Build.VERSION.SDK_INT >= 34) {
            return Api33Impl.getParcelableArray(in, key, clazz);
        } else {
            return in.getParcelableArray(key);
        }
    }

    /**
     * Returns the value associated with the given key, or {@code null} if:
     * <ul>
     *     <li>No mapping of the desired type exists for the given key.
     *     <li>A {@code null} value is explicitly associated with the key.
     *     <li>The object is not of type {@code clazz}.
     * </ul>
     * <p>
     * <b>Note: </b> if the expected value is not a class provided by the Android platform, you
     * must call {@link Bundle#setClassLoader(ClassLoader)} with the proper {@link ClassLoader}
     * first. Otherwise, this method might throw an exception or return {@code null}.
     * <p>
     * Compatibility behavior:
     * <ul>
     *     <li>SDK 34 and above, this method matches platform behavior.
     *     <li>SDK 33 and below, this method will not check the list elements' types.
     * </ul>
     *
     * @param in The bundle to retrieve from.
     * @param key a String, or {@code null}
     * @param clazz The type of the items inside the array list. This is only verified when
     *     unparceling.
     * @return an ArrayList<T> value, or {@code null}
     */
    @Nullable
    @SuppressWarnings({"deprecation", "unchecked"})
    @SuppressLint({"ConcreteCollection", "NullableCollection"})
    public static  <T> ArrayList<T> getParcelableArrayList(@NonNull Bundle in, @Nullable String key,
            @NonNull Class<? extends T> clazz) {
        // Even though API was introduced in 33, we use 34 as 33 is bugged in some scenarios.
        if (Build.VERSION.SDK_INT >= 34) {
            return Api33Impl.getParcelableArrayList(in, key, clazz);
        } else {
            return (ArrayList<T>) in.getParcelableArrayList(key);
        }
    }

    /**
     * Returns the value associated with the given key, or {@code null} if:
     * <ul>
     *     <li>No mapping of the desired type exists for the given key.
     *     <li>A {@code null} value is explicitly associated with the key.
     *     <li>The object is not of type {@code clazz}.
     * </ul>
     *
     * Compatibility behavior:
     * <ul>
     *     <li>SDK 34 and above, this method matches platform behavior.
     *     <li>SDK 33 and below, this method will not check the array elements' types.
     * </ul>
     *
     * @param in The bundle to retrieve from.
     * @param key a String, or null
     * @param clazz The type of the items inside the sparse array. This is only verified when
     *     unparceling.
     * @return a SparseArray of T values, or null
     */
    @SuppressWarnings({"deprecation", "unchecked"})
    @Nullable
    public static <T> SparseArray<T> getSparseParcelableArray(@NonNull Bundle in,
            @Nullable String key, @NonNull Class<? extends T> clazz) {
        if (Build.VERSION.SDK_INT >= 34) {
            return Api33Impl.getSparseParcelableArray(in, key, clazz);
        } else {
            return (SparseArray<T>) in.getSparseParcelableArray(key);
        }
    }

    /**
     * A convenience method to handle getting an {@link IBinder} inside a {@link Bundle} for all
     * Android versions.
     *
     * @param bundle The bundle to get the {@link IBinder}.
     * @param key The key to use while getting the {@link IBinder}.
     * @return The {@link IBinder} that was obtained.
     */
    @Nullable
    public static IBinder getBinder(@NonNull Bundle bundle, @Nullable String key) {
        return bundle.getBinder(key);
    }

    /**
     * A convenience method to handle putting an {@link IBinder} inside a {@link Bundle} for all
     * Android versions.
     *
     * @param bundle The bundle to insert the {@link IBinder}.
     * @param key The key to use while putting the {@link IBinder}.
     * @param binder The {@link IBinder} to put.
     */
    public static void putBinder(@NonNull Bundle bundle, @Nullable String key,
            @Nullable IBinder binder) {
        bundle.putBinder(key, binder);
    }

    /**
     * Returns the value associated with the given key or {@code null} if:
     * <ul>
     *     <li>No mapping of the desired type exists for the given key.
     *     <li>A {@code null} value is explicitly associated with the key.
     *     <li>The object is not of type {@code clazz}.
     * </ul>
     * Compatibility behavior:
     * <ul>
     *     <li>SDK 34 and above, this method matches platform behavior.
     *     <li>SDK 33 and below, the object type is checked after deserialization.
     * </ul>
     *
     *
     * @param in The bundle to retrieve from.
     * @param key a String, or {@code null}
     * @param clazz The type of the object expected
     * @return a Serializable value, or {@code null}
     */
    @SuppressWarnings({"deprecation", "unchecked"})
    @Nullable
    public static <T extends Serializable> T getSerializable(@NonNull Bundle in,
            @Nullable String key, @NonNull Class<T> clazz) {
        if (Build.VERSION.SDK_INT >= 34) {
            return Api33Impl.getSerializable(in, key, clazz);
        } else {
            Serializable serializable = in.getSerializable(key);
            return clazz.isInstance(serializable) ? (T) serializable : null;
        }
    }

    @RequiresApi(33)
    static class Api33Impl {
        private Api33Impl() {
            // This class is non-instantiable.
        }

        @DoNotInline
        static <T> T getParcelable(@NonNull Bundle in, @Nullable String key,
                @NonNull Class<T> clazz) {
            return in.getParcelable(key, clazz);
        }

        @DoNotInline
        static <T> T[] getParcelableArray(@NonNull Bundle in, @Nullable String key,
                @NonNull Class<T> clazz) {
            return in.getParcelableArray(key, clazz);
        }

        @DoNotInline
        static <T> ArrayList<T> getParcelableArrayList(@NonNull Bundle in, @Nullable String key,
                @NonNull Class<? extends T> clazz) {
            return in.getParcelableArrayList(key, clazz);
        }

        @DoNotInline
        static <T> SparseArray<T> getSparseParcelableArray(@NonNull Bundle in, @Nullable String key,
                @NonNull Class<? extends T> clazz) {
            return in.getSparseParcelableArray(key, clazz);
        }

        @DoNotInline
        static <T extends Serializable> T getSerializable(@NonNull Bundle in, @Nullable String key,
                @NonNull Class<T> clazz) {
            return in.getSerializable(key, clazz);
        }
    }
}
