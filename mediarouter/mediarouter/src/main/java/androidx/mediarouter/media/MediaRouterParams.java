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

package androidx.mediarouter.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * MediaRouterParams are used in {@link MediaRouter} to denote routing functionality and UI types.
 */
public class MediaRouterParams {
    /**
     * A dialog type used for default if not set.
     * {@link androidx.mediarouter.app.MediaRouteChooserDialog} and
     * {@link androidx.mediarouter.app.MediaRouteControllerDialog} will be shown
     */
    public static final int DIALOG_TYPE_DEFAULT = 1;

    /**
     * A dialog type supporting dynamic group.
     * Users can dynamically group and ungroup route devices via this type of route dialog when the
     * selected routes are from a {@link MediaRouteProvider} that supports dynamic group.
     */
    public static final int DIALOG_TYPE_DYNAMIC_GROUP = 2;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @IntDef({DIALOG_TYPE_DEFAULT, DIALOG_TYPE_DYNAMIC_GROUP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DialogType {}

    /**
     * Bundle key used for disabling group volume UX.
     *
     * <p>TYPE: boolean
     */
    public static final String EXTRAS_KEY_DISABLE_GROUP_VOLUME_UX =
            "androidx.mediarouter.media.MediaRouterParams.DISABLE_GROUP_VOLUME_UX";

    /**
     * Bundle key used for setting the cast icon fixed regardless of its connection state.
     *
     * <p>TYPE: boolean
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final String EXTRAS_KEY_FIXED_CAST_ICON =
            "androidx.mediarouter.media.MediaRouterParams.FIXED_CAST_ICON";

    @DialogType
    final int mDialogType;
    final boolean mOutputSwitcherEnabled;
    final boolean mTransferToLocalEnabled;
    final Bundle mExtras;

    MediaRouterParams(@NonNull Builder builder) {
        mDialogType = builder.mDialogType;
        mOutputSwitcherEnabled = builder.mOutputSwitcherEnabled;
        mTransferToLocalEnabled = builder.mTransferToLocalEnabled;

        Bundle extras = builder.mExtras;
        mExtras = extras == null ? Bundle.EMPTY : new Bundle(extras);
    }

    /**
     * Gets the media route controller dialog type.
     *
     * @see Builder#setDialogType(int)
     */
    public @DialogType int getDialogType() {
        return mDialogType;
    }

    /**
     * Gets whether the output switcher dialog is enabled.
     * <p>
     * Note that it always returns {@code false} for Android versions earlier than Android R.
     *
     * @see Builder#setOutputSwitcherEnabled(boolean)
     */
    public boolean isOutputSwitcherEnabled() {
        return mOutputSwitcherEnabled;
    }

    /**
     * Returns whether transferring media from remote to local is enabled.
     * <p>
     * Note that it always returns {@code false} for Android versions earlier than Android R.
     *
     * @see Builder#setTransferToLocalEnabled(boolean)
     */
    public boolean isTransferToLocalEnabled() {
        return mTransferToLocalEnabled;
    }

    /**
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public Bundle getExtras() {
        return mExtras;
    }

    /**
     * Builder class for {@link MediaRouterParams}.
     */
    public static final class Builder {
        @DialogType
        int mDialogType = DIALOG_TYPE_DEFAULT;
        boolean mOutputSwitcherEnabled;
        boolean mTransferToLocalEnabled;
        Bundle mExtras;

        /**
         * Constructor for builder to create {@link MediaRouterParams}.
         */
        public Builder() {}

        /**
         * Constructor for builder to create {@link MediaRouterParams} with existing
         * {@link MediaRouterParams} instance.
         *
         * @param params the existing instance to copy data from.
         */
        public Builder(@NonNull MediaRouterParams params) {
            if (params == null) {
                throw new NullPointerException("params should not be null!");
            }

            mDialogType = params.mDialogType;
            mOutputSwitcherEnabled = params.mOutputSwitcherEnabled;
            mTransferToLocalEnabled = params.mTransferToLocalEnabled;
            mExtras = params.mExtras == null ? null : new Bundle(params.mExtras);
        }


        /**
         * Sets the media route controller dialog type. Default value is
         * {@link #DIALOG_TYPE_DEFAULT}.
         * <p>
         * Note that from Android R, output switcher will be used rather than the dialog type set by
         * this method if both {@link #setOutputSwitcherEnabled(boolean)} output switcher} and
         * {@link MediaTransferReceiver media transfer feature} are enabled.
         *
         * @param dialogType the dialog type
         * @see #setOutputSwitcherEnabled(boolean)
         * @see #DIALOG_TYPE_DEFAULT
         * @see #DIALOG_TYPE_DYNAMIC_GROUP
         */
        @NonNull
        public Builder setDialogType(@DialogType int dialogType) {
            mDialogType = dialogType;
            return this;
        }

        /**
         * Sets whether output switcher dialogs are enabled. This method will be no-op for Android
         * versions earlier than Android R. Default value is {@code false}.
         * <p>
         * If set to {@code true}, and when {@link MediaTransferReceiver media transfer is enabled},
         * {@link androidx.mediarouter.app.MediaRouteButton MediaRouteButton} will show output
         * switcher when clicked, no matter what type of dialog is set by
         * {@link #setDialogType(int)}.
         * <p>
         * If set to {@code false}, {@link androidx.mediarouter.app.MediaRouteButton
         * MediaRouteButton} will show the dialog type which is set by {@link #setDialogType(int)}.
         */
        @NonNull
        public Builder setOutputSwitcherEnabled(boolean enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                mOutputSwitcherEnabled = enabled;
            }
            return this;
        }

        /**
         * Enables media can be transferred from remote (e.g. TV) to local (e.g. phone, Bluetooth).
         * Apps that enabling this feature should handle the case in their {@link
         * MediaRouter.Callback#onRouteSelected(MediaRouter, MediaRouter.RouteInfo, int) callback}
         * properly. Default value is {@code false}.
         * <p>
         * When this is enabled, {@link MediaRouter.Callback#onRouteSelected(MediaRouter,
         * MediaRouter.RouteInfo, int, MediaRouter.RouteInfo)} will be called whenever the
         * 'remote to local' transfer happens, regardless of the selector provided in
         * {@link MediaRouter#addCallback(MediaRouteSelector, MediaRouter.Callback)}.
         * <p>
         * Note: This method will be no-op for Android versions earlier than Android R. It has
         * effect only when {@link MediaTransferReceiver media transfer is enabled}.
         */
        @NonNull
        public Builder setTransferToLocalEnabled(boolean enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                mTransferToLocalEnabled = enabled;
            }
            return this;
        }

        /**
         * Set extras. Default value is {@link Bundle#EMPTY} if not set.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @NonNull
        public Builder setExtras(@Nullable Bundle extras) {
            mExtras = (extras == null) ? null : new Bundle(extras);
            return this;
        }

        /**
         * Builds the {@link MediaRouterParams} instance.
         */
        @NonNull
        public MediaRouterParams build() {
            return new MediaRouterParams(this);
        }
    }
}
