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

package androidx.media2;

import android.annotation.SuppressLint;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.versionedparcelable.ParcelImpl;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
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
    public static ParcelImpl toParcelable(VersionedParcelable item) {
        if (item instanceof MediaItem) {
            return new MediaItemParcelImpl((MediaItem) item);
        }
        return (ParcelImpl) ParcelUtils.toParcelable(item);
    }

    /**
     * Media2 version of {@link ParcelUtils#fromParcelable(Parcelable)}.
     */
    @SuppressWarnings("TypeParameterUnusedInFormals")
    public static <T extends VersionedParcelable> T fromParcelable(ParcelImpl p) {
        return ParcelUtils.<T>fromParcelable(p);
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
        public MediaItem getVersionedParcel() {
            return mItem;
        }
    }
}
