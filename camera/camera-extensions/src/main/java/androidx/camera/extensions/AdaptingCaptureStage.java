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

import android.hardware.camera2.CaptureRequest;
import android.util.Pair;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.CaptureStage;
import androidx.camera.extensions.impl.CaptureStageImpl;

/** A {@link CaptureStage} that calls a vendor provided implementation. */
final class AdaptingCaptureStage implements CaptureStage {

    private final CaptureConfig mCaptureRequestConfiguration;
    private final int mId;

    @SuppressWarnings("unchecked")
    AdaptingCaptureStage(CaptureStageImpl impl) {
        mId = impl.getId();

        Camera2Config.Builder camera2ConfigurationBuilder = new Camera2Config.Builder();

        for (Pair<CaptureRequest.Key, Object> captureParameter : impl.getParameters()) {
            camera2ConfigurationBuilder.setCaptureRequestOption(captureParameter.first,
                    captureParameter.second);
        }

        CaptureConfig.Builder captureConfigBuilder = new CaptureConfig.Builder();
        captureConfigBuilder.addImplementationOptions(camera2ConfigurationBuilder.build());
        captureConfigBuilder.setTag(mId);
        mCaptureRequestConfiguration = captureConfigBuilder.build();
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public CaptureConfig getCaptureConfig() {
        return mCaptureRequestConfiguration;
    }
}
