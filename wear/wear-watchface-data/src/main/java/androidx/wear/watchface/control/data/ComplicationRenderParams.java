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
import android.support.wearable.complications.ComplicationData;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;
import androidx.wear.watchface.data.RenderParametersWireFormat;
import androidx.wear.watchface.style.data.UserStyleWireFormat;

/**
 * Parameters for the various AIDL renderComplicationToBitmap commands.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VersionedParcelize
@SuppressLint("BanParcelableUsage") // TODO(b/169214666): Remove Parcelable
public class ComplicationRenderParams implements VersionedParcelable, Parcelable {

    /** ID of the complication slot we want to take a screen short of. */
    @ParcelField(1)
    int mComplicationSlotId;

    /** The {@link RenderParametersWireFormat} to render with. */
    @ParcelField(2)
    @NonNull
    RenderParametersWireFormat mRenderParametersWireFormats;

    /** The UTC time in milliseconds since the epoch to render with. */
    @ParcelField(4)
    long mCalendarTimeMillis;

    /**
     * The {@link ComplicationData} to render with. If null then interactive watch faces will
     * display the current complications, and headless watch faces will display an empty
     * complication.
     */
    @ParcelField(5)
    @Nullable
    ComplicationData mComplicationData;

    /**
     * The {@link UserStyleWireFormat} to render with. If null then interactive watch faces will
     * render with the current style, and headless watch faces will render with the default style.
     */
    @ParcelField(6)
    @Nullable
    UserStyleWireFormat mUserStyle;

    /** Used by VersionedParcelable. */
    ComplicationRenderParams() {}

    public ComplicationRenderParams(
            int complicationSlotId,
            @NonNull RenderParametersWireFormat renderParametersWireFormats,
            long calendarTimeMillis,
            @Nullable ComplicationData complicationData,
            @Nullable UserStyleWireFormat userStyle) {
        mComplicationSlotId = complicationSlotId;
        mRenderParametersWireFormats = renderParametersWireFormats;
        mCalendarTimeMillis = calendarTimeMillis;
        mComplicationData = complicationData;
        mUserStyle = userStyle;
    }

    public int getComplicationSlotId() {
        return mComplicationSlotId;
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
    public ComplicationData getComplicationData() {
        return mComplicationData;
    }

    @Nullable
    public UserStyleWireFormat getUserStyle() {
        return mUserStyle;
    }

    /** Serializes this ComplicationScreenshotParams to the specified {@link Parcel}. */
    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeParcelable(ParcelUtils.toParcelable(this), flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<ComplicationRenderParams> CREATOR =
            new Parcelable.Creator<ComplicationRenderParams>() {
                @Override
                public ComplicationRenderParams createFromParcel(Parcel source) {
                    return ParcelUtils.fromParcelable(
                            source.readParcelable(getClass().getClassLoader()));
                }

                @Override
                public ComplicationRenderParams[] newArray(int size) {
                    return new ComplicationRenderParams[size];
                }
            };
}
