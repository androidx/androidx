/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.car.navigation.utils;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Class responsible for serializing and deserializing data into a {@link Bundle}. It also
 * provides a way to detect what items in the {@link Bundle} have been modified during
 * marshalling.
 * <p>
 * A single {@link BundleMarshaller} can be re-used to serialize or deserialize data multiple times.
 * Similarity, deserialization can be done in-place, updating existing {@link Bundlable}s. This
 * reduces the number of instances being allocated.
 * <p>
 * When serializing, use {@link #resetBundle()} before marshalling and {@link #getBundle()} to
 * obtain an snap-shot of the serialized content. Or use {@link #resetDelta()} and
 * {@link #getDelta()} to obtain a {@link Bundle} representing the patch between the last and
 * the new serialized data.
 * <p>
 * When deserializing, use {@link #setBundle(Bundle)} to deserialize a {@link Bundle} containing an
 * snap-shot, or {@link #applyDelta(Bundle)} to process a patch from the last deserialized data.
 * <p>
 * Keys used in the "get" and "put" methods must be lower camel case alphanumerical identifiers
 * (e.g.: "distanceUnit"). Symbols like "." and "_" are reserved by the system.
 * <p>
 * When deserializing {@link List} objects, this class assumes that they implement random access
 * (e.g. {@link ArrayList}), or they are relatively small (see more details at
 * {@link #trimList(List, int)})
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class BundleMarshaller {
    /**
     * Separator used to concatenate identifiers when marshalling non-primitive types (e.g. lists
     * or {@link Bundlable}s).
     */
    private static final String KEY_SEPARATOR = ".";
    /**
     * Identifier used to record if a given non-primitive field is null or not. This allows
     * serializing null objects without the need of using reflection or static methods.
     */
    private static final String IS_NULL_KEY = "_isNull";
    /**
     * Identifier used to record the length of a collection. This allows serializing changes to the
     * length of a collection without having to remove elements or having to iterate over every
     * possible collection key.
     */
    private static final String SIZE_KEY = "_size";
    /**
     * Special value for {@link #SIZE_KEY} to serialize a null collection.
     */
    private static final int NULL_SIZE = -1;

    private Bundle mBundle = new Bundle();
    private String mKeyPrefix = "";
    private final Bundle mBundleDelta = new Bundle();

    /**
     * Returns data serialized since the last time this instance was constructed, or
     * {@link #resetBundle()} was called.
     */
    public Bundle getBundle() {
        return mBundle;
    }

    /**
     * Resets this {@link BundleMarshaller} causing {@link #getBundle()} to return an empty
     * {@link Bundle} until the next marshalling is executed. This can be used occasionally to
     * remove unused keys in the {@link Bundle}.
     */
    public void resetBundle() {
        mBundle.clear();
    }

    /**
     * Replaces the {@link Bundle} to serialize into or deserialize from.
     */
    public void setBundle(Bundle bundle) {
        mBundle = bundle;
    }

    /**
     * Gets a {@link Bundle} containing only the entries of {@link #getBundle()} that were modified
     * since this instance was constructed, or {@link #resetDelta()} was called.
     */
    public Bundle getDelta() {
        return mBundleDelta;
    }

    /**
     * Merges the provided {@link Bundle} on top of the one stored in this {@link BundleMarshaller}.
     *
     * @param delta a {@link Bundle} containing entries to be updated on one stored in this
     *              {@link BundleMarshaller} instance. Such {@link Bundle} can be produced by
     *              using the {@link #resetDelta()} and {@link #getDelta()} methods during data
     *              serialization.
     */
    public void applyDelta(Bundle delta) {
        mBundle.putAll(delta);
    }

    /**
     * Resets tracking of modified entries, causing {@link #getDelta()} to return an empty
     * {@link Bundle} until the next marshalling is executed. This can be used between
     * serializations make {@link #getDelta()} return only the differences.
     */
    public void resetDelta() {
        mBundleDelta.clear();
    }

    /**
     * Inserts an int value, replacing any existing value for the given key.
     *
     * @param key lower camel case alphanumerical identifier
     * @param value an int
     */
    public void putInt(@NonNull String key, int value) {
        String mangledKey = getMangledKey(key);
        if (!mBundle.containsKey(mangledKey) || mBundle.getInt(mangledKey) != value) {
            mBundleDelta.putInt(mangledKey, value);
            mBundle.putInt(mangledKey, value);
        }
    }

    /**
     * Returns the value associated with the given key, or 0 if no mapping of the desired type
     * exists for the given key.
     *
     * @param key lower camel case alphanumerical identifier
     * @return an int
     */
    public int getInt(@NonNull String key) {
        return mBundle.getInt(getMangledKey(key));
    }

    /**
     * Inserts a float value, replacing any existing value for the given key.
     *
     * @param key lower camel case alphanumerical identifier
     * @param value a float
     */
    public void putFloat(@NonNull String key, float value) {
        String mangledKey = getMangledKey(key);
        if (!mBundle.containsKey(mangledKey)
                || Float.compare(mBundle.getFloat(mangledKey), value) != 0) {
            mBundleDelta.putFloat(mangledKey, value);
            mBundle.putFloat(mangledKey, value);
        }
    }

    /**
     * Returns the value associated with the given key, or 0.0f if no mapping of the desired type
     * exists for the given key.
     *
     * @param key lower camel case alphanumerical identifier
     * @return a float
     */
    public float getFloat(@NonNull String key) {
        return mBundle.getFloat(getMangledKey(key));
    }

    /**
     * Inserts a double value, replacing any existing value for the given key.
     *
     * @param key lower camel case alphanumerical identifier
     * @param value a double
     */
    public void putDouble(@NonNull String key, double value) {
        String mangledKey = getMangledKey(key);
        if (!mBundle.containsKey(mangledKey)
                || Double.compare(mBundle.getDouble(mangledKey), value) != 0) {
            mBundleDelta.putDouble(mangledKey, value);
            mBundle.putDouble(mangledKey, value);
        }
    }

    /**
     * Returns the value associated with the given key, or 0.0 if no mapping of the desired type
     * exists for the given key.
     *
     * @param key lower camel case alphanumerical identifier
     * @return a double
     */
    public double getDouble(@NonNull String key) {
        return mBundle.getDouble(getMangledKey(key));
    }

    /**
     * Inserts a boolean value, replacing any existing value for the given key.
     *
     * @param key lower camel case alphanumerical identifier
     * @param value a boolean
     */
    public void putBoolean(@NonNull String key, boolean value) {
        String mangledKey = getMangledKey(key);
        if (!mBundle.containsKey(mangledKey) || mBundle.getBoolean(mangledKey) != value) {
            mBundleDelta.putBoolean(mangledKey, value);
            mBundle.putBoolean(mangledKey, value);
        }
    }

    /**
     * Returns the value associated with the given key, or false if no mapping of the desired type
     * exists for the given key.
     *
     * @param key lower camel case alphanumerical identifier
     * @return a boolean
     */
    public boolean getBoolean(@NonNull String key) {
        return mBundle.getBoolean(getMangledKey(key));
    }

    /**
     * Inserts a string value, replacing any existing value for the given key.
     *
     * @param key lower camel case alphanumerical identifier
     * @param value a string, or null
     */
    public void putString(@NonNull String key, @Nullable String value) {
        String mangledKey = getMangledKey(key);
        if (!mBundle.containsKey(mangledKey)
                || !Objects.equals(mBundle.getString(mangledKey), value)) {
            mBundleDelta.putString(mangledKey, value);
            mBundle.putString(mangledKey, value);
        }
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of the desired type
     * exists for the given key.
     *
     * @param key lower camel case alphanumerical identifier
     * @return a string, or null
     */
    @Nullable
    public String getString(@NonNull String key) {
        return mBundle.getString(getMangledKey(key));
    }

    /**
     * Returns the value associated with the given key, or the provided default value if no mapping
     * of the desired type exists for the given key.
     *
     * @param key lower camel case alphanumerical identifier
     * @param defaultValue value to return if key does not exist or if a null value is associated
     *                     with the given key.
     * @return a string
     */
    @NonNull
    public String getStringNonNull(@NonNull String key, @NonNull String defaultValue) {
        return mBundle.getString(getMangledKey(key), defaultValue);
    }

    /**
     * Inserts an enum value, replacing any existing value for the given key. The provided enum
     * will be serialized as a string using {@link Enum#name()}.
     *
     * @param key lower camel case alphanumerical identifier
     * @param value an enum, or null
     */
    public <T extends Enum<T>> void putEnum(@NonNull String key, @Nullable T value) {
        putString(key, value != null ? value.name() : null);
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of the desired type
     * exists for the given key.
     *
     * @param key lower camel case alphanumerical identifier
     * @param clazz {@link Enum} class to be used to deserialize the value.
     * @param <T> {@link Enum} type to be returned.
     * @return an enum, or null
     */
    @Nullable
    public <T extends Enum<T>> T getEnum(@NonNull String key, @NonNull Class<T> clazz) {
        String name = getString(key);
        try {
            return name != null ? Enum.valueOf(clazz, name) : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or the provided default value if no mapping
     * of the desired type exists for the given key.
     *
     * @param key lower camel case alphanumerical identifier
     * @param clazz {@link Enum} class to be used to deserialize the value.
     * @param defaultValue value to return if key does not exist or if a null value is associated
     *                     with the given key.
     * @param <T> {@link Enum} type to be returned.
     * @return an enum
     */
    @NonNull
    public <T extends Enum<T>> T getEnumNonNull(@NonNull String key, @NonNull Class<T> clazz,
            @NonNull T defaultValue) {
        T result = getEnum(key, clazz);
        return result != null ? result : defaultValue;
    }

    /**
     * Inserts a {@link Bundlable} value, replacing any existing value for the given key.
     *
     * @param key lower camel case alphanumerical identifier
     * @param value a {@link Bundlable}, or null
     */
    public <T extends Bundlable> void putBundlable(@NonNull String key, @Nullable T value) {
        withKeyPrefix(key, () -> {
            putBoolean(IS_NULL_KEY, value == null);
            if (value != null) {
                value.toBundle(this);
            }
        });
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of the desired type
     * exists for the given key. If a non-null "current" instance is provided, then the
     * deserialization would be done in place. Otherwise, a new instance will be created using the
     * provided factory.
     *
     * @param key lower camel case alphanumerical identifier
     * @param current current value (if available) to perform in-place deserialization, or null
     * @param factory a {@link Supplier} capable of providing an instance of a {@link Bundlable} of
     *                type T. The suggested implementation is to pass a reference to the default
     *                constructor of that class.
     * @param <T> {@link Bundlable} type to be returned.
     * @return an instance of type T, or null
     */
    @Nullable
    public <T extends Bundlable> T getBundlable(@NonNull String key, @Nullable T current,
            @NonNull Supplier<T> factory) {
        return withKeyPrefix(key, () -> {
            if (getBoolean(IS_NULL_KEY)) {
                return null;
            }
            T result = current != null ? current : factory.get();
            result.fromBundle(this);
            return result;
        });
    }

    /**
     * Returns the value associated with the given key, or a default value if no mapping of the
     * desired type exists for the given key. If a non-null value is available, then such value
     * will be deserialized in-place on the given "current" instance. Otherwise, a default value
     * will be generated using the provided factory.
     *
     * @param key lower camel case alphanumerical identifier
     * @param current current value to perform in-place deserialization
     * @param factory a {@link Supplier} capable of providing an instance of a {@link Bundlable} of
     *                type T. The suggested implementation is to pass a reference to the default
     *                constructor of that class.
     * @param <T> {@link Bundlable} type to be returned.
     * @return an instance of type T
     */
    @NonNull
    public <T extends Bundlable> T getBundlableNonNull(@NonNull String key, @NonNull T current,
            @NonNull Supplier<T> factory) {
        T result = getBundlable(key, current, factory);
        return result != null ? result : factory.get();
    }

    /**
     * Inserts a {@link List} of {@link Bundlable} values, replacing any existing value for the
     * given key.
     *
     * @param key lower camel case alphanumerical identifier
     * @param values a {@link List} of {@link Bundlable} values, or null
     */
    public <T extends Bundlable> void putBundlableList(@NonNull String key,
            @Nullable List<T> values) {
        withKeyPrefix(key, () -> {
            putInt(SIZE_KEY, values != null ? values.size() : NULL_SIZE);
            if (values != null) {
                int pos = 0;
                // Using for-each as the provided list might not implement random access (e.g. it
                // might be a linked list).
                for (T value : values) {
                    putBundlable(String.valueOf(pos), value);
                    pos++;
                }
            }
        });
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of the desired type
     * exists for the given key. If a non-null "current" list is provided, then the deserialization
     * would be done in place. Otherwise, a new list will be created and items will be instantiated
     * using the provided factory.
     *
     * @param key lower camel case alphanumerical identifier
     * @param current current value (if available) to perform in-place deserialization, or null
     * @param factory a {@link Supplier} capable of providing an instance of a {@link Bundlable} of
     *                type T. The suggested implementation is to pass a reference to the default
     *                constructor of that class.
     * @param <T> {@link Bundlable} type to be returned.
     * @return a list of instances of type T, or null. The resulting list might contain null
     *         elements.
     */
    @Nullable
    public <T extends Bundlable> List<T> getBundlableList(@NonNull String key,
            @Nullable List<T> current, @NonNull Supplier<T> factory) {
        return withKeyPrefix(key, () -> {
            int listSize = getInt(SIZE_KEY);
            if (listSize == NULL_SIZE) {
                return null;
            }
            List<T> result = current != null ? current : new ArrayList<>(listSize);
            if (result.size() > listSize) {
                result.subList(listSize, result.size()).clear();
            }
            for (int pos = 0; pos < listSize; pos++) {
                String subKey = String.valueOf(pos);
                if (pos < result.size()) {
                    result.set(pos, getBundlable(subKey, result.get(pos), factory));
                } else {
                    result.add(getBundlable(String.valueOf(pos),
                            null /* force the creation of a new instance */,
                            factory));
                }
            }
            return result;
        });
    }

    /**
     * Returns the value associated with the given key, or an empty list if no mapping of the
     * desired type exists for the given key. If a non-null "current" list is provided, then the
     * deserialization would be done in place. Otherwise, a new list will be created and items will
     * be instantiated using the provided factory.
     *
     * @param key lower camel case alphanumerical identifier
     * @param current current value (if available) to perform in-place deserialization, or null
     * @param factory a {@link Supplier} capable of providing an instance of a {@link Bundlable} of
     *                type T. The suggested implementation is to pass a reference to the default
     *                constructor of that class.
     * @param <T> {@link Bundlable} type to be returned.
     * @return a list of instances of type T, or an empty list. The resulting list might contain
     *         null elements.
     */
    @NonNull
    public <T extends Bundlable> List<T> getBundlableListNonNull(@NonNull String key,
            @NonNull List<T> current, @NonNull Supplier<T> factory) {
        List<T> result = getBundlableList(key, current, factory);
        return result != null ? result : new ArrayList<>();
    }

    /**
     * Executes the given {@link Runnable} in a context where {@link #getMangledKey(String)}
     * includes the given key as part of the prefix. Calls to this method can be nested (the
     * provided {@link Runnable} can call to this method if needed). This method should be used when
     * serializing or deserializing nested objects.
     * <p>
     * For example: calling to {@link #withKeyPrefix(String, Runnable)} with "foo" as key and
     * a {@link Runnable} that calls {@link #getMangledKey(String)} with "bar" as key, will
     * cause such {@link #getMangledKey(String)} call to return "foo.bar".
     */
    private void withKeyPrefix(@NonNull String key, @NonNull Runnable runnable) {
        String originalKeyPrefix = mKeyPrefix;
        mKeyPrefix = mKeyPrefix + key + KEY_SEPARATOR;
        runnable.run();
        mKeyPrefix = originalKeyPrefix;
    }

    /**
     * Similar to {@link #withKeyPrefix(String, Runnable)} but allows returning a value.
     */
    private <X> X withKeyPrefix(@NonNull String key, @NonNull Supplier<X> supplier) {
        String originalKeyPrefix = mKeyPrefix;
        mKeyPrefix = mKeyPrefix + key + KEY_SEPARATOR;
        X res = supplier.get();
        mKeyPrefix = originalKeyPrefix;
        return res;
    }

    /**
     * Returns a composed key based on the given one and the current serialization/deserialization
     * key prefix (initially empty). This prefix can be temporarily changed with
     * {@link #withKeyPrefix(String, Runnable)} or {@link #withKeyPrefix(String, Supplier)}.
     */
    private String getMangledKey(@NonNull String key) {
        return mKeyPrefix + key;
    }
}
