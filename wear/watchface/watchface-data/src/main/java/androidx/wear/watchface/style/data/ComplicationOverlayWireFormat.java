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

package androidx.wear.watchface.style.data;

import android.annotation.SuppressLint;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Map;

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VersionedParcelize
@SuppressLint("BanParcelableUsage") // TODO(b/169214666): Remove Parcelable
public class ComplicationOverlayWireFormat implements VersionedParcelable, Parcelable {
    public static final int ENABLED_UNKNOWN = -1;
    public static final int ENABLED_YES = 1;
    public static final int ENABLED_NO = 0;
    public static final long NULL_ACCESSIBILITY_TRAVERSAL_INDEX = 0x100000000L;

    @ParcelField(1)
    public int mComplicationSlotId;

    /**
     * VersionedParcelable doesn't support boxed Boolean so we set this to one of
     * ENABLED_UNKNOWN, ENABLED_YES, ENABLED_NO.
     */
    @ParcelField(2)
    public int mEnabled;

    @ParcelField(3)
    @Nullable
    public Map<Integer, RectF> mPerComplicationTypeBounds;

    /** Ideally this would be Integer but VersionedParcelable doesn't support that. */
    @ParcelField(4)
    long mAccessibilityTraversalIndex;

    ComplicationOverlayWireFormat() {
    }

    public ComplicationOverlayWireFormat(
            int complicationSlotId,
            @Nullable Boolean enabled,
            @Nullable Map<Integer, RectF> perComplicationTypeBounds,
            @Nullable Integer accessibilityTraversalIndex
    ) {
        mComplicationSlotId = complicationSlotId;
        if (enabled != null) {
            mEnabled = enabled ? ENABLED_YES : ENABLED_NO;
        } else {
            mEnabled = ENABLED_UNKNOWN;
        }
        mPerComplicationTypeBounds = perComplicationTypeBounds;
        if (accessibilityTraversalIndex == null) {
            mAccessibilityTraversalIndex = NULL_ACCESSIBILITY_TRAVERSAL_INDEX;
        } else {
            mAccessibilityTraversalIndex = accessibilityTraversalIndex;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Returns the optional override to the accessibilityTraversalIndex used for sorting
     * ContentDescriptionLabels. See ComplicationOverlay for details.
     */
    @Nullable
    public Integer getAccessibilityTraversalIndex() {
        if (mAccessibilityTraversalIndex == NULL_ACCESSIBILITY_TRAVERSAL_INDEX) {
            return null;
        } else {
            return (int) mAccessibilityTraversalIndex;
        }
    }

    /** Serializes this UserStyleWireFormat to the specified {@link Parcel}. */
    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeParcelable(ParcelUtils.toParcelable(this), flags);
    }

    public static final Creator<ComplicationOverlayWireFormat> CREATOR =
            new Creator<ComplicationOverlayWireFormat>() {
                @Override
                public ComplicationOverlayWireFormat createFromParcel(Parcel source) {
                    return ParcelUtils.fromParcelable(
                            source.readParcelable(getClass().getClassLoader()));
                }

                @Override
                public ComplicationOverlayWireFormat[] newArray(int size) {
                    return new ComplicationOverlayWireFormat[size];
                }
            };
}
