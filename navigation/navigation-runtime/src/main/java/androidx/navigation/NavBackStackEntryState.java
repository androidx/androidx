/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

@SuppressLint("BanParcelableUsage")
final class NavBackStackEntryState implements Parcelable {

    private final UUID mUUID;
    private final int mDestinationId;
    private final Bundle mArgs;
    private final Bundle mSavedState;

    NavBackStackEntryState(NavBackStackEntry entry) {
        mUUID = entry.mId;
        mDestinationId = entry.getDestination().getId();
        mArgs = entry.getArguments();
        mSavedState = new Bundle();
        entry.saveState(mSavedState);
    }

    @SuppressWarnings("WeakerAccess")
    NavBackStackEntryState(Parcel in) {
        mUUID = UUID.fromString(in.readString());
        mDestinationId = in.readInt();
        mArgs = in.readBundle(getClass().getClassLoader());
        mSavedState = in.readBundle(getClass().getClassLoader());
    }

    @NonNull
    UUID getUUID() {
        return mUUID;
    }

    int getDestinationId() {
        return mDestinationId;
    }

    @Nullable
    Bundle getArgs() {
        return mArgs;
    }

    @NonNull
    Bundle getSavedState() {
        return mSavedState;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(mUUID.toString());
        parcel.writeInt(mDestinationId);
        parcel.writeBundle(mArgs);
        parcel.writeBundle(mSavedState);
    }

    public static final Parcelable.Creator<NavBackStackEntryState> CREATOR =
            new Parcelable.Creator<NavBackStackEntryState>() {
                @Override
                public NavBackStackEntryState createFromParcel(Parcel in) {
                    return new NavBackStackEntryState(in);
                }

                @Override
                public NavBackStackEntryState[] newArray(int size) {
                    return new NavBackStackEntryState[size];
                }
            };
}
