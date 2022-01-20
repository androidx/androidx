/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.watchface.data;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * Wire format for {@link androidx.wear.watchface.WatchFace.WatchFaceOverlayStyle}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VersionedParcelize
@SuppressLint("BanParcelableUsage")
public class WatchFaceOverlayStyleWireFormat implements VersionedParcelable, Parcelable  {
    @ParcelField(1)
    boolean mHasBackgroundColor;

    @ParcelField(2)
    int mBackgroundColor;

    @ParcelField(3)
    boolean mHasForegroundColor;

    @ParcelField(4)
    int mForegroundColor;

    WatchFaceOverlayStyleWireFormat() {}

    public WatchFaceOverlayStyleWireFormat(
            @Nullable Color backgroundColor,
            @Nullable Color foregroundColor) {
        if (backgroundColor == null) {
            mHasBackgroundColor = false;
        } else {
            mHasBackgroundColor = true;
            mBackgroundColor = backgroundColor.toArgb();
        }

        if (foregroundColor == null) {
            mHasForegroundColor = false;
        } else {
            mHasForegroundColor = true;
            mForegroundColor = foregroundColor.toArgb();
        }
    }

    /** Returns the background color or `null`. */
    @Nullable
    public Color getBackgroundColor() {
        if (!mHasBackgroundColor) {
            return null;
        }
        return Color.valueOf(mBackgroundColor);
    }

    /** Returns the foreground color or `null`. */
    @Nullable
    public Color getForegroundColor() {
        if (!mHasForegroundColor) {
            return null;
        }
        return Color.valueOf(mForegroundColor);
    }

    /** Serializes this WatchFaceOverlayStyleWireFormat to the specified {@link Parcel}. */
    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeParcelable(ParcelUtils.toParcelable(this), flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<WatchFaceOverlayStyleWireFormat> CREATOR =
            new Parcelable.Creator<WatchFaceOverlayStyleWireFormat>() {
                @SuppressWarnings("deprecation")
                @Override
                public WatchFaceOverlayStyleWireFormat createFromParcel(Parcel source) {
                    return ParcelUtils.fromParcelable(
                            source.readParcelable(getClass().getClassLoader()));
                }

                @Override
                public WatchFaceOverlayStyleWireFormat[] newArray(int size) {
                    return new WatchFaceOverlayStyleWireFormat[size];
                }
            };
}
