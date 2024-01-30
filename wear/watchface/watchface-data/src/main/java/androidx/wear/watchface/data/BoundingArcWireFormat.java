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
import androidx.wear.watchface.complications.data.ComplicationExperimental;

/** Wire format for BoundingArc. */
// TODO(b/230364881): Mark as @RestrictTo when BoundingArc is no longer experimental.
@VersionedParcelize
@SuppressLint("BanParcelableUsage")
@ComplicationExperimental
public final class BoundingArcWireFormat implements VersionedParcelable, Parcelable {

    @ParcelField(1)
    float mArcStartAngle = 0.0f;

    @ParcelField(2)
    float mTotalArcAngle = 0.0f;

    @ParcelField(3)
    float mArcThickness = 0.0f;

    /** Used by VersionedParcelable. */
    BoundingArcWireFormat() {}

    public BoundingArcWireFormat(float arcStartAngle, float totalArcAngle, float arcThickness) {
        mArcStartAngle = arcStartAngle;
        mTotalArcAngle = totalArcAngle;
        mArcThickness = arcThickness;
    }

    public float getArcStartAngle() {
        return mArcStartAngle;
    }

    public float getTotalArcAngle() {
        return mTotalArcAngle;
    }

    public float getArcThickness() {
        return mArcThickness;
    }

    /** Serializes this BoundingArcWireFormat to the specified {@link Parcel}. */
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
    public static final Parcelable.Creator<BoundingArcWireFormat> CREATOR =
            new Parcelable.Creator<BoundingArcWireFormat>() {
                @SuppressWarnings("deprecation")
                @Override
                public BoundingArcWireFormat createFromParcel(Parcel source) {
                    return ParcelUtils.fromParcelable(
                            source.readParcelable(getClass().getClassLoader()));
                }

                @Override
                public BoundingArcWireFormat[] newArray(int size) {
                    return new BoundingArcWireFormat[size];
                }
            };
}
