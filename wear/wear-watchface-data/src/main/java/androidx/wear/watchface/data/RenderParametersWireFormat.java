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

/**
 * Wire format for {@link androidx.wear.watchface.RenderParameters}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VersionedParcelize
@SuppressLint("BanParcelableUsage") // TODO(b/169214666): Remove Parcelable
public class RenderParametersWireFormat implements VersionedParcelable, Parcelable {
    /** Used when {@link androidx.wear.watchface.RenderParameters#getHighlightLayer} is `null`. */
    public static int ELEMENT_TYPE_NONE = 0;

    /**
     * Used when {@link androidx.wear.watchface.RenderParameters#getHighlightLayer} is
     * {@link androidx.wear.watchface.HighlightedElement.AllComplications}.
     */
    public static int ELEMENT_TYPE_ALL_COMPLICATIONS = 1;

    /**
     * Used when {@link androidx.wear.watchface.RenderParameters#getHighlightLayer} is
     * {@link androidx.wear.watchface.HighlightedElement.Complication}.
     */
    public static int ELEMENT_TYPE_COMPLICATION = 2;

    /**
     * Used when {@link androidx.wear.watchface.RenderParameters#getHighlightLayer} is
     * {@link androidx.wear.watchface.HighlightedElement.UserStyle}.
     */
    public static int ELEMENT_TYPE_USER_STYLE = 3;

    /** Wire format for {@link androidx.wear.watchface.DrawMode}. */
    @ParcelField(1)
    int mDrawMode;

    /**
     * A bitfield where each bit represents one layer in the set of
     * {@link androidx.wear.watchface.style.WatchFaceLayer}s.
     */
    @ParcelField(2)
    int mWatchFaceLayerSetBitfield;

    /**
     * One of {@link #ELEMENT_TYPE_NONE}, {@link #ELEMENT_TYPE_ALL_COMPLICATIONS},
     * {@link #ELEMENT_TYPE_COMPLICATION} or {@link #ELEMENT_TYPE_USER_STYLE}.
     */
    @ParcelField(3)
    int mElementType;

    /**
     * Optional ID of a single complication slot to render highlighted, only used with
     * {@link #ELEMENT_TYPE_COMPLICATION}.
     */
    @ParcelField(4)
    int mElementComplicationSlotId;

    /**
     * Optional UserStyleSetting to render highlighted, only non-null with
     * {@link #ELEMENT_TYPE_USER_STYLE}.
     */
    @ParcelField(5)
    @Nullable
    String mElementUserStyleSettingId;

    /**
     * Specifies the tint for the highlighted element. Only used when {@link #mElementType} isn't
     * {@link #ELEMENT_TYPE_NONE}.
     */
    @ParcelField(6)
    @ColorInt
    int mHighlightTint;

    /**
     * Specifies the tint for everything else. Only used when {@link #mElementType} isn't
     * {@link #ELEMENT_TYPE_NONE}.
     */
    @ParcelField(7)
    @ColorInt
    int mBackgroundTint;

    RenderParametersWireFormat() {
    }

    public RenderParametersWireFormat(
            int drawMode,
            int watchFaceLayerSetBitfield,
            int elementType,
            int complicationSlotId,
            @Nullable String elementUserStyleSettingId,
            @ColorInt int highlightTint,
            @ColorInt int backgroundTint) {
        mDrawMode = drawMode;
        mWatchFaceLayerSetBitfield = watchFaceLayerSetBitfield;
        mElementType = elementType;
        mElementComplicationSlotId = complicationSlotId;
        mElementUserStyleSettingId = elementUserStyleSettingId;
        mHighlightTint = highlightTint;
        mBackgroundTint = backgroundTint;
        if (elementType == ELEMENT_TYPE_USER_STYLE) {
            if (elementUserStyleSettingId == null) {
                throw new IllegalArgumentException(
                        "selectedUserStyleSettingId must be non-null when elementType is "
                                + "ELEMENT_TYPE_USER_STYLE");
            }
        } else {
            if (elementUserStyleSettingId != null) {
                throw new IllegalArgumentException(
                        "selectedUserStyleSettingId must be null when elementType isn't "
                                + "ELEMENT_TYPE_USER_STYLE");
            }
        }
    }

    public int getDrawMode() {
        return mDrawMode;
    }

    public int getWatchFaceLayerSetBitfield() {
        return mWatchFaceLayerSetBitfield;
    }

    public int getElementType() {
        return mElementType;
    }

    public int getElementComplicationSlotId() {
        return mElementComplicationSlotId;
    }

    @Nullable
    public String getElementUserStyleSettingId() {
        return mElementUserStyleSettingId;
    }

    @ColorInt
    public int getHighlightTint() {
        return mHighlightTint;
    }

    @ColorInt
    public int getBackgroundTint() {
        return mBackgroundTint;
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
                    return ParcelUtils.fromParcelable(
                            source.readParcelable(getClass().getClassLoader()));
                }

                @Override
                public RenderParametersWireFormat[] newArray(int size) {
                    return new RenderParametersWireFormat[size];
                }
            };

}
