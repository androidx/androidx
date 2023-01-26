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
import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;

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
     *
     * <p><b>Note: </b> if the expected value is not a class provided by the Android platform,
     * you must call {@link Bundle#setClassLoader(ClassLoader)} with the proper {@link ClassLoader}
     * first.
     * Otherwise, this method might throw an exception or return {@code null}.
     *
     * Compatibility behavior:
     * <ul>
     *     <li>{@link BuildCompat#isAtLeastU() Android U and later}, this method matches platform
     *     behavior.
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
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @SuppressWarnings({"deprecation", "unchecked"})
    public static <T> T getParcelable(@NonNull Bundle in, @Nullable String key,
            @NonNull Class<T> clazz) {
        if (BuildCompat.isAtLeastU()) {
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
     *
     * <p><b>Note: </b> if the expected value is not a class provided by the Android platform,
     * you must call {@link Bundle#setClassLoader(ClassLoader)} with the proper {@link ClassLoader}
     * first.
     * Otherwise, this method might throw an exception or return {@code null}.
     *
     * Compatibility behavior:
     * <ul>
     *     <li>{@link BuildCompat#isAtLeastU() Android U and later}, this method matches platform
     *     behavior.
     *     <li>SDK 33 and below, this method will not check the array elements' types.
     * </ul>
     *
     * @param in The bundle to retrieve from.
     * @param key a String, or {@code null}
     * @param clazz The type of the items inside the array. This is only verified when unparceling.
     * @return a Parcelable[] value, or {@code null}
     */
    @Nullable
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @SuppressWarnings({"deprecation"})
    @SuppressLint({"ArrayReturn", "NullableCollection"})
    public static Parcelable[] getParcelableArray(@NonNull Bundle in, @Nullable String key,
            @NonNull Class<? extends Parcelable> clazz) {
        if (BuildCompat.isAtLeastU()) {
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
     *
     * <p><b>Note: </b> if the expected value is not a class provided by the Android platform,
     * you must call {@link Bundle#setClassLoader(ClassLoader)} with the proper {@link ClassLoader}
     * first.
     * Otherwise, this method might throw an exception or return {@code null}.
     *
     * Compatibility behavior:
     * <ul>
     *     <li>{@link BuildCompat#isAtLeastU() Android U and later}, this method matches platform
     *     behavior.
     *     <li>SDK 33 and below, this method will not check the list elements' types.
     * </ul>
     *
     * @param in The bundle to retrieve from.
     * @param key   a String, or {@code null}
     * @param clazz The type of the items inside the array list. This is only verified when
     *     unparceling.
     * @return an ArrayList<T> value, or {@code null}
     */
    @Nullable
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @SuppressWarnings({"deprecation", "unchecked"})
    @SuppressLint({"ConcreteCollection", "NullableCollection"})
    public static  <T> ArrayList<T> getParcelableArrayList(@NonNull Bundle in, @Nullable String key,
            @NonNull Class<? extends T> clazz) {
        if (BuildCompat.isAtLeastU()) {
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
     *     <li>{@link BuildCompat#isAtLeastU() Android U and later}, this method matches platform
     *     behavior.
     *     <li>SDK 33 and below, this method will not check the array elements' types.
     * </ul>
     *
     * @param in The bundle to retrieve from.
     * @param key a String, or null
     * @param clazz The type of the items inside the sparse array. This is only verified when
     *     unparceling.
     * @return a SparseArray of T values, or null
     */
    @Nullable
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @SuppressWarnings({"deprecation", "unchecked"})
    public static <T> SparseArray<T> getSparseParcelableArray(@NonNull Bundle in,
            @Nullable String key, @NonNull Class<? extends T> clazz) {
        if (BuildCompat.isAtLeastU()) {
            return Api33Impl.getSparseParcelableArray(in, key, clazz);
        } else {
            return (SparseArray<T>) in.getSparseParcelableArray(key);
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
    }
}
