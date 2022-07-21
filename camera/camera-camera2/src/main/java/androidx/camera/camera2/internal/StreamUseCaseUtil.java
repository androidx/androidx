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

package androidx.camera.camera2.internal;

import android.hardware.camera2.CameraDevice;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.compat.params.OutputConfigurationCompat;
import androidx.camera.core.impl.ImageAnalysisConfig;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.VideoCaptureConfig;

import java.util.Collection;

/**
 * A class that contains utility methods for stream use case.
 */
public final class StreamUseCaseUtil {

    private StreamUseCaseUtil() {
    }

    /**
     * Returns the appropriate stream use case for a capture session based on the attached
     * CameraX use cases. If API level is below 33, return
     * {@value OutputConfigurationCompat#STREAM_USE_CASE_NONE}. If use cases are empty or is ZSL,
     * return DEFAULT. Otherwise, return PREVIEW_VIDEO_STILL for ImageCapture + VideoCapture;
     * return STILL_CAPTURE for ImageCapture; return VIDEO_RECORD for VideoCapture; return
     * VIEW_FINDER for Preview only.
     *
     * @param useCaseConfigs collection of all attached CameraX use cases for this capture session
     * @param sessionConfigs collection of all session configs for this capture session
     * @return the appropriate stream use case for this capture session
     */
    public static long getStreamUseCaseFromUseCaseConfigs(
            @NonNull Collection<UseCaseConfig<?>> useCaseConfigs,
            @NonNull Collection<SessionConfig> sessionConfigs) {
        if (Build.VERSION.SDK_INT < 33) {
            return OutputConfigurationCompat.STREAM_USE_CASE_NONE;
        }
        if (useCaseConfigs.isEmpty()) {
            //If the collection is empty, return default case.
            //TODO: b/237337336 Return SCALER_AVAILABLE_STREAM_USE_CASE_DEFAULT here
            return OutputConfigurationCompat.STREAM_USE_CASE_NONE;
        } else {
            for (SessionConfig sessionConfig : sessionConfigs) {
                if (sessionConfig.getTemplateType() == CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG) {
                    //If is ZSL, return default case.
                    //TODO: b/237337336 Return SCALER_AVAILABLE_STREAM_USE_CASE_DEFAULT here
                    return OutputConfigurationCompat.STREAM_USE_CASE_NONE;
                }
            }
            boolean hasImageCapture = false, hasVideoCapture = false, hasPreview = false;
            for (UseCaseConfig<?> useCaseConfig : useCaseConfigs) {
                if (useCaseConfig instanceof ImageAnalysisConfig) {
                    //If contains analysis use case, return default case.
                    //TODO: b/237337336 Return SCALER_AVAILABLE_STREAM_USE_CASE_DEFAULT here
                    return OutputConfigurationCompat.STREAM_USE_CASE_NONE;
                }

                if (useCaseConfig instanceof PreviewConfig) {
                    hasPreview = true;
                    continue;
                }

                if (useCaseConfig instanceof ImageCaptureConfig) {
                    if (hasVideoCapture) {
                        // If has both image and video capture, return preview video still case.
                        //TODO: b/237337336 Return
                        // SCALER_AVAILABLE_STREAM_USE_CASE_PREVIEW_VIDEO_STILL here
                        return OutputConfigurationCompat.STREAM_USE_CASE_NONE;
                    }
                    hasImageCapture = true;
                    continue;

                }

                if (useCaseConfig instanceof VideoCaptureConfig) {
                    if (hasImageCapture) {
                        // If has both image and video capture, return preview video still case.
                        //TODO: b/237337336 Return
                        // SCALER_AVAILABLE_STREAM_USE_CASE_PREVIEW_VIDEO_STILL here
                        return OutputConfigurationCompat.STREAM_USE_CASE_NONE;
                    }
                    hasVideoCapture = true;
                    continue;

                }
            }
            if (!hasPreview) {
                // If doesn't contain preview, we are not sure what's the situation. Return
                // default case.
                //TODO: b/237337336 Return SCALER_AVAILABLE_STREAM_USE_CASE_DEFAULT here
                return OutputConfigurationCompat.STREAM_USE_CASE_NONE;
            }

            if (hasImageCapture) {
                // If contains image capture, return still capture case.
                //TODO: b/237337336 Return SCALER_AVAILABLE_STREAM_USE_CASE_STILL_CAPTURE here
                return OutputConfigurationCompat.STREAM_USE_CASE_NONE;
            } else if (hasVideoCapture) {
                // If contains video capture, return video record case.
                //TODO: b/237337336 Return SCALER_AVAILABLE_STREAM_USE_CASE_VIDEO_RECORD here
                return OutputConfigurationCompat.STREAM_USE_CASE_NONE;
            } else {
                // If contains only preview, return view finder case.
                //TODO: b/237337336 Return SCALER_AVAILABLE_STREAM_USE_CASE_VIEW_FINDER here
                return OutputConfigurationCompat.STREAM_USE_CASE_NONE;
            }
        }
    }
}
