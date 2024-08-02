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
package androidx.core.app

import android.app.Activity
import androidx.core.util.Consumer

/**
 * Interface for components that can dispatch calls from [Activity.onPictureInPictureModeChanged].
 */
public interface OnPictureInPictureModeChangedProvider {
    /**
     * Add a new listener that will get a callback associated with
     * [Activity.onPictureInPictureModeChanged] with the new [PictureInPictureModeChangedInfo].
     *
     * @param listener The listener that should be called whenever
     *   [Activity.onPictureInPictureModeChanged] was called.
     */
    public fun addOnPictureInPictureModeChangedListener(
        listener: Consumer<PictureInPictureModeChangedInfo>
    )

    /**
     * Remove a previously added listener. It will not receive any future callbacks.
     *
     * @param listener The listener previously added with [addOnPictureInPictureModeChangedListener]
     *   that should be removed.
     */
    public fun removeOnPictureInPictureModeChangedListener(
        listener: Consumer<PictureInPictureModeChangedInfo>
    )
}
