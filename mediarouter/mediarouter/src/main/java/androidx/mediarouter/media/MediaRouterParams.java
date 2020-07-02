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
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final String EXTRAS_KEY_TEST_PRIVATE_UI =
            "androidx.mediarouter.media.MediaRouterParams.TEST_PRIVATE_UI";

    private int mDialogType = DIALOG_TYPE_DEFAULT;
    private boolean mOutputSwitcherEnabled = false;
    private Bundle mExtras = Bundle.EMPTY;

    /**
     * A default constructor for MediaRouterParams.
     */
    public MediaRouterParams() {
    }

    /**
     * A copy constructor for MediaRouterParams.
     */
    public MediaRouterParams(@NonNull MediaRouterParams params) {
        if (params == null) {
            throw new NullPointerException("params should not be null!");
        }

        mDialogType = params.mDialogType;
        mOutputSwitcherEnabled = params.mOutputSwitcherEnabled;
        mExtras = params.mExtras == null ? Bundle.EMPTY : new Bundle(params.mExtras);
    }

    /**
     * Gets the media route controller dialog type. Default value is {@link #DIALOG_TYPE_DEFAULT}
     * if not set.
     */
    public @DialogType int getDialogType() {
        return mDialogType;
    }

    /**
     * Sets the media route controller dialog type.
     * <p>
     * Note that from Android R, output switcher will be used rather than the dialog type set by
     * this method if both {@link #setOutputSwitcherEnabled(boolean) output switcher} and
     * {@link MediaTransferReceiver media transfer feature} are enabled.
     *
     * @param dialogType the dialog type
     * @see #setOutputSwitcherEnabled(boolean)
     * @see #DIALOG_TYPE_DEFAULT
     * @see #DIALOG_TYPE_DYNAMIC_GROUP
     */
    public void setDialogType(@DialogType int dialogType) {
        mDialogType = dialogType;
    }

    /**
     * Sets whether output switcher dialogs are enabled. This method will be no-op for Android
     * versions earlier than Android R.
     * <p>
     * If set to {@code true}, and when {@link MediaTransferReceiver media transfer is enabled},
     * {@link androidx.mediarouter.app.MediaRouteButton MediaRouteButton} will show output
     * switcher when clicked, no matter what type of dialog is set by {@link #setDialogType(int)}.
     * <p>
     * If set to {@code false}, {@link androidx.mediarouter.app.MediaRouteButton MediaRouteButton}
     * will show the dialog type which is set by {@link #setDialogType(int)}.
     *
     * @see #isOutputSwitcherEnabled()
     */
    public void setOutputSwitcherEnabled(boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mOutputSwitcherEnabled = enabled;
        }
    }

    /**
     * Gets whether the output switcher dialog is enabled. Default value is {@code false} if not
     * set.
     * <p>
     * Note that it always returns {@code false} for Android versions earlier than Android R.
     *
     * @see #setOutputSwitcherEnabled(boolean)
     */
    public boolean isOutputSwitcherEnabled() {
        return mOutputSwitcherEnabled;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setExtras(@Nullable Bundle extras) {
        mExtras = (extras == null) ? Bundle.EMPTY : new Bundle(extras);
    }

    /**
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public Bundle getExtras() {
        return mExtras;
    }
}
