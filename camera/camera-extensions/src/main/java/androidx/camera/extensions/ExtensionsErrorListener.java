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

package androidx.camera.extensions;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;

/** Listener called when any extensions error occurs.
 *
 * @deprecated Currently, this interface is only used to monitor whether a {@link Preview} or
 * {@link ImageCapture} is lacking when enabling extension modes. CameraX will automatically add
 * an extra {@link Preview} or {@link ImageCapture} to make the extension functions work well.
 * After that, no error will be reported via this interface.
 */
@Deprecated
public interface ExtensionsErrorListener {
    /** Types of error when extensions function is enabled.*/
    enum ExtensionsErrorCode {
        /** Unknown error. */
        UNKNOWN,
        /** Preview extension is required to enable when ImageCapture extension is enabled. */
        PREVIEW_EXTENSION_REQUIRED,
        /** ImageCapture extension is required to enable when Preview extension is enabled. */
        IMAGE_CAPTURE_EXTENSION_REQUIRED,
        /** Mismatched ImageCapture/Preview extensions are enabled. */
        MISMATCHED_EXTENSIONS_ENABLED
    }

    /**
     * This will be called when any extensions error occurs.
     *
     * @param errorCode error code in {@link ExtensionsErrorCode}
     */
    void onError(@NonNull ExtensionsErrorCode errorCode);
}
