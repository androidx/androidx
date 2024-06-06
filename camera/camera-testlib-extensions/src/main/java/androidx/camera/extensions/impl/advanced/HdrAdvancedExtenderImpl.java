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

package androidx.camera.extensions.impl.advanced;

import android.graphics.ImageFormat;

/**
 * A sample HDR implementation for testing long processing capture. It is capable of outputting
 * the postview(JPEG format) and the process progress event. ImageAnalysis is not supported.
 *
 * @since 1.2
 */
public class HdrAdvancedExtenderImpl extends ConfigurableAdvancedExtenderImpl {
    public HdrAdvancedExtenderImpl() {
        super(/* longDurationCapture */ false,
                /* postviewFormat */ ImageFormat.JPEG,
                /* invokeOnCaptureCompleted */ true);
    }

    /**
     * This method is used to check if test lib is running. If OEM implementation exists, invoking
     * this method will throw {@link NoSuchMethodError}. This can be used to determine if OEM
     * implementation is used or not.
     */
    public static void checkTestlibRunning() {}
}
