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

package androidx.wear.watchface.control.data;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;
import androidx.wear.watchface.data.DeviceConfig;

/**
 * Parameters for {@link IWatchFaceControlService#createHeadlessWatchFaceInstance}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VersionedParcelize
@SuppressLint("BanParcelableUsage") // TODO(b/169214666): Remove Parcelable
public class HeadlessWatchFaceInstanceParams implements VersionedParcelable, Parcelable {

    /** The {@link ComponentName} of the watchface family to create. */
    @ParcelField(1)
    @NonNull
    ComponentName mWatchFaceName;

    /** The {@link DeviceConfig} for the host wearable. */
    @ParcelField(2)
    @NonNull
    DeviceConfig mDeviceConfig;

    /** The desired width for screenshots. */
    @ParcelField(3)
    int mWidth;

    /** The desired height for screenshots. */
    @ParcelField(4)
    int mHeight;

    /** Used by VersionedParcelable. */
    HeadlessWatchFaceInstanceParams() {}

    public HeadlessWatchFaceInstanceParams(
            @NonNull ComponentName watchFaceName,
            @NonNull DeviceConfig deviceConfig,
            int width,
            int height) {
        mWatchFaceName = watchFaceName;
        mDeviceConfig = deviceConfig;
        mWidth = width;
        mHeight = height;
    }

    @NonNull
    public ComponentName getWatchFaceName() {
        return mWatchFaceName;
    }

    @NonNull
    public DeviceConfig getDeviceConfig() {
        return mDeviceConfig;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    /** Serializes this InteractiveWatchFaceInstanceParams to the specified {@link Parcel}. */
    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeParcelable(ParcelUtils.toParcelable(this), flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<HeadlessWatchFaceInstanceParams> CREATOR =
            new Parcelable.Creator<HeadlessWatchFaceInstanceParams>() {
                @Override
                public HeadlessWatchFaceInstanceParams createFromParcel(Parcel source) {
                    return ParcelUtils.fromParcelable(
                            source.readParcelable(getClass().getClassLoader()));
                }

                @Override
                public HeadlessWatchFaceInstanceParams[] newArray(int size) {
                    return new HeadlessWatchFaceInstanceParams[size];
                }
            };
}
