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
     * A dialog type that shows the Output Switcher.
     * Use this dialog type to provide consistent casting UX with Android framework.
     * <p>
     * On devices that do not support API level 30 or higher, {@link #DIALOG_TYPE_DEFAULT) would
     * be used.
     */
    public static final int DIALOG_TYPE_OUTPUT_SWITCHER = 3;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @IntDef({DIALOG_TYPE_DEFAULT, DIALOG_TYPE_DYNAMIC_GROUP, DIALOG_TYPE_OUTPUT_SWITCHER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DialogType {}

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final String EXTRAS_KEY_TEST_PRIVATE_UI =
            "androidx.mediarouter.media.MediaRouterParams.TEST_PRIVATE_UI";

    private int mDialogType = DIALOG_TYPE_DEFAULT;
    private Bundle mExtras = Bundle.EMPTY;

    /**
     * A default constructor for MediaRouterParams.
     */
    public MediaRouterParams() {
    }

    /**
     * Gets the media route controller dialog type.
     */
    public @DialogType int getDialogType() {
        return mDialogType;
    }

    /**
     * Sets the media route controller dialog type.
     *
     * @param dialogType
     */
    public void setDialogType(@DialogType int dialogType) {
        mDialogType = dialogType;
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
