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
 * Data sent over AIDL for {@link IWatchFaceCommand#setIndicatorState}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VersionedParcelize
public final class IndicatorState implements VersionedParcelable {
    @ParcelField(1)
    boolean mIsCharging;

    @ParcelField(2)
    boolean mInAirplaneMode;

    @ParcelField(3)
    boolean mIsConnectedToCompanion;

    @ParcelField(4)
    boolean mInTheaterMode;

    @ParcelField(5)
    boolean mIsGpsActive;

    @ParcelField(6)
    boolean mIsKeyguardLocked;

    /** Used by VersionedParcelable. */
    IndicatorState() {}

    public IndicatorState(
            boolean isCharging,
            boolean inAirplaneMode,
            boolean isConnectedToCompanion,
            boolean inTheaterMode,
            boolean isGpsActive,
            boolean isKeyguardLocked) {
        mIsCharging = isCharging;
        mInAirplaneMode = inAirplaneMode;
        mIsConnectedToCompanion = isConnectedToCompanion;
        mInTheaterMode = inTheaterMode;
        mIsGpsActive = isGpsActive;
        mIsKeyguardLocked = isKeyguardLocked;
    }

    public boolean getIsCharging() {
        return mIsCharging;
    }

    public boolean getInAirplaneMode() {
        return mInAirplaneMode;
    }

    public boolean getIsConnectedToCompanion() {
        return mIsConnectedToCompanion;
    }

    public boolean getInTheaterMode() {
        return mInTheaterMode;
    }

    public boolean getIsGpsActive() {
        return mIsGpsActive;
    }

    public boolean getIsKeyguardLocked() {
        return mIsKeyguardLocked;
    }

    /** Serializes this SystemState to the specified {@link Parcel}. */
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeParcelable(ParcelUtils.toParcelable(this), flags);
    }

    public static final Parcelable.Creator<IndicatorState> CREATOR =
            new Parcelable.Creator<IndicatorState>() {
                @Override
                public IndicatorState createFromParcel(Parcel source) {
                    return IndicatorStateParcelizer.read(
                            ParcelUtils.fromParcelable(source.readParcelable(
                                    getClass().getClassLoader())));
                }

                @Override
                public IndicatorState[] newArray(int size) {
                    return new IndicatorState[size];
                }
            };
}
