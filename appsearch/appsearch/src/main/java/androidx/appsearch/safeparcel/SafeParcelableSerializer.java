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

package androidx.appsearch.safeparcel;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper functions for safely serializing a {@link SafeParcelable} object to/from byte arrays or
 * Strings.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SafeParcelableSerializer {

    /**
     * Serializes {@link SafeParcelable}s to a bundle. The bundle can be deserialized using {@link
     * #deserializeIterableFromBundleSafe(Bundle, String, android.os.Parcelable.Creator)}, and will
     * use SafeParcelable semantics if deserializing to an older or newer revision of the class.
     *
     * <p>Note: {@link Bundle#putParcelableArrayList(String, java.util.ArrayList)} does not preserve
     * SafeParcelable semantics. Use this method instead.
     *
     * @param safeParcelables SafeParcelables to serialize.
     * @param bundle the bundle to serialize into.
     * @param key the name of the key to save to.
     * @param <T> The type of the SafeParcelable.
     */
    public static <T extends Parcelable & SafeParcelable> void serializeIterableToBundleSafe(
            @NonNull Bundle bundle, @NonNull String key, @NonNull Iterable<T> safeParcelables) {
        final byte[] safeParcelablesBytes = serializeIterableToBytes(safeParcelables);
        bundle.putByteArray(key, safeParcelablesBytes);
    }

    /**
     * Deserializes {@link SafeParcelable}s from a bundle created by {@link
     * #serializeIterableToBundleSafe(android.os.Bundle, String, Iterable)}.
     *
     * @param bundle the bundle containing the key to deserialize from.
     * @param key the name of the key mapping to the serialized object.
     * @param safeParcelableCreator The CREATOR of the SafeParcelable to deserialize.
     * @param <T> The type of the SafeParcelable.
     * @return The deserialized SafeParcelables, or null if the extra was missing or the class could
     *     not be deserialized.
     */
    @Nullable
    public static <T extends SafeParcelable> List<T> deserializeIterableFromBundleSafe(
            @NonNull Bundle bundle, @NonNull String key,
            @NonNull Parcelable.Creator<T> safeParcelableCreator) {
        final byte[] serializedBytes = bundle.getByteArray(key);
        return deserializeIterableFromBytes(serializedBytes, safeParcelableCreator);
    }

    private static <T extends Parcelable & SafeParcelable> byte[] serializeIterableToBytes(
            Iterable<T> safeParcelables) {
        final Parcel parcel = Parcel.obtain();
        try {
            parcel.writeTypedList(createListFromIterable(safeParcelables));
            return parcel.marshall();
        } finally {
            parcel.recycle();
        }
    }

    @NonNull
    private static <T extends SafeParcelable> List<T> createListFromIterable(
            @NonNull Iterable<T> iterable) {
        List<T> list = new ArrayList<>();
        for (T element : iterable) {
            list.add(element);
        }
        return list;
    }

    @Nullable
    private static <T extends SafeParcelable> List<T> deserializeIterableFromBytes(
            @Nullable byte[] serializedBytes, Parcelable.Creator<T> safeParcelableCreator) {
        if (serializedBytes == null) {
            return null;
        }
        final Parcel parcel = Parcel.obtain();
        parcel.unmarshall(serializedBytes, 0, serializedBytes.length);
        parcel.setDataPosition(0);
        try {
            final ArrayList<T> safeParcelables = new ArrayList<>();
            parcel.readTypedList(safeParcelables, safeParcelableCreator);
            return safeParcelables;
        } finally {
            parcel.recycle();
        }
    }

    private SafeParcelableSerializer() {}
}
