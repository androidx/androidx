/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core.impl;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * A report of failed capture for a single image capture.
 *
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class CameraCaptureFailure {

    private final Reason mReason;

    public CameraCaptureFailure(@NonNull Reason reason) {
        mReason = reason;
    }

    /**
     * Determine why the request was dropped, whether due to an error or to a user action.
     *
     * @return int The reason code.
     * @see CameraCaptureFailure.Reason#ERROR
     */
    @NonNull
    public Reason getReason() {
        return mReason;
    }

    /**
     * The capture result has been dropped this frame only due to an error in the framework.
     *
     * @see #getReason()
     */
    public enum Reason {
        ERROR,
    }
}
