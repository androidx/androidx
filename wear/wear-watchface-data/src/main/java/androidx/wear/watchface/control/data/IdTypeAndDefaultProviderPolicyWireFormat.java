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

package androidx.wear.watchface.control.data;

import android.annotation.SuppressLint;
import android.content.ComponentName;
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
 * Wire format to encode a pair of id and DefaultProviderPolicy.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VersionedParcelize
@SuppressLint("BanParcelableUsage")
public final class IdTypeAndDefaultProviderPolicyWireFormat implements VersionedParcelable,
        Parcelable {
    /** The watch's ID for the complication. */
    @ParcelField(1)
    int mId;

    @ParcelField(2)
    @Nullable
    List<ComponentName> mDefaultProvidersToTry;

    @ParcelField(3)
    int mFallbackSystemProvider;

    @ParcelField(4)
    @ComplicationData.ComplicationType
    int mDefaultProviderType;

    /** Used by VersionedParcelable. */
    IdTypeAndDefaultProviderPolicyWireFormat() {
    }

    public IdTypeAndDefaultProviderPolicyWireFormat(
            int id,
            @Nullable List<ComponentName> defaultProvidersToTry,
            int fallbackSystemProvider,
            @ComplicationData.ComplicationType int defaultProviderType) {
        mId = id;
        mDefaultProvidersToTry = defaultProvidersToTry;
        mFallbackSystemProvider = fallbackSystemProvider;
        mDefaultProviderType = defaultProviderType;
    }

    public int getId() {
        return mId;
    }

    /**
     * Along with {@link #getFallbackSystemProvider} this is the wire format for
     * DefaultComplicationDataSourcePolicy.
     */
    @Nullable
    public List<ComponentName> getDefaultProvidersToTry() {
        return mDefaultProvidersToTry;
    }

    /**
     * Along with {@link #getDefaultProvidersToTry} this is the wire format for
     * DefaultComplicationDataSourcePolicy.
     */
    public int getFallbackSystemProvider() {
        return mFallbackSystemProvider;
    }

    @ComplicationData.ComplicationType
    public int getDefaultProviderType() {
        return mDefaultProviderType;
    }

    /** Serializes this IdAndDefaultProviderPolicyWireFormat to the specified {@link Parcel}. */
    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeParcelable(ParcelUtils.toParcelable(this), flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<IdTypeAndDefaultProviderPolicyWireFormat> CREATOR =
            new Parcelable.Creator<IdTypeAndDefaultProviderPolicyWireFormat>() {
                @Override
                public IdTypeAndDefaultProviderPolicyWireFormat createFromParcel(Parcel source) {
                    return ParcelUtils.fromParcelable(
                            source.readParcelable(getClass().getClassLoader()));
                }

                @Override
                public IdTypeAndDefaultProviderPolicyWireFormat[] newArray(int size) {
                    return new IdTypeAndDefaultProviderPolicyWireFormat[size];
                }
            };
}
