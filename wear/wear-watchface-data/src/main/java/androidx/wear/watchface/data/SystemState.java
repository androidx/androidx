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
@VersionedParcelize(allowSerialization = true)
@SuppressLint("BanParcelableUsage") // TODO(b/169214666): Remove Parcelable
public final class SystemState implements VersionedParcelable, Parcelable {
    @ParcelField(1)
    boolean mInAmbientMode;

    @ParcelField(2)
    int mInterruptionFilter;

    /** Used by VersionedParcelable. */
    SystemState() {}

    public SystemState(
            boolean inAmbientMode,
            int interruptionFilter) {
        mInAmbientMode = inAmbientMode;
        mInterruptionFilter = interruptionFilter;
    }

    public boolean getInAmbientMode() {
        return mInAmbientMode;
    }

    public int getInterruptionFilter() {
        return mInterruptionFilter;
    }

    /** Serializes this SystemState to the specified {@link Parcel}. */
    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeParcelable(ParcelUtils.toParcelable(this), flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<SystemState> CREATOR =
            new Parcelable.Creator<SystemState>() {
                @Override
                public SystemState createFromParcel(Parcel source) {
                    return ParcelUtils.fromParcelable(
                            source.readParcelable(getClass().getClassLoader()));
                }

                @Override
                public SystemState[] newArray(int size) {
                    return new SystemState[size];
                }
            };
}
