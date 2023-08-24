/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat.workaround;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.quirk.Preview3AThreadCrash;
import androidx.camera.core.impl.Quirks;

/**
 * Indicate the required actions when going to switch CameraCaptureSession.
 *
 * @see Preview3AThreadCrash
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class SessionResetPolicy {

    private final boolean mNeedAbortCapture;

    public SessionResetPolicy(@NonNull Quirks deviceQuirks) {
        mNeedAbortCapture = deviceQuirks.contains(Preview3AThreadCrash.class);
    }

    /**
     * @return true if it needs to call abortCapture before the CameraCaptureSession is closed.
     */
    public boolean needAbortCapture() {
        return mNeedAbortCapture;
    }
}
