/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.common;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelImpl;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MediaParcelUtils {
    public static final String TAG = "MediaParcelUtils";

    /**
     * Media2 version of {@link ParcelUtils#toParcelable(VersionedParcelable)}.
     * <p>
     * This sanitizes {@link MediaItem}'s subclass information.
     *
     * @param item
     * @return
     */
    @NonNull
    public static ParcelImpl toParcelable(@Nullable VersionedParcelable item) {
        if (item instanceof MediaItem) {
            return new MediaItemParcelImpl((MediaItem) item);
        }
        return (ParcelImpl) ParcelUtils.toParcelable(item);
    }

    /**
     * Helper method for converting a list of VersionedParcelable items into ParcelImpl items.
     *
     * @param items
     * @return
     */
    @NonNull
    public static List<ParcelImpl> toParcelableList(
            @NonNull List<? extends VersionedParcelable> items) {
        List<ParcelImpl> list = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            list.add(toParcelable(items.get(i)));
        }
        return list;
    }

    /**
     * Media2 version of {@link ParcelUtils#fromParcelable(Parcelable)}.
     */
    @SuppressWarnings("TypeParameterUnusedInFormals")
    @Nullable
    public static <T extends VersionedParcelable> T fromParcelable(@NonNull ParcelImpl p) {
        return ParcelUtils.fromParcelable(p);
    }

    /**
     * Helper method for converting a list of ParcelImpl items into VersionedParcelable items
     *
     * @param parcelList
     * @return
     */
    @SuppressWarnings({"TypeParameterUnusedInFormals", "unchecked"})
    @NonNull
    public static <T extends VersionedParcelable> List<T> fromParcelableList(
            @NonNull List<ParcelImpl> parcelList) {
        List<T> list = new ArrayList<>();
        for (int i = 0; i < parcelList.size(); i++) {
            list.add((T) fromParcelable(parcelList.get(i)));
        }
        return list;
    }

    @SuppressLint("RestrictedApi")
    private static class MediaItemParcelImpl extends ParcelImpl {
        private final MediaItem mItem;
        MediaItemParcelImpl(MediaItem item) {
            // Up-cast (possibly MediaItem's subclass object) item to MediaItem for the
            // writeToParcel(). The copied media item will be only used when it's sent across the
            // process.
            super(new MediaItem(item));

            // Keeps the original copy for local binder to send the original item.
            // When local binder is used (i.e. binder call happens in a single process),
            // writeToParcel() wouldn't happen for the Parcelable object and the same object will
            // be sent through the binder call.
            mItem = item;
        }

        @Override
        @SuppressWarnings("unchecked")
        public MediaItem getVersionedParcel() {
            return mItem;
        }
    }
}
