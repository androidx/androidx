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

import java.util.List;

/**
 * Data sent over AIDL for {@link IWatchFaceCommand#setComplicationDetails}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
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

    /** Used by VersionedParcelable. */
    ComplicationStateWireFormat() {
    }

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
            @NonNull Bundle complicationConfigExtras) {
        mBounds = bounds;
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
    }

    /** @deprecated Use the other constructor instead. */
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
