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

package androidx.camera.testing.impl.fakes;

import static androidx.camera.core.impl.UseCaseConfig.OPTION_CAPTURE_CONFIG_UNPACKER;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_DEFAULT_SESSION_CONFIG;
import static androidx.camera.core.impl.UseCaseConfig.OPTION_SESSION_CONFIG_UNPACKER;

import android.annotation.SuppressLint;
import android.hardware.camera2.CameraDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.camera.core.ExperimentalZeroShutterLag;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCapture.CaptureMode;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;

/**
 * A fake implementation of {@link UseCaseConfigFactory}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class FakeUseCaseConfigFactory implements UseCaseConfigFactory {

    @Nullable
    private CaptureType mLastRequestedCaptureType;

    /**
     * Returns the configuration for the given capture type, or <code>null</code> if the
     * configuration cannot be produced.
     */
    @NonNull
    @Override
    public Config getConfig(
            @NonNull CaptureType captureType,
            @CaptureMode int captureMode) {
        mLastRequestedCaptureType = captureType;
        MutableOptionsBundle mutableConfig = MutableOptionsBundle.create();

        SessionConfig.Builder sessionBuilder = new SessionConfig.Builder();
        sessionBuilder.setTemplateType(getSessionConfigTemplateType(captureType, captureMode));

        mutableConfig.insertOption(OPTION_DEFAULT_SESSION_CONFIG, sessionBuilder.build());

        mutableConfig.insertOption(OPTION_CAPTURE_CONFIG_UNPACKER, (config, builder) -> {});
        mutableConfig.insertOption(OPTION_SESSION_CONFIG_UNPACKER,
                new FakeSessionConfigOptionUnpacker());

        return OptionsBundle.from(mutableConfig);
    }

    @Nullable
    public CaptureType getLastRequestedCaptureType() {
        return mLastRequestedCaptureType;
    }

    /**
     * Returns the appropriate template type for a session configuration.
     */
    @SuppressLint("NullAnnotationGroup")
    @OptIn(markerClass = ExperimentalZeroShutterLag.class)
    public static int getSessionConfigTemplateType(
            @NonNull UseCaseConfigFactory.CaptureType captureType,
            @ImageCapture.CaptureMode int captureMode
    ) {
        switch (captureType) {
            case IMAGE_CAPTURE:
                return captureMode == ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
                        ? CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG :
                        CameraDevice.TEMPLATE_PREVIEW;
            case VIDEO_CAPTURE:
                return CameraDevice.TEMPLATE_RECORD;
            case STREAM_SHARING:
            case PREVIEW:
            case IMAGE_ANALYSIS:
            default:
                return CameraDevice.TEMPLATE_PREVIEW;
        }
    }
}
