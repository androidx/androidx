/*
 * Copyright 2021 The Android Open Source Project
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
import android.content.ComponentName;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.wearable.complications.ComplicationData;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
public final class ComplicationSlotMetadataWireFormat implements VersionedParcelable, Parcelable {

    @ParcelField(1)
    int mId;

    @ParcelField(2)
    @NonNull
    int[] mComplicationBoundsType;

    @ParcelField(3)
    @NonNull
    RectF[] mComplicationBounds;

    @ParcelField(4)
    @ComplicationSlotBoundsType
    int mBoundsType;

    @ParcelField(5)
    @NonNull
    @ComplicationData.ComplicationType
    int[] mSupportedTypes;

    @ParcelField(6)
    @Nullable
    List<ComponentName> mDefaultDataSourcesToTry;

    @ParcelField(7)
    int mFallbackSystemDataSource;

    @ParcelField(8)
    @ComplicationData.ComplicationType
    int mDefaultDataSourceType;

    @ParcelField(9)
    boolean mIsInitiallyEnabled;

    @ParcelField(10)
    boolean mFixedComplicationDataSource;

    @ParcelField(11)
    @NonNull
    Bundle mComplicationConfigExtras;

    /** Used by VersionedParcelable. */
    ComplicationSlotMetadataWireFormat() {
    }

    public ComplicationSlotMetadataWireFormat(
            int id,
            @NonNull int[] complicationBoundsType,
            @NonNull RectF[] complicationBounds,
            @ComplicationSlotBoundsType int boundsType,
            @NonNull @ComplicationData.ComplicationType int[] supportedTypes,
            @Nullable List<ComponentName> defaultDataSourcesToTry,
            int fallbackSystemDataSource,
            @ComplicationData.ComplicationType int defaultDataSourceType,
            boolean isInitiallyEnabled,
            boolean fixedComplicationDataSource,
            @NonNull Bundle complicationConfigExtras) {
        mId = id;
        mComplicationBoundsType = complicationBoundsType;
        mComplicationBounds = complicationBounds;
        mBoundsType = boundsType;
        mSupportedTypes = supportedTypes;
        mDefaultDataSourcesToTry = defaultDataSourcesToTry;
        mFallbackSystemDataSource = fallbackSystemDataSource;
        mDefaultDataSourceType = defaultDataSourceType;
        mIsInitiallyEnabled = isInitiallyEnabled;
        mFixedComplicationDataSource = fixedComplicationDataSource;
        mComplicationConfigExtras = complicationConfigExtras;
    }

    public int getId() {
        return mId;
    }

    @NonNull
    public int[] getComplicationBoundsType() {
        return mComplicationBoundsType;
    }

    @NonNull
    public RectF[] getComplicationBounds() {
        return mComplicationBounds;
    }

    @ComplicationSlotBoundsType
    public int getBoundsType() {
        return mBoundsType;
    }

    @NonNull
    @ComplicationData.ComplicationType
    public int[] getSupportedTypes() {
        return mSupportedTypes;
    }

    /**
     * Along with {@link #getFallbackSystemDataSource} this is the wire format for
     * DefaultComplicationDataSourcePolicy.
     */
    @Nullable
    public List<ComponentName> getDefaultDataSourcesToTry() {
        return mDefaultDataSourcesToTry;
    }

    /**
     * Along with {@link #getDefaultDataSourcesToTry} this is the wire format for
     * DefaultComplicationDataSourcePolicy.
     */
    public int getFallbackSystemDataSource() {
        return mFallbackSystemDataSource;
    }

    @ComplicationData.ComplicationType
    public int getDefaultDataSourceType() {
        return mDefaultDataSourceType;
    }

    public boolean isInitiallyEnabled() {
        return mIsInitiallyEnabled;
    }

    public boolean isFixedComplicationDataSource() {
        return mFixedComplicationDataSource;
    }

    @NonNull
    public Bundle getComplicationConfigExtras() {
        return mComplicationConfigExtras;
    }

    /** Serializes this ComplicationDetails to the specified {@link Parcel}. */
    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeParcelable(ParcelUtils.toParcelable(this), flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<ComplicationSlotMetadataWireFormat> CREATOR =
            new Parcelable.Creator<ComplicationSlotMetadataWireFormat>() {
                @Override
                public ComplicationSlotMetadataWireFormat createFromParcel(Parcel source) {
                    return ParcelUtils.fromParcelable(
                            source.readParcelable(getClass().getClassLoader()));
                }

                @Override
                public ComplicationSlotMetadataWireFormat[] newArray(int size) {
                    return new ComplicationSlotMetadataWireFormat[size];
                }
            };
}
