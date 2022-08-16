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
import androidx.wear.watchface.complications.data.ComplicationExperimental;

import java.util.List;

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@VersionedParcelize
@SuppressLint("BanParcelableUsage") // TODO(b/169214666): Remove Parcelable
public final class ComplicationSlotMetadataWireFormat implements VersionedParcelable, Parcelable {

    @ParcelField(1)
    int mId;

    @ParcelField(2)
    @NonNull
    int[] mComplicationBoundsType = new int[0];

    @ParcelField(3)
    @NonNull
    RectF[] mComplicationBounds = new RectF[0];

    @ParcelField(4)
    int mBoundsType;

    @ParcelField(5)
    @NonNull
    @ComplicationData.ComplicationType
    int[] mSupportedTypes = new int[0];

    @ParcelField(6)
    @Nullable
    List<ComponentName> mDefaultDataSourcesToTry;

    @ParcelField(7)
    int mFallbackSystemDataSource;

    @ParcelField(8)
    @ComplicationData.ComplicationType
    int mDefaultType;

    @ParcelField(9)
    boolean mIsInitiallyEnabled;

    @ParcelField(10)
    boolean mFixedComplicationDataSource;

    @ParcelField(11)
    @NonNull
    Bundle mComplicationConfigExtras;

    // Not supported in library v1.0.
    @ParcelField(12)
    @ComplicationData.ComplicationType
    int mPrimaryDataSourceDefaultType = ComplicationData.TYPE_NOT_CONFIGURED;

    // Not supported in library v1.0.
    @ParcelField(13)
    @ComplicationData.ComplicationType
    int mSecondaryDataSourceDefaultType = ComplicationData.TYPE_NOT_CONFIGURED;

    // Only valid for edge complications. Not supported in library v1.0.
    @ParcelField(14)
    @Nullable
    BoundingArcWireFormat mBoundingArc;

    // This needs to be a list because VersionedParcelable appears not to be backwards compatible
    // when introducing new arrays.
    @ParcelField(value = 15, defaultValue = "null")
    @Nullable
    List<RectF> mComplicationMargins;

    /** Used by VersionedParcelable. */
    ComplicationSlotMetadataWireFormat() {
    }

    @ComplicationExperimental
    public ComplicationSlotMetadataWireFormat(
            int id,
            @NonNull int[] complicationBoundsType,
            @NonNull RectF[] complicationBounds,
            @NonNull List<RectF> complicationMargins,
            int boundsType,
            @NonNull @ComplicationData.ComplicationType int[] supportedTypes,
            @Nullable List<ComponentName> defaultDataSourcesToTry,
            int fallbackSystemDataSource,
            @ComplicationData.ComplicationType int defaultDataSourceType,
            @ComplicationData.ComplicationType int primaryDataSourceDefaultType,
            @ComplicationData.ComplicationType int secondaryDataSourceDefaultType,
            boolean isInitiallyEnabled,
            boolean fixedComplicationDataSource,
            @NonNull Bundle complicationConfigExtras,
            @Nullable BoundingArcWireFormat boundingArc) {
        mId = id;
        mComplicationBoundsType = complicationBoundsType;
        mComplicationBounds = complicationBounds;
        mComplicationMargins = complicationMargins;
        mBoundsType = boundsType;
        mSupportedTypes = supportedTypes;
        mDefaultDataSourcesToTry = defaultDataSourcesToTry;
        mPrimaryDataSourceDefaultType = primaryDataSourceDefaultType;
        mSecondaryDataSourceDefaultType = secondaryDataSourceDefaultType;
        mFallbackSystemDataSource = fallbackSystemDataSource;
        mDefaultType = defaultDataSourceType;
        mIsInitiallyEnabled = isInitiallyEnabled;
        mFixedComplicationDataSource = fixedComplicationDataSource;
        mComplicationConfigExtras = complicationConfigExtras;
        mBoundingArc = boundingArc;
    }

    // TODO(b/230364881): Deprecate when BoundingArc is no longer experimental.
    public ComplicationSlotMetadataWireFormat(
            int id,
            @NonNull int[] complicationBoundsType,
            @NonNull RectF[] complicationBounds,
            int boundsType,
            @NonNull @ComplicationData.ComplicationType int[] supportedTypes,
            @Nullable List<ComponentName> defaultDataSourcesToTry,
            int fallbackSystemDataSource,
            @ComplicationData.ComplicationType int defaultDataSourceType,
            @ComplicationData.ComplicationType int primaryDataSourceDefaultType,
            @ComplicationData.ComplicationType int secondaryDataSourceDefaultType,
            boolean isInitiallyEnabled,
            boolean fixedComplicationDataSource,
            @NonNull Bundle complicationConfigExtras) {
        mId = id;
        mComplicationBoundsType = complicationBoundsType;
        mComplicationBounds = complicationBounds;
        mBoundsType = boundsType;
        mSupportedTypes = supportedTypes;
        mDefaultDataSourcesToTry = defaultDataSourcesToTry;
        mPrimaryDataSourceDefaultType = primaryDataSourceDefaultType;
        mSecondaryDataSourceDefaultType = secondaryDataSourceDefaultType;
        mFallbackSystemDataSource = fallbackSystemDataSource;
        mDefaultType = defaultDataSourceType;
        mIsInitiallyEnabled = isInitiallyEnabled;
        mFixedComplicationDataSource = fixedComplicationDataSource;
        mComplicationConfigExtras = complicationConfigExtras;
    }

    /**
     * @deprecated Use the other constructor with primaryDataSourceDefaultType &
     * secondaryDataSourceDefaultType instead.
     */
    @Deprecated
    public ComplicationSlotMetadataWireFormat(
            int id,
            @NonNull int[] complicationBoundsType,
            @NonNull RectF[] complicationBounds,
            int boundsType,
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
        mDefaultType = defaultDataSourceType;
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

    @Nullable
    public List<RectF> getComplicationMargins() {
        return mComplicationMargins;
    }

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

    /**
     * @return The {@link ComplicationData.ComplicationType} for
     * {@link #getFallbackSystemDataSource}.
     */
    @ComplicationData.ComplicationType
    public int getDefaultDataSourceType() {
        return mDefaultType;
    }

    /**
     * @return The {@link ComplicationData.ComplicationType} for the first entry from
     * {@link #getDefaultDataSourcesToTry}.
     */
    @ComplicationData.ComplicationType
    public int getPrimaryDataSourceDefaultType() {
        // Not supported in library v1.0. TYPE_NOT_CONFIGURED is not a valid API choice indicating
        // and old client.
        return (mPrimaryDataSourceDefaultType == ComplicationData.TYPE_NOT_CONFIGURED)
                ? mDefaultType : mPrimaryDataSourceDefaultType;
    }

    /**
     * @return The {@link ComplicationData.ComplicationType} for the second entry from
     * {@link #getDefaultDataSourcesToTry}.
     */
    @ComplicationData.ComplicationType
    public int getSecondaryDataSourceDefaultType() {
        // Not supported in library v1.0. TYPE_NOT_CONFIGURED is not a valid API choice indicating
        // and old client.
        return (mSecondaryDataSourceDefaultType == ComplicationData.TYPE_NOT_CONFIGURED)
                ? mDefaultType : mSecondaryDataSourceDefaultType;
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

    @Nullable
    @ComplicationExperimental
    public BoundingArcWireFormat getBoundingArc() {
        return mBoundingArc;
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
                @SuppressWarnings("deprecation")
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
