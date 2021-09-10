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
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.BiFunction;

/**
 * Base class for holders of protobuf messages that can be parceled to be transferred to the rest of
 * the system.
 *
 * @hide
 */
@SuppressWarnings("AndroidApiChecker") // Uses java.util.function.Function
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ProtoParcelable implements Parcelable {
    private final byte[] mContents;
    private final int mVersion;

    static <T extends ProtoParcelable> Creator<T> newCreator(
            Class<T> clazz, BiFunction<byte[], Integer, T> creator) {
        return new Creator<T>() {

            @Override
            public T createFromParcel(Parcel source) {
                int version = source.readInt();
                byte[] payload = source.createByteArray();

                return creator.apply(payload, version);
            }

            @SuppressWarnings("unchecked")
            @Override
            public T[] newArray(int size) {
                return (T[]) Array.newInstance(clazz, size);
            }
        };
    }

    protected ProtoParcelable(@NonNull byte[] contents, int version) {
        this.mContents = contents;
        this.mVersion = version;
    }

    /** Get the payload contained within this ProtoParcelable. */
    @NonNull
    public byte[] getContents() {
        return mContents;
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
        return mVersion == that.mVersion && Arrays.equals(mContents, that.mContents);
    }

    @Override
    public int hashCode() {
        return 31 * mVersion + Arrays.hashCode(mContents);
    }
}
