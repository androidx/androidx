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
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat;
import androidx.wear.watchface.data.RenderParametersWireFormat;
import androidx.wear.watchface.style.data.UserStyleWireFormat;

import java.util.List;

/**
 * Parameters for the various takeWatchfaceScreenshot AIDL methods.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VersionedParcelize
@SuppressLint("BanParcelableUsage") // TODO(b/169214666): Remove Parcelable
public class WatchFaceRenderParams implements VersionedParcelable, Parcelable {
    /** The {@link RenderParametersWireFormat} to render with. */
    @ParcelField(1)
    @NonNull
    RenderParametersWireFormat mRenderParametersWireFormats;

    /** The UTC time in milliseconds since the epoch to render with. */
    @ParcelField(3)
    long mCalendarTimeMillis;

    /**
     * The {@link UserStyleWireFormat} to render with. If null then the current style is used
     * instead.
     */
    @ParcelField(5)
    @Nullable
    UserStyleWireFormat mUserStyle;

    /**
     * The complications to render with. If null then the current complication data is used
     * instead.
     */
    @ParcelField(100)
    @Nullable
    List<IdAndComplicationDataWireFormat> mIdAndComplicationDatumWireFormats;

    /** Used by VersionedParcelable. */
    WatchFaceRenderParams() {}

    public WatchFaceRenderParams(
            @NonNull RenderParametersWireFormat renderParametersWireFormats,
            long calendarTimeMillis,
            @Nullable UserStyleWireFormat userStyle,
            @Nullable List<IdAndComplicationDataWireFormat> idAndComplicationDatumWireFormats) {
        mRenderParametersWireFormats = renderParametersWireFormats;
        mCalendarTimeMillis = calendarTimeMillis;
        mUserStyle = userStyle;
        mIdAndComplicationDatumWireFormats = idAndComplicationDatumWireFormats;
    }

    @NonNull
    public RenderParametersWireFormat getRenderParametersWireFormat() {
        return mRenderParametersWireFormats;
    }

    /** The UTC time in milliseconds since the epoch. */
    public long getCalendarTimeMillis() {
        return mCalendarTimeMillis;
    }

    @Nullable
    public UserStyleWireFormat getUserStyle() {
        return mUserStyle;
    }

    @Nullable
    public List<IdAndComplicationDataWireFormat> getIdAndComplicationDatumWireFormats() {
        return mIdAndComplicationDatumWireFormats;
    }

    /** Serializes this WatchfaceScreenshotParams to the specified {@link Parcel}. */
    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeParcelable(ParcelUtils.toParcelable(this), flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<WatchFaceRenderParams> CREATOR =
            new Parcelable.Creator<WatchFaceRenderParams>() {
                @Override
                public WatchFaceRenderParams createFromParcel(Parcel source) {
                    return ParcelUtils.fromParcelable(
                            source.readParcelable(getClass().getClassLoader()));
                }

                @Override
                public WatchFaceRenderParams[] newArray(int size) {
                    return new WatchFaceRenderParams[size];
                }
            };
}
