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

package androidx.camera.camera2.internal;

import android.content.Context;
import android.hardware.camera2.CameraDevice;
import android.util.Log;
import android.util.Rational;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraX;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.ConfigProvider;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.SessionConfig;

/**
 * Provides defaults for {@link PreviewConfig} in the Camera2 implementation.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public final class PreviewConfigProvider implements ConfigProvider<PreviewConfig> {
    private static final String TAG = "PreviewConfigProvider";
    private static final Rational DEFAULT_ASPECT_RATIO_4_3 = new Rational(4, 3);
    private static final Rational DEFAULT_ASPECT_RATIO_3_4 = new Rational(3, 4);

    private final CameraFactory mCameraFactory;
    private final WindowManager mWindowManager;

    public PreviewConfigProvider(CameraFactory cameraFactory, Context context) {
        mCameraFactory = cameraFactory;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    @Override
    public PreviewConfig getConfig(@Nullable Integer lensFacing) {
        Preview.Builder builder = Preview.Builder.fromConfig(
                Preview.DEFAULT_CONFIG.getConfig(lensFacing));

        // SessionConfig containing all intrinsic properties needed for Preview
        SessionConfig.Builder sessionBuilder = new SessionConfig.Builder();
        sessionBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);

        // Add options to UseCaseConfig
        builder.setDefaultSessionConfig(sessionBuilder.build());
        builder.setSessionOptionUnpacker(Camera2SessionOptionUnpacker.INSTANCE);

        CaptureConfig.Builder captureBuilder = new CaptureConfig.Builder();
        captureBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        builder.setDefaultCaptureConfig(captureBuilder.build());
        builder.setCaptureOptionUnpacker(Camera2CaptureOptionUnpacker.INSTANCE);

        try {
            // Add default lensFacing if we can
            Integer checkedLensFacing =
                    (lensFacing != null) ? lensFacing : CameraX.getDefaultLensFacing();
            String defaultId = mCameraFactory.cameraIdForLensFacing(checkedLensFacing);
            if (defaultId != null) {
                builder.setLensFacing(checkedLensFacing);
            }

            int targetRotation = mWindowManager.getDefaultDisplay().getRotation();
            int rotationDegrees = CameraX.getCameraInfo(defaultId).getSensorRotationDegrees(
                    targetRotation);
            boolean isRotateNeeded = (rotationDegrees == 90 || rotationDegrees == 270);
            builder.setTargetRotation(targetRotation);
            builder.setTargetAspectRatioCustom(
                    isRotateNeeded ? DEFAULT_ASPECT_RATIO_3_4 : DEFAULT_ASPECT_RATIO_4_3);
        } catch (Exception e) {
            Log.w(TAG, "Unable to determine default lens facing for Preview.", e);
        }

        return builder.getUseCaseConfig();
    }
}
