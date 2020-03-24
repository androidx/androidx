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

package androidx.versionedparcelable;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for managing {@link VersionedParcelable}s.
 */
public class ParcelUtils {

    private static final String INNER_BUNDLE_KEY = "a";

    private ParcelUtils() { }

    /**
     * Turn a VersionedParcelable into a Parcelable
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static Parcelable toParcelable(VersionedParcelable obj) {
        return new ParcelImpl(obj);
    }

    /**
     * Turn a Parcelable into a VersionedParcelable.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @SuppressWarnings("TypeParameterUnusedInFormals")
    public static <T extends VersionedParcelable> T fromParcelable(Parcelable p) {
        if (!(p instanceof ParcelImpl)) {
            throw new IllegalArgumentException("Invalid parcel");
        }
        return ((ParcelImpl) p).getVersionedParcel();
    }

    /**
     * Write a VersionedParcelable into an OutputStream.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static void toOutputStream(VersionedParcelable obj, OutputStream output) {
        VersionedParcelStream stream = new VersionedParcelStream(null, output);
        stream.writeVersionedParcelable(obj);
        stream.closeField();
    }

    /**
     * Read a VersionedParcelable from an InputStream.
     * @hide
     */
    @SuppressWarnings("TypeParameterUnusedInFormals")
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static <T extends VersionedParcelable> T fromInputStream(InputStream input) {
        VersionedParcelStream stream = new VersionedParcelStream(input, null);
        return stream.readVersionedParcelable();
    }

    /**
     * Add a VersionedParcelable to an existing Bundle.
     */
    public static void putVersionedParcelable(@NonNull Bundle b, @NonNull String key,
            @Nullable VersionedParcelable obj) {
        if (obj == null) {
            return;
        }
        Bundle innerBundle = new Bundle();
        innerBundle.putParcelable(INNER_BUNDLE_KEY, toParcelable(obj));
        b.putParcelable(key, innerBundle);
    }

    /**
     * Get a VersionedParcelable from a Bundle.
     *
     * Returns null if the bundle isn't present or ClassLoader issues occur.
     */
    @SuppressWarnings("TypeParameterUnusedInFormals")
    @Nullable
    public static <T extends VersionedParcelable> T getVersionedParcelable(
            @NonNull Bundle bundle, @NonNull String key) {
        try {
            Bundle innerBundle = bundle.getParcelable(key);
            if (innerBundle == null) {
                return null;
            }
            innerBundle.setClassLoader(ParcelUtils.class.getClassLoader());
            return fromParcelable(innerBundle.getParcelable(INNER_BUNDLE_KEY));
        } catch (RuntimeException e) {
            // There may be new classes or such in the bundle, make sure not to crash the caller.
            return null;
        }
    }

    /**
     * Add a list of VersionedParcelable to an existing Bundle.
     */
    public static void putVersionedParcelableList(@NonNull Bundle b, @NonNull String key,
            @NonNull List<? extends VersionedParcelable> list) {
        Bundle innerBundle = new Bundle();
        ArrayList<Parcelable> toWrite = new ArrayList<>();
        for (VersionedParcelable obj : list) {
            toWrite.add(toParcelable(obj));
        }
        innerBundle.putParcelableArrayList(INNER_BUNDLE_KEY, toWrite);
        b.putParcelable(key, innerBundle);
    }

    /**
     * Get a list of VersionedParcelable from a Bundle.
     *
     * Returns null if the bundle isn't present or ClassLoader issues occur.
     */
    @SuppressWarnings({"TypeParameterUnusedInFormals","unchecked"})
    @Nullable
    public static <T extends VersionedParcelable> List<T> getVersionedParcelableList(
            Bundle bundle, String key) {
        List<T> resultList = new ArrayList<>();
        try {
            Bundle innerBundle = bundle.getParcelable(key);
            innerBundle.setClassLoader(ParcelUtils.class.getClassLoader());
            ArrayList<Parcelable> parcelableArrayList =
                    innerBundle.getParcelableArrayList(INNER_BUNDLE_KEY);
            for (Parcelable parcelable : parcelableArrayList) {
                resultList.add((T) fromParcelable(parcelable));
            }
            return resultList;
        } catch (RuntimeException e) {
            // There may be new classes or such in the bundle, make sure not to crash the caller.
        }
        return null;
    }
}
