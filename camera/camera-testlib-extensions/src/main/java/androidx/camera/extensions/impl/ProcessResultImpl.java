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

package androidx.camera.extensions.impl;

import android.annotation.SuppressLint;
import android.hardware.camera2.CaptureResult;
import android.util.Pair;

import java.util.List;

/**
 * Allows clients to receive information about the capture result values of processed frames.
 *
 * @since 1.3
 */
@SuppressLint("UnknownNullness")
public interface ProcessResultImpl {
    /**
     * Capture result callback that needs to be called when the process capture results are
     * ready as part of frame post-processing.
     *
     * @param shutterTimestamp     The shutter time stamp of the processed frame.
     * @param result               Key value pairs for all supported capture results. Do note that
     *                             if results 'android.jpeg.quality' and 'android.jpeg.orientation'
     *                             are present in the process capture input results, then the values
     *                             must also be passed as part of this callback. Both Camera2 and
     *                             CameraX guarantee that those two settings and results are always
     *                             supported and applied by the corresponding framework.
     */
    void onCaptureCompleted(long shutterTimestamp, List<Pair<CaptureResult.Key, Object>> result);
}
