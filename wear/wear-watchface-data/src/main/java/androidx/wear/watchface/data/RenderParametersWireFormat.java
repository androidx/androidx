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
    /** */
    private static final int NO_COMPLICATION_ID = -1;

    /** Wire format for {@link androidx.wear.watchface.DrawMode}. */
    @ParcelField(1)
    int mDrawMode;

    /**
     * Optional parameter which specifies that a particular complication rather than all
     * complications should be highlighted when Layer.COMPLICATIONS is
     * LayerMode.DRAW_HIGHLIGHTED. NO_COMPLICATION_ID represents null.
     */
    @ParcelField(2)
    int mHighlightedComplicationId;

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
            @Nullable Integer highlightedComplicationId) {
        mDrawMode = drawMode;
        mLayerParameters = layerParameters;
        mHighlightedComplicationId = (highlightedComplicationId != null)
                ? highlightedComplicationId : NO_COMPLICATION_ID;
    }

    public int getDrawMode() {
        return mDrawMode;
    }

    @Nullable
    public Integer getHighlightedComplicationId() {
        return (mHighlightedComplicationId == NO_COMPLICATION_ID) ? null :
                mHighlightedComplicationId;
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

    /**
     * Wire format for Map<{@link androidx.wear.watchface.style.Layer},
     * {@link androidx.wear.watchface.LayerMode}>
     *
     * Unfortunately we can't ever add new members to this because we use it in lists and
     * VersionedParcelable isn't fully backwards compatible when new members are added to lists.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @VersionedParcelize
    @SuppressLint("BanParcelableUsage") // TODO(b/169214666): Remove Parcelable
    public static class LayerParameterWireFormat implements VersionedParcelable, Parcelable {
        /** Wire format for Map<{@link androidx.wear.watchface.style.Layer> */
        @ParcelField(1)
        int mLayer;

        /** Wire format for Map<{@link androidx.wear.watchface.LayerMode> */
        @ParcelField(2)
        int mLayerMode;

        LayerParameterWireFormat() {
        }

        public LayerParameterWireFormat(int layer, int layerMode) {
            mLayer = layer;
            mLayerMode = layerMode;
        }

        public int getLayer() {
            return mLayer;
        }

        public int getLayerMode() {
            return mLayerMode;
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

        public static final Creator<LayerParameterWireFormat> CREATOR =
                new Creator<LayerParameterWireFormat>() {
                    @Override
                    public LayerParameterWireFormat createFromParcel(Parcel source) {
                        return ParcelUtils.fromParcelable(
                                source.readParcelable(getClass().getClassLoader()));
                    }

                    @Override
                    public LayerParameterWireFormat[] newArray(int size) {
                        return new LayerParameterWireFormat[size];
                    }
                };
    }
}
