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

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.List;

/**
 * Wire format for {@link androidx.wear.watchface.RenderParameters}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VersionedParcelize
@SuppressLint("BanParcelableUsage") // TODO(b/169214666): Remove Parcelable
public class RenderParametersWireFormat implements VersionedParcelable, Parcelable {
    private static final int NO_COMPLICATION_ID = -1;

    /** Wire format for {@link androidx.wear.watchface.DrawMode}. */
    @ParcelField(1)
    int mDrawMode;

    /**
     * Optional parameter which if non null specifies that a particular complication, should be
     * drawn with a special highlight to indicate it's been selected.
     */
    @ParcelField(2)
    int mSelectedComplicationId;

    /**
     * Specifies the tint for any outlined element.
     */
    @ParcelField(3)
    int mOutlineTint;

    /**
     * Wire format for Map<{@link androidx.wear.watchface.style.Layer},
     * {@link androidx.wear.watchface.LayerMode}>.
     *
     * This list needs to go last because VersionedParcelable has a design flaw, if the format
     * changes the reader can't determine the correct size of the list and data afterwards would get
     * corrupted. We try to avoid this by putting the list last.
     */
    @NonNull
    @ParcelField(100)
    List<LayerParameterWireFormat> mLayerParameters;

    RenderParametersWireFormat() {
    }

    public RenderParametersWireFormat(
            int drawMode,
            @NonNull List<LayerParameterWireFormat> layerParameters,
            @Nullable Integer selectedComplicationId,
            @ColorInt int outlineTint) {
        mDrawMode = drawMode;
        mLayerParameters = layerParameters;
        mSelectedComplicationId = (selectedComplicationId != null)
                ? selectedComplicationId : NO_COMPLICATION_ID;
        mOutlineTint = outlineTint;
    }

    public int getDrawMode() {
        return mDrawMode;
    }

    @Nullable
    public Integer getSelectedComplicationId() {
        return (mSelectedComplicationId == NO_COMPLICATION_ID) ? null :
                mSelectedComplicationId;
    }

    @ColorInt
    public int getOutlineTint() {
        return mOutlineTint;
    }

    @NonNull
    public List<LayerParameterWireFormat> getLayerParameters() {
        return mLayerParameters;
    }

    /** Serializes this IndicatorState to the specified {@link Parcel}. */
    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeParcelable(ParcelUtils.toParcelable(this), flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<RenderParametersWireFormat> CREATOR =
            new Parcelable.Creator<RenderParametersWireFormat>() {
                @Override
                public RenderParametersWireFormat createFromParcel(Parcel source) {
                    return RenderParametersWireFormatParcelizer.read(
                            ParcelUtils.fromParcelable(source.readParcelable(
                                    getClass().getClassLoader())));
                }

                @Override
                public RenderParametersWireFormat[] newArray(int size) {
                    return new RenderParametersWireFormat[size];
                }
            };

}
