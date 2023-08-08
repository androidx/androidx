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
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

/** Wire format for WatchFaceColorsWireFormat. */
// TODO(b/230364881): Mark as @RestrictTo when BoundingArc is no longer experimental.
@VersionedParcelize
@SuppressLint("BanParcelableUsage")
public final class WatchFaceColorsWireFormat implements VersionedParcelable, Parcelable {

    @ParcelField(1)
    int mPrimaryColor;

    @ParcelField(2)
    int mSecondaryColor;

    @ParcelField(3)
    int mTertiaryColor;

    /** Used by VersionedParcelable. */
    WatchFaceColorsWireFormat() {}

    public WatchFaceColorsWireFormat(int primaryColor, int secondaryColor, int tertiaryColor) {
        mPrimaryColor = primaryColor;
        mSecondaryColor = secondaryColor;
        mTertiaryColor = tertiaryColor;
    }

    public int getPrimaryColor() {
        return mPrimaryColor;
    }

    public int getSecondaryColor() {
        return mSecondaryColor;
    }

    public int getTertiaryColor() {
        return mTertiaryColor;
    }

    /** Serializes this WatchFaceColorsWireFormat to the specified {@link Parcel}. */
    @Override
    public void writeToParcel(@Nullable Parcel parcel, int flags) {
        if (parcel != null) {
            parcel.writeParcelable(ParcelUtils.toParcelable(this), flags);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public static final Parcelable.Creator<WatchFaceColorsWireFormat> CREATOR =
            new Parcelable.Creator<WatchFaceColorsWireFormat>() {
                @SuppressWarnings("deprecation")
                @Override
                public WatchFaceColorsWireFormat createFromParcel(Parcel source) {
                    return ParcelUtils.fromParcelable(
                            source.readParcelable(getClass().getClassLoader()));
                }

                @Override
                public WatchFaceColorsWireFormat[] newArray(int size) {
                    return new WatchFaceColorsWireFormat[size];
                }
            };
}
