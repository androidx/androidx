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

package androidx.work.multiprocess.parcelable;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ParcelConverters {
    private ParcelConverters() {
        // Does nothing
    }

    /**
     * Marshalls a {@link Parcelable}.
     */
    @NonNull
    public static byte[] marshall(@NonNull Parcelable parcelable) {
        Parcel parcel = Parcel.obtain();
        try {
            parcelable.writeToParcel(parcel, 0 /* flags */);
            return parcel.marshall();
        } finally {
            parcel.recycle();
        }
    }

    /**
     * Unmarshalls a {@code byte[]} to the {@link T} given a {@link android.os.Parcelable.Creator}.
     */
    @NonNull
    public static <T> T unmarshall(
            @NonNull byte[] array,
            @NonNull Parcelable.Creator<T> creator) {

        Parcel parcel = Parcel.obtain();
        try {
            parcel.unmarshall(array, 0, array.length);
            parcel.setDataPosition(0); // reset
            return creator.createFromParcel(parcel);
        } finally {
            parcel.recycle();
        }
    }
}
