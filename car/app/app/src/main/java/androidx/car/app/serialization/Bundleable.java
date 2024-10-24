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

package androidx.car.app.serialization;

import static java.util.Objects.requireNonNull;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.jspecify.annotations.NonNull;

/**
 * A class that serializes and stores an object for sending over IPC.
 */
@SuppressWarnings("BanParcelableUsage")
public final class Bundleable implements Parcelable {
    private final Bundle mBundle;

    /**
     * Serializes the {@code objectToSerialize} into a {@link Bundleable} to send over IPC.
     *
     * @throws BundlerException if serialization fails
     */
    public static @NonNull Bundleable create(@NonNull Object objectToSerialize)
            throws BundlerException {
        return new Bundleable(objectToSerialize);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * De-serializes the object passed in for IPC communication.
     *
     * @throws BundlerException if deserialization fails
     */
    public @NonNull Object get() throws BundlerException {
        return Bundler.fromBundle(mBundle);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeBundle(mBundle);
    }

    private Bundleable(Object o) throws BundlerException {
        mBundle = Bundler.toBundle(o);
    }

    Bundleable(Bundle bundle) {
        mBundle = bundle;
    }

    public static final @NonNull Creator<Bundleable> CREATOR =
            new Creator<Bundleable>() {
                @Override
                public Bundleable createFromParcel(final Parcel source) {
                    // To work around a lint warning that indicates the default class loader will
                    // not work for restoring our own classes.
                    return new Bundleable(
                            requireNonNull(source.readBundle(getClass().getClassLoader())));
                }

                @Override
                public Bundleable[] newArray(final int size) {
                    return new Bundleable[size];
                }
            };
}
