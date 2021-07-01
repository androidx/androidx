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

package android.support.wearable.complications;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Holder of details of a complication provider, for use by watch faces (for example, to show the
 * current provider in settings). A
 * {@link androidx.wear.complications.ComplicationDataSourceInfoRetriever} can be used to obtain
 * instances of this class for each of a watch face's complications.
 *
 * @hide
 */
@SuppressLint("BanParcelableUsage")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ComplicationProviderInfo implements Parcelable {

    @NonNull
    public static final Creator<ComplicationProviderInfo> CREATOR =
            new Creator<ComplicationProviderInfo>() {
                @Override
                public ComplicationProviderInfo createFromParcel(Parcel source) {
                    return new ComplicationProviderInfo(source);
                }

                @Override
                public ComplicationProviderInfo[] newArray(int size) {
                    return new ComplicationProviderInfo[size];
                }
            };

    private static final String KEY_APP_NAME = "app_name";
    private static final String KEY_PROVIDER_COMPONENT_NAME = "provider_component";
    private static final String KEY_PROVIDER_NAME = "provider_name";
    private static final String KEY_PROVIDER_ICON = "provider_icon";
    private static final String KEY_PROVIDER_TYPE = "complication_type";

    @Nullable private String mAppName;
    @Nullable private String mProviderName;
    @Nullable private Icon mProviderIcon;
    @ComplicationData.ComplicationType private int mComplicationType;
    /** This field is only populate in Android R and up and it is null otherwise. */
    @Nullable private ComponentName mProviderComponentName;

    /**
     * Constructs a {@link ComplicationProviderInfo} with the details of a complication provider.
     *
     * <p>The providerComponentName field is only populated from Android R and up.
     *
     * @param appName The name of the app providing the complication
     * @param providerName The name of the complication provider within the app
     * @param providerIcon The icon for the complication provider
     * @param complicationType The type of complication provided
     * @param providerComponentName The component name of the complication provider
     */
    public ComplicationProviderInfo(
            @NonNull String appName, @NonNull String providerName, @NonNull Icon providerIcon,
            @ComplicationData.ComplicationType int complicationType,
            @Nullable ComponentName providerComponentName) {
        this.mAppName = appName;
        this.mProviderName = providerName;
        this.mProviderIcon = providerIcon;
        this.mComplicationType = complicationType;
        this.mProviderComponentName = providerComponentName;
    }

    /**
     * Constructs a {@link ComplicationProviderInfo} from details stored in a {@link Parcel}.
     */
    @SuppressWarnings("ParcelConstructor")
    public ComplicationProviderInfo(@NonNull Parcel in) {
        Bundle bundle = in.readBundle(getClass().getClassLoader());
        mAppName = bundle.getString(KEY_APP_NAME);
        mProviderName = bundle.getString(KEY_PROVIDER_NAME);
        mProviderIcon = bundle.getParcelable(KEY_PROVIDER_ICON);
        mComplicationType = bundle.getInt(KEY_PROVIDER_TYPE);
        mProviderComponentName = bundle.getParcelable(KEY_PROVIDER_COMPONENT_NAME);
    }

    /**
     * Writes this {@link ComplicationProviderInfo} to a {@link Parcel}.
     *
     * @param dest The {@link Parcel} to write to
     * @param flags Flags for writing the {@link Parcel}
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_APP_NAME, mAppName);
        bundle.putString(KEY_PROVIDER_NAME, mProviderName);
        bundle.putParcelable(KEY_PROVIDER_ICON, mProviderIcon);
        bundle.putInt(KEY_PROVIDER_TYPE, mComplicationType);
        bundle.putParcelable(KEY_PROVIDER_COMPONENT_NAME, mProviderComponentName);
        dest.writeBundle(bundle);
    }

    /** Returns the name of the application containing the complication provider. */
    @Nullable
    public String getAppName() {
        return mAppName;
    }

    /** Sets the name of the application containing the complication provider. */
    public void setAppName(@NonNull String appName) {
        mAppName = appName;
    }

    /** Returns the name of the complication provider. */
    @Nullable
    public String getProviderName() {
        return mProviderName;
    }

    /** Sets the  name of the complication provider */
    public void setProviderName(@NonNull String providerName) {
        mProviderName = providerName;
    }

    /** Returns the icon for the complication provider. */
    @Nullable
    public Icon getProviderIcon() {
        return mProviderIcon;
    }

    /** Sets the icon for the complication provider. */
    public void setProviderIcon(@NonNull Icon providerIcon) {
        mProviderIcon = providerIcon;
    }

    /** Returns the type of the complication provided by the provider. */
    public @ComplicationData.ComplicationType int getComplicationType() {
        return mComplicationType;
    }

    /** Sets the type of the complication provided by the provider. */
    public void setComplicationType(@ComplicationData.ComplicationType int complicationType) {
        mComplicationType = complicationType;
    }

    /** Returns the provider's {@link ComponentName}. */
    public @Nullable ComponentName getProviderComponentName() {
        return mProviderComponentName;
    }

    /** Sets the provider's {@link ComponentName}. */
    public void setProviderComponentName(@NonNull ComponentName providerComponentName) {
        mProviderComponentName = providerComponentName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "ComplicationProviderInfo{"
                + "appName='"
                + mAppName
                + '\''
                + ", providerName='"
                + mProviderName
                + '\''
                + ", providerIcon="
                + mProviderIcon
                + ", complicationType="
                + mComplicationType
                + ", providerComponentName="
                + mProviderComponentName
                + '}';
    }
}
