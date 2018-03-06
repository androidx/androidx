/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.support.v4.os;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Helper for accessing features in {@link android.os.Parcelable}.
 *
 * @deprecated Use {@link android.os.Parcelable.ClassLoaderCreator} directly.
 */
@Deprecated
public final class ParcelableCompat {

    /**
     * Factory method for {@link Parcelable.Creator}.
     *
     * @param callbacks Creator callbacks implementation.
     * @return New creator.
     *
     * @deprecated Use {@link android.os.Parcelable.ClassLoaderCreator} directly.
     */
    @Deprecated
    public static <T> Parcelable.Creator<T> newCreator(
            ParcelableCompatCreatorCallbacks<T> callbacks) {
        return new ParcelableCompatCreatorHoneycombMR2<T>(callbacks);
    }

    static class ParcelableCompatCreatorHoneycombMR2<T>
            implements Parcelable.ClassLoaderCreator<T> {
        private final ParcelableCompatCreatorCallbacks<T> mCallbacks;

        ParcelableCompatCreatorHoneycombMR2(ParcelableCompatCreatorCallbacks<T> callbacks) {
            mCallbacks = callbacks;
        }

        @Override
        public T createFromParcel(Parcel in) {
            return mCallbacks.createFromParcel(in, null);
        }

        @Override
        public T createFromParcel(Parcel in, ClassLoader loader) {
            return mCallbacks.createFromParcel(in, loader);
        }

        @Override
        public T[] newArray(int size) {
            return mCallbacks.newArray(size);
        }
    }

    private ParcelableCompat() {}
}
