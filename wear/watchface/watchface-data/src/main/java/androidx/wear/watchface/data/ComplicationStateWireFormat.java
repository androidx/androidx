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

package androidx.wear.watchface.data;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.graphics.Rect;
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

/**
 * Data sent over AIDL for {@link IWatchFaceCommand#setComplicationDetails}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@VersionedParcelize
@SuppressLint("BanParcelableUsage") // TODO(b/169214666): Remove Parcelable
public final class ComplicationStateWireFormat implements VersionedParcelable, Parcelable {
    @ParcelField(1)
    @NonNull
    Rect mBounds;

    @ParcelField(2)
    int mBoundsType;

    @ParcelField(3)
    @NonNull
    @ComplicationData.ComplicationType
    int[] mSupportedTypes;

    @ParcelField(4)
    @Nullable
    List<ComponentName> mDefaultDataSourcesToTry;

    @ParcelField(5)
    int mFallbackSystemProvider;

    @ParcelField(6)
    @ComplicationData.ComplicationType
    int mDefaultType;

    @ParcelField(7)
    boolean mIsEnabled;

    @ParcelField(8)
    boolean mIsInitiallyEnabled;

    @ParcelField(9)
    @ComplicationData.ComplicationType
    int mCurrentType;

    @ParcelField(10)
    boolean mFixedComplicationProvider;

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

    // Not supported in library v1.0.
    @ParcelField(14)
    int mNameResourceId;

    // Not supported in library v1.0.
    @ParcelField(15)
    int mScreenReaderNameResourceId;

    // Only valid for edge complications. Not supported in library v1.0.
    @ParcelField(16)
    @Nullable
    BoundingArcWireFormat mBoundingArc;

    @ParcelField(value = 17, defaultValue = "null")
    @Nullable
    Rect mBoundsWithMargins;

    // NB 0 is not a valid resource id.
    private static final int NULL_NAME_RESOURCE_ID = 0;

    /** Used by VersionedParcelable. */
    ComplicationStateWireFormat() {
    }

    @ComplicationExperimental
    public ComplicationStateWireFormat(
            @NonNull Rect bounds,
            @NonNull Rect boundsWithMargins,
            int boundsType,
            @NonNull @ComplicationData.ComplicationType int[] supportedTypes,
            @Nullable List<ComponentName> defaultDataSourcesToTry,
            int fallbackSystemProvider,
            @ComplicationData.ComplicationType int defaultDataSourceType,
            @ComplicationData.ComplicationType int primaryDataSourceDefaultType,
            @ComplicationData.ComplicationType int secondaryDataSourceDefaultType,
            boolean isEnabled,
            boolean isInitiallyEnabled,
            @ComplicationData.ComplicationType int currentType,
            boolean fixedComplicationProvider,
            @NonNull Bundle complicationConfigExtras,
            @Nullable Integer nameResourceId,
            @Nullable Integer screenReaderNameResourceId,
            @Nullable BoundingArcWireFormat boundingArc) {
        mBounds = bounds;
        mBoundsWithMargins = boundsWithMargins;
        mBoundsType = boundsType;
        mSupportedTypes = supportedTypes;
        mDefaultDataSourcesToTry = defaultDataSourcesToTry;
        mFallbackSystemProvider = fallbackSystemProvider;
        mDefaultType = defaultDataSourceType;
        mPrimaryDataSourceDefaultType = primaryDataSourceDefaultType;
        mSecondaryDataSourceDefaultType = secondaryDataSourceDefaultType;
        mIsEnabled = isEnabled;
        mIsInitiallyEnabled = isInitiallyEnabled;
        mCurrentType = currentType;
        mFixedComplicationProvider = fixedComplicationProvider;
        mComplicationConfigExtras = complicationConfigExtras;
        mNameResourceId = (nameResourceId != null) ? nameResourceId : NULL_NAME_RESOURCE_ID;
        mScreenReaderNameResourceId =
                (screenReaderNameResourceId != null) ? screenReaderNameResourceId
                        : NULL_NAME_RESOURCE_ID;
        mBoundingArc = boundingArc;
    }

    // TODO(b/230364881): Deprecate when BoundingArc is no longer experimental.
    public ComplicationStateWireFormat(
            @NonNull Rect bounds,
            int boundsType,
            @NonNull @ComplicationData.ComplicationType int[] supportedTypes,
            @Nullable List<ComponentName> defaultDataSourcesToTry,
            int fallbackSystemProvider,
            @ComplicationData.ComplicationType int defaultDataSourceType,
            @ComplicationData.ComplicationType int primaryDataSourceDefaultType,
            @ComplicationData.ComplicationType int secondaryDataSourceDefaultType,
            boolean isEnabled,
            boolean isInitiallyEnabled,
            @ComplicationData.ComplicationType int currentType,
            boolean fixedComplicationProvider,
            @NonNull Bundle complicationConfigExtras,
            @Nullable Integer nameResourceId,
            @Nullable Integer screenReaderNameResourceId) {
        mBounds = bounds;
        mBoundsWithMargins = bounds;
        mBoundsType = boundsType;
        mSupportedTypes = supportedTypes;
        mDefaultDataSourcesToTry = defaultDataSourcesToTry;
        mFallbackSystemProvider = fallbackSystemProvider;
        mDefaultType = defaultDataSourceType;
        mPrimaryDataSourceDefaultType = primaryDataSourceDefaultType;
        mSecondaryDataSourceDefaultType = secondaryDataSourceDefaultType;
        mIsEnabled = isEnabled;
        mIsInitiallyEnabled = isInitiallyEnabled;
        mCurrentType = currentType;
        mFixedComplicationProvider = fixedComplicationProvider;
        mComplicationConfigExtras = complicationConfigExtras;
        mNameResourceId = (nameResourceId != null) ? nameResourceId : NULL_NAME_RESOURCE_ID;
        mScreenReaderNameResourceId =
                (screenReaderNameResourceId != null) ? screenReaderNameResourceId
                        : NULL_NAME_RESOURCE_ID;
    }

    /**
     * @deprecated Use the other constructor instead.
     */
    @Deprecated
    public ComplicationStateWireFormat(
            @NonNull Rect bounds,
            int boundsType,
            @NonNull @ComplicationData.ComplicationType int[] supportedTypes,
            @Nullable List<ComponentName> defaultDataSourcesToTry,
            int fallbackSystemProvider,
            @ComplicationData.ComplicationType int defaultDataSourceType,
            boolean isEnabled,
            boolean isInitiallyEnabled,
            @ComplicationData.ComplicationType int currentType,
            boolean fixedComplicationProvider,
            @NonNull Bundle complicationConfigExtras) {
        mBounds = bounds;
        mBoundsWithMargins = bounds;
        mBoundsType = boundsType;
        mSupportedTypes = supportedTypes;
        mDefaultDataSourcesToTry = defaultDataSourcesToTry;
        mFallbackSystemProvider = fallbackSystemProvider;
        mDefaultType = defaultDataSourceType;
        mIsEnabled = isEnabled;
        mIsInitiallyEnabled = isInitiallyEnabled;
        mCurrentType = currentType;
        mFixedComplicationProvider = fixedComplicationProvider;
        mComplicationConfigExtras = complicationConfigExtras;
    }

    @NonNull
    public Rect getBounds() {
        return mBounds;
    }

    @Nullable
    public Rect getBoundsWithMargins() {
        return mBoundsWithMargins;
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
     * Along with {@link #getFallbackSystemProvider} this is the wire format for
     * DefaultComplicationDataSourcePolicy.
     *
     * @deprecated Use {@link #getDefaultDataSourcesToTry} instead.
     */
    @Deprecated
    @Nullable
    public List<ComponentName> getDefaultProvidersToTry() {
        return mDefaultDataSourcesToTry;
    }

    /**
     * Along with {@link #getFallbackSystemProvider} this is the wire format for
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
    public int getFallbackSystemProvider() {
        return mFallbackSystemProvider;
    }

    /** @deprecated Use {@link #getDefaultDataSourceType} instead. */
    @Deprecated
    @ComplicationData.ComplicationType
    public int getDefaultProviderType() {
        return mDefaultType;
    }

    /**
     * @return The {@link ComplicationData.ComplicationType} for {@link #getFallbackSystemProvider}.
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

    public boolean isEnabled() {
        return mIsEnabled;
    }

    public boolean isInitiallyEnabled() {
        return mIsInitiallyEnabled;
    }

    public boolean isFixedComplicationProvider() {
        return mFixedComplicationProvider;
    }

    @ComplicationData.ComplicationType
    public int getCurrentType() {
        return mCurrentType;
    }

    @NonNull
    public Bundle getComplicationConfigExtras() {
        return mComplicationConfigExtras;
    }

    @Nullable
    public Integer getNameResourceId() {
        return mNameResourceId != NULL_NAME_RESOURCE_ID ? mNameResourceId : null;
    }

    @Nullable
    public Integer getScreenReaderNameResourceId() {
        return mScreenReaderNameResourceId != NULL_NAME_RESOURCE_ID ? mScreenReaderNameResourceId
                : null;
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

    public static final Parcelable.Creator<ComplicationStateWireFormat> CREATOR =
            new Parcelable.Creator<ComplicationStateWireFormat>() {
                @SuppressWarnings("deprecation")
                @Override
                public ComplicationStateWireFormat createFromParcel(Parcel source) {
                    return ParcelUtils.fromParcelable(
                            source.readParcelable(getClass().getClassLoader()));
                }

                @Override
                public ComplicationStateWireFormat[] newArray(int size) {
                    return new ComplicationStateWireFormat[size];
                }
            };
}
