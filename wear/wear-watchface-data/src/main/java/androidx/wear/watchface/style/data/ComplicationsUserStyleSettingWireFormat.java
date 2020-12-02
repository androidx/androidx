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

package androidx.wear.watchface.style.data;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.graphics.RectF;
import android.graphics.drawable.Icon;
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
import androidx.wear.complications.SystemProviders;
import androidx.wear.complications.data.ComplicationType;

import java.util.List;
import java.util.Map;

/**
 * Wire format for {@link androidx.wear.watchface.style.ComplicationsUserStyleSetting}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VersionedParcelize
public class ComplicationsUserStyleSettingWireFormat extends UserStyleSettingWireFormat {

    ComplicationsUserStyleSettingWireFormat() {
    }

    public ComplicationsUserStyleSettingWireFormat(
            @NonNull String id,
            @NonNull CharSequence displayName,
            @NonNull CharSequence description,
            @Nullable Icon icon,
            @NonNull List<OptionWireFormat> options,
            int defaultOptionIndex,
            @NonNull List<Integer> affectsLayers) {
        super(id, displayName, description, icon, options, defaultOptionIndex, affectsLayers);
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @VersionedParcelize
    @SuppressLint("BanParcelableUsage") // TODO(b/169214666): Remove Parcelable
    public static class ComplicationOverlayWireFormat implements VersionedParcelable, Parcelable {
        public static final int ENABLED_UNKNOWN = -1;
        public static final int ENABLED_YES = 1;
        public static final int ENABLED_NO = 0;
        public static final int NO_DEFAULT_PROVIDER_TYPE = -1;

        @ParcelField(1)
        public int mComplicationId;

        /**
         * VersionedParcelable doesn't support boxed Boolean so we set this to one of
         * ENABLED_UNKNOWN, ENABLED_YES, ENABLED_NO.
         */
        @ParcelField(2)
        public int mEnabled;

        @ParcelField(3)
        @Nullable
        public Map<ComplicationType, RectF> mPerComplicationTypeBounds;

        @ParcelField(4)
        @Nullable
        public int[] mSupportedTypes;

        @ParcelField(5)
        @Nullable
        public List<ComponentName> mDefaultProviders;

        /**
         * VersionedParcelable doesn't support boxed Integer, but that's OK, this is only valid when
         * mDefaultProviders is non null.
         */
        @ParcelField(6)
        @SystemProviders.ProviderId
        public int mSystemProviderFallback;

        /**
         * VersionedParcelable doesn't support boxed Integer so NO_DEFAULT_PROVIDER_TYPE is used to
         * represent null.
         */
        @ParcelField(7)
        @ComplicationData.ComplicationType
        public int mDefaultProviderType;

        ComplicationOverlayWireFormat() {}

        public ComplicationOverlayWireFormat(
                int complicationId,
                @Nullable Boolean enabled,
                @Nullable Map<ComplicationType, RectF> perComplicationTypeBounds,
                @Nullable int[] supportedTypes,
                @Nullable List<ComponentName> defaultProviders,
                @Nullable Integer systemProviderFallback,
                @Nullable @ComplicationData.ComplicationType Integer defaultProviderType
        ) {
            mComplicationId = complicationId;
            if (enabled != null) {
                mEnabled = enabled ? ENABLED_YES : ENABLED_NO;
            } else {
                mEnabled = ENABLED_UNKNOWN;
            }
            mPerComplicationTypeBounds = perComplicationTypeBounds;
            mSupportedTypes = supportedTypes;
            mDefaultProviders = defaultProviders;
            if (systemProviderFallback != null) {
                mSystemProviderFallback = systemProviderFallback;
            }
            mDefaultProviderType =
                    (defaultProviderType != null) ? defaultProviderType : NO_DEFAULT_PROVIDER_TYPE;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        /** Serializes this UserStyleWireFormat to the specified {@link Parcel}. */
        @Override
        public void writeToParcel(@NonNull Parcel parcel, int flags) {
            parcel.writeParcelable(ParcelUtils.toParcelable(this), flags);
        }

        public static final Parcelable.Creator<ComplicationOverlayWireFormat> CREATOR =
                new Parcelable.Creator<ComplicationOverlayWireFormat>() {
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

    /**
     * Wire format for {@link
     * androidx.wear.watchface.style.ComplicationsUserStyleSetting.ComplicationsOption}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @VersionedParcelize
    public static class ComplicationsOptionWireFormat extends OptionWireFormat {
        @ParcelField(2)
        @NonNull
        public CharSequence mDisplayName;

        @ParcelField(3)
        @Nullable
        public Icon mIcon;

        /**
         * Great care should be taken to ensure backwards compatibility of the versioned parcelable
         * if {@link ComplicationOverlayWireFormat} is ever extended.
         */
        @ParcelField(100)
        @NonNull
        public ComplicationOverlayWireFormat[] mComplicationOverlays =
                new ComplicationOverlayWireFormat[0];

        ComplicationsOptionWireFormat() {
        }

        public ComplicationsOptionWireFormat(
                @NonNull String id,
                @NonNull CharSequence displayName,
                @Nullable Icon icon,
                @NonNull ComplicationOverlayWireFormat[] complicationOverlays
        ) {
            super(id);
            mDisplayName = displayName;
            mIcon = icon;
            mComplicationOverlays = complicationOverlays;
        }
    }
}
