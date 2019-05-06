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

package androidx.camera.camera2.impl;

import android.content.Context;
import android.hardware.camera2.CameraDevice;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.ConfigProvider;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.SessionConfig;

import java.util.Arrays;
import java.util.List;

/**
 * Provides defaults for {@link PreviewConfig} in the Camera2 implementation.
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public final class PreviewConfigProvider implements ConfigProvider<PreviewConfig> {
    private static final String TAG = "PreviewConfigProvider";

    private final CameraFactory mCameraFactory;
    private final WindowManager mWindowManager;

    public PreviewConfigProvider(CameraFactory cameraFactory, Context context) {
        mCameraFactory = cameraFactory;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    @Override
    public PreviewConfig getConfig(LensFacing lensFacing) {
        PreviewConfig.Builder builder =
                PreviewConfig.Builder.fromConfig(Preview.DEFAULT_CONFIG.getConfig(lensFacing));

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

        List<LensFacing> lensFacingList;

        // Add default lensFacing if we can
        if (lensFacing == LensFacing.FRONT) {
            lensFacingList = Arrays.asList(LensFacing.FRONT, LensFacing.BACK);
        } else {
            lensFacingList = Arrays.asList(LensFacing.BACK, LensFacing.FRONT);
        }

        try {
            String defaultId = null;

            for (LensFacing lensFacingCandidate : lensFacingList) {
                defaultId = mCameraFactory.cameraIdForLensFacing(lensFacingCandidate);
                if (defaultId != null) {
                    builder.setLensFacing(lensFacingCandidate);
                    break;
                }
            }

            int targetRotation = mWindowManager.getDefaultDisplay().getRotation();
            builder.setTargetRotation(targetRotation);
        } catch (Exception e) {
            Log.w(TAG, "Unable to determine default lens facing for Preview.", e);
        }

        return builder.build();
    }
}
