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

package androidx.core.os;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;

import java.io.Serializable;
import java.util.List;

/**
 * Helper for accessing features in {@link Parcel}.
 */
public final class ParcelCompat {

    /**
     * Read a boolean value from the parcel at the current {@link Parcel#dataPosition()}.
     */
    public static boolean readBoolean(@NonNull Parcel in) {
        return in.readInt() != 0;
    }

    /**
     * Write a boolean value into the parcel at the current {@link Parcel#dataPosition()},
     * growing {@link Parcel#dataCapacity()} if needed.
     *
     * <p>Note: This method currently delegates to {@link Parcel#writeInt} with a value of 1 or 0
     * for true or false, respectively, but may change in the future.
     */
    public static void writeBoolean(@NonNull Parcel out, boolean value) {
        out.writeInt(value ? 1 : 0);
    }

    /**
     * Same as {@link Parcel#readParcelable(ClassLoader)} but accepts {@code clazz} parameter as
     * the type required for each item.
     *
     * @throws android.os.BadParcelableException Throws BadParcelableException if the item to be
     * deserialized is not an instance of that class or any of its children classes or there was
     * an error trying to instantiate an element.
     */
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @SuppressWarnings("deprecation")
    @Nullable
    public static <T extends Parcelable> T readParcelable(@NonNull Parcel in,
            @Nullable ClassLoader loader, @NonNull Class<T> clazz) {
        if (BuildCompat.isAtLeastT()) {
            return TiramisuImpl.readParcelable(in, loader, clazz);
        } else {
            return in.readParcelable(loader);
        }
    }

    /**
     * Same as {@link Parcel#readParcelableArray(ClassLoader)}  but accepts {@code clazz} parameter
     * as the type required for each item.
     *
     * @throws android.os.BadParcelableException Throws BadParcelableException if the item to be
     * deserialized is not an instance of that class or any of its children classes or there was
     * an error trying to instantiate an element.
     */
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @SuppressWarnings("unchecked")
    @SuppressLint({"ArrayReturn", "NullableCollection"})
    @Nullable
    public static <T> T[] readParcelableArray(@NonNull Parcel in, @Nullable ClassLoader loader,
            @NonNull Class<T> clazz) {
        if (BuildCompat.isAtLeastT()) {
            return TiramisuImpl.readParcelableArray(in, loader, clazz);
        } else {
            return (T[]) in.readParcelableArray(loader);
        }
    }

    /**
     * Same as {@link Parcel#readParcelableList(List, ClassLoader)} but accepts {@code clazz}
     * parameter as the type required for each item.
     *
     * @throws android.os.BadParcelableException Throws BadParcelableException if the item to be
     * deserialized is not an instance of that class or any of its children classes or there was
     * an error trying to instantiate an element.
     */
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @NonNull
    @SuppressWarnings({"deprecation", "unchecked"})
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static <T> List<T> readParcelableList(@NonNull Parcel in, @NonNull List<T> list,
            @Nullable ClassLoader cl, @NonNull Class<T> clazz) {
        if (BuildCompat.isAtLeastT()) {
            return TiramisuImpl.readParcelableList(in, list, cl, clazz);
        } else {
            return Api29Impl.readParcelableList(in, (List) list, cl);
        }
    }

    /**
     * Same as {@link Parcel#readSerializable()} but accepts {@code loader} parameter
     * as the primary classLoader for resolving the Serializable class; and {@code clazz} parameter
     * as the required type.
     *
     * @throws android.os.BadParcelableException Throws BadParcelableException if the item to be
     * deserialized is not an instance of that class or any of its children class or there there
     * was an error deserializing the object.
     */
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @SuppressWarnings({"deprecation", "unchecked"})
    @Nullable
    public static <T extends Serializable> T readSerializable(@NonNull Parcel in,
            @Nullable ClassLoader loader, @NonNull Class<T> clazz) {
        if (BuildCompat.isAtLeastT()) {
            return TiramisuImpl.readSerializable(in, loader, clazz);
        } else {
            return (T) in.readSerializable();
        }
    }

    private ParcelCompat() {}

    @RequiresApi(29)
    static class Api29Impl {
        private Api29Impl() {
            // This class is non-instantiable.
        }

        @DoNotInline
        static final <T extends Parcelable> List<T> readParcelableList(@NonNull Parcel in,
                @NonNull List<T> list, @Nullable ClassLoader cl) {
            return in.readParcelableList(list, cl);
        }
    }

    @RequiresApi(33)
    static class TiramisuImpl {
        private TiramisuImpl() {
            // This class is non-instantiable.
        }

        @DoNotInline
        static <T extends Serializable> T readSerializable(@NonNull Parcel in,
                @Nullable ClassLoader loader, @NonNull Class<T> clazz) {
            return in.readSerializable(loader, clazz);
        }

        @DoNotInline
        static <T extends Parcelable> T readParcelable(@NonNull Parcel in,
                @Nullable ClassLoader loader, @NonNull Class<T> clazz) {
            return in.readParcelable(loader, clazz);
        }

        @DoNotInline
        static <T> T[] readParcelableArray(@NonNull Parcel in, @Nullable ClassLoader loader,
                @NonNull Class<T> clazz) {
            return in.readParcelableArray(loader, clazz);
        }

        @DoNotInline
        static <T> List<T> readParcelableList(@NonNull Parcel in, @NonNull List<T> list,
                @Nullable ClassLoader cl, @NonNull Class<T> clazz) {
            return in.readParcelableList(list, cl, clazz);
        }
    }
}
