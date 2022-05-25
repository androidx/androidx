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

package androidx.wear.watchface.complications.data;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.wearable.complications.ComplicationData;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.List;

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VersionedParcelize
@SuppressLint("BanParcelableUsage") // TODO(b/169214666): Remove Parcelable
public final class DefaultComplicationDataSourcePolicyWireFormat
        implements VersionedParcelable, Parcelable {

    @ParcelField(1)
    @NonNull
    public List<ComponentName> mDefaultDataSourcesToTry;

    @ParcelField(2)
    public int mFallbackSystemDataSource;

    @ParcelField(3)
    @ComplicationData.ComplicationType
    public int mDefaultType;

    @ParcelField(4)
    @ComplicationData.ComplicationType
    public int mPrimaryDataSourceDefaultType;

    @ParcelField(5)
    @ComplicationData.ComplicationType
    public int mSecondaryDataSourceDefaultType;

    DefaultComplicationDataSourcePolicyWireFormat() {
    }

    public DefaultComplicationDataSourcePolicyWireFormat(
            @NonNull List<ComponentName> defaultDataSourcesToTry,
            int fallbackSystemDataSource,
            @ComplicationData.ComplicationType int defaultDataSourceType,
            @ComplicationData.ComplicationType int primaryDataSourceDefaultType,
            @ComplicationData.ComplicationType int secondaryDataSourceDefaultType) {
        mDefaultDataSourcesToTry = defaultDataSourcesToTry;
        mPrimaryDataSourceDefaultType = primaryDataSourceDefaultType;
        mSecondaryDataSourceDefaultType = secondaryDataSourceDefaultType;
        mFallbackSystemDataSource = fallbackSystemDataSource;
        mDefaultType = defaultDataSourceType;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeParcelable(ParcelUtils.toParcelable(this), flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DefaultComplicationDataSourcePolicyWireFormat> CREATOR =
            new Creator<DefaultComplicationDataSourcePolicyWireFormat>() {
                @SuppressWarnings("deprecation")
                @Override
                public DefaultComplicationDataSourcePolicyWireFormat createFromParcel(
                        Parcel source) {
                    return ParcelUtils.fromParcelable(
                            source.readParcelable(getClass().getClassLoader()));
                }

                @Override
                public DefaultComplicationDataSourcePolicyWireFormat[] newArray(int size) {
                    return new DefaultComplicationDataSourcePolicyWireFormat[size];
                }
            };
}
