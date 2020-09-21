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

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * Data sent over AIDL for {@link IWatchFaceCommand#setSystemState}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VersionedParcelize
public final class ImmutableSystemState implements VersionedParcelable {
    @ParcelField(1)
    boolean mHasLowBitAmbient;

    @ParcelField(2)
    boolean mHasBurnInProtection;

    /** Used by VersionedParcelable. */
    ImmutableSystemState() {}

    public ImmutableSystemState(
            boolean hasLowBitAmbient,
            boolean hasBurnInProtection) {
        mHasLowBitAmbient = hasLowBitAmbient;
        mHasBurnInProtection = hasBurnInProtection;
    }

    public boolean getHasLowBitAmbient() {
        return mHasLowBitAmbient;
    }

    public boolean getHasBurnInProtection() {
        return mHasBurnInProtection;
    }

    /** Serializes this SystemState to the specified {@link Parcel}. */
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeParcelable(ParcelUtils.toParcelable(this), flags);
    }

    public static final Parcelable.Creator<ImmutableSystemState> CREATOR =
            new Parcelable.Creator<ImmutableSystemState>() {
                @Override
                public ImmutableSystemState createFromParcel(Parcel source) {
                    return ImmutableSystemStateParcelizer.read(
                            ParcelUtils.fromParcelable(source.readParcelable(
                                    getClass().getClassLoader())));
                }

                @Override
                public ImmutableSystemState[] newArray(int size) {
                    return new ImmutableSystemState[size];
                }
            };
}
