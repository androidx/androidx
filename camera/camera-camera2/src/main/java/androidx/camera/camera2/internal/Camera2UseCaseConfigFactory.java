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

package androidx.camera.camera2.internal;

import static androidx.camera.core.impl.ImageOutputConfig.OPTION_MAX_RESOLUTION;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_TARGET_ROTATION;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_CAPTURE_CONFIG_UNPACKER;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_DEFAULT_CAPTURE_CONFIG;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_DEFAULT_SESSION_CONFIG;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_SESSION_CONFIG_UNPACKER;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_ZSL_DISABLED;

import android.content.Context;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCapture.CaptureMode;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;

/**
 * Implementation of UseCaseConfigFactory to provide the default camera2 configurations for use
 * cases.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class Camera2UseCaseConfigFactory implements UseCaseConfigFactory {
    final DisplayInfoManager mDisplayInfoManager;

    public Camera2UseCaseConfigFactory(@NonNull Context context) {
        mDisplayInfoManager = DisplayInfoManager.getInstance(context);
    }

    /**
     * Returns the configuration for the given capture type, or <code>null</code> if the
     * configuration cannot be produced.
     */
    @NonNull
    @Override
    public Config getConfig(
            @NonNull CaptureType captureType,
            @CaptureMode int captureMode) {
        final MutableOptionsBundle mutableConfig = MutableOptionsBundle.create();

        SessionConfig.Builder sessionBuilder = new SessionConfig.Builder();
        sessionBuilder.setTemplateType(
                TemplateTypeUtil.getSessionConfigTemplateType(captureType, captureMode));

        mutableConfig.insertOption(OPTION_DEFAULT_SESSION_CONFIG, sessionBuilder.build());

        mutableConfig.insertOption(OPTION_SESSION_CONFIG_UNPACKER,
                Camera2SessionOptionUnpacker.INSTANCE);

        CaptureConfig.Builder captureBuilder = new CaptureConfig.Builder();
        captureBuilder.setTemplateType(
                TemplateTypeUtil.getCaptureConfigTemplateType(captureType, captureMode));
        mutableConfig.insertOption(OPTION_DEFAULT_CAPTURE_CONFIG, captureBuilder.build());

        // Only CAPTURE_TYPE_IMAGE_CAPTURE has its own ImageCaptureOptionUnpacker. Other
        // capture types all use the standard Camera2CaptureOptionUnpacker.
        mutableConfig.insertOption(OPTION_CAPTURE_CONFIG_UNPACKER,
                captureType == CaptureType.IMAGE_CAPTURE ? ImageCaptureOptionUnpacker.INSTANCE
                        : Camera2CaptureOptionUnpacker.INSTANCE);

        if (captureType == CaptureType.PREVIEW) {
            Size previewSize = mDisplayInfoManager.getPreviewSize();
            mutableConfig.insertOption(OPTION_MAX_RESOLUTION, previewSize);
        }

        // The default rotation value should be determined by the max non-state-off display.
        // Calling getMaxSizeDisplay() function with parameter `true` can skips the displays with
        // off state.
        int targetRotation = mDisplayInfoManager.getMaxSizeDisplay(true).getRotation();
        mutableConfig.insertOption(OPTION_TARGET_ROTATION, targetRotation);

        if (captureType == CaptureType.VIDEO_CAPTURE || captureType == CaptureType.STREAM_SHARING) {
            mutableConfig.insertOption(OPTION_ZSL_DISABLED, true);
        }

        return OptionsBundle.from(mutableConfig);
    }
}
