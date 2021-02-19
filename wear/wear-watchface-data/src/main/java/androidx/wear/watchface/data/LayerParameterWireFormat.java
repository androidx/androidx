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
public class LayerParameterWireFormat implements VersionedParcelable, Parcelable {
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
