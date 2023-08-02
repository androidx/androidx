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

import android.graphics.drawable.Icon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.List;

/**
 * Wire format for {@link
 * androidx.wear.watchface.style.ComplicationsUserStyleSetting.ComplicationsOption}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@VersionedParcelize
public class ComplicationsOptionWireFormat extends OptionWireFormat {
    @ParcelField(2)
    @NonNull
    public CharSequence mDisplayName;

    @ParcelField(3)
    @Nullable
    public Icon mIcon;

    // WARNING: This class is held in a list and can't change due to flaws in VersionedParcelable.

    /**
     * Great care should be taken to ensure backwards compatibility of the versioned parcelable
     * if {@link ComplicationOverlayWireFormat} is ever
     * extended.
     */
    @ParcelField(100)
    @NonNull
    public ComplicationOverlayWireFormat[] mComplicationOverlays =
            new ComplicationOverlayWireFormat[0];

    @ParcelField(value = 101, defaultValue = "null")
    @Nullable
    public List<PerComplicationTypeMargins> mComplicationOverlaysMargins;

    ComplicationsOptionWireFormat() {
    }

    public ComplicationsOptionWireFormat(
            @NonNull byte[] id,
            @NonNull CharSequence displayName,
            @Nullable Icon icon,
            @NonNull ComplicationOverlayWireFormat[] complicationOverlays,
            @Nullable List<PerComplicationTypeMargins> complicationOverlaysMargins
    ) {
        super(id);
        mDisplayName = displayName;
        mIcon = icon;
        mComplicationOverlays = complicationOverlays;
        mComplicationOverlaysMargins = complicationOverlaysMargins;
    }

    /** @deprecated Use a constructor with perComplicationTypeMargins instead. */
    @Deprecated
    public ComplicationsOptionWireFormat(
            @NonNull byte[] id,
            @NonNull CharSequence displayName,
            @Nullable Icon icon,
            @NonNull ComplicationOverlayWireFormat[]
                    complicationOverlays
    ) {
        super(id);
        mDisplayName = displayName;
        mIcon = icon;
        mComplicationOverlays = complicationOverlays;
    }
}
