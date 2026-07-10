/*
 * Copyright 2025 The Android Open Source Project
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

import android.app.PictureInPictureParams

/** Provider interface to support PiP (Picture-in-Picture) functionalities. */
public interface PictureInPictureProvider :
    OnPictureInPictureModeChangedProvider,
    OnPictureInPictureUiStateChangedProvider,
    OnUserLeaveHintProvider {
    /**
     * Available since API 24 in the framework Activity class, puts the activity in
     * picture-in-picture mode if possible in the current system state. Any prior calls to
     * [setPictureInPictureParams] will still apply when entering picture-in-picture through this
     * call.
     */
    public fun enterPictureInPictureMode(params: PictureInPictureParamsCompat)

    /**
     * Available since API 26 in the framework Activity class, updates the properties of the
     * picture-in-picture activity, or sets it to be used later when [enterPictureInPictureMode] is
     * called
     *
     * @param params [PictureInPictureParams] to use for picture-in-picture mode.
     */
    public fun setPictureInPictureParams(params: PictureInPictureParamsCompat)
}
