/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.core.app;

import android.app.Activity;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Class that encapsulates the information that is delivered when
 * {@link Activity#onPictureInPictureModeChanged} is dispatched to a
 * {@link OnPictureInPictureModeChangedProvider}.
 */
public final class PictureInPictureModeChangedInfo {
    private final boolean mIsInPictureInPictureMode;
    private final Configuration mNewConfig;

    /**
     * Construct an instance that only contains the new picture-in-picture mode.
     *
     * @param isInPictureInPictureMode True if the activity is in picture-in-picture mode.
     */
    public PictureInPictureModeChangedInfo(boolean isInPictureInPictureMode) {
        mIsInPictureInPictureMode = isInPictureInPictureMode;
        mNewConfig = null;
    }

    /**
     * Construct an instance that contains the new picture-in-picture mode and the new
     * configuration with the new picture-in-picture mode applied.
     *
     * @param isInPictureInPictureMode True if the activity is in picture-in-picture mode.
     * @param newConfig The new configuration of the activity with the state
     * {@param isInPictureInPictureMode}.
     */
    @RequiresApi(26)
    public PictureInPictureModeChangedInfo(boolean isInPictureInPictureMode,
            @NonNull Configuration newConfig) {
        mIsInPictureInPictureMode = isInPictureInPictureMode;
        mNewConfig = newConfig;
    }

    /**
     * Gets the new picture-in-picture mode.
     *
     * @return True if the activity is in picture-in-picture mode.
     */
    public boolean isInPictureInPictureMode() {
        return mIsInPictureInPictureMode;
    }

    /**
     * Gets the new {@link Configuration} of the with activity with the state
     * {@link #isInPictureInPictureMode()} applied.
     *
     * Note that this is only valid on devices that are running API 26
     * ({@link android.os.Build.VERSION_CODES#O}) or higher.
     *
     * @return The new configuration of the activity with the state
     * {@link #isInPictureInPictureMode()}.
     * @throws IllegalStateException if the new {@link Configuration} is not available (i.e.,
     * you are running on a device less that {@link android.os.Build.VERSION_CODES#O} which is
     * when this information first became available).
     */
    @RequiresApi(26)
    @NonNull
    public Configuration getNewConfig() {
        if (mNewConfig == null) {
            throw new IllegalStateException("PictureInPictureModeChangedInfo must be constructed "
                    + "with the constructor that takes a Configuration to call getNewConfig(). "
                    + "Are you running on an API 26 or higher device that makes this "
                    + "information available?");
        }
        return mNewConfig;
    }
}
