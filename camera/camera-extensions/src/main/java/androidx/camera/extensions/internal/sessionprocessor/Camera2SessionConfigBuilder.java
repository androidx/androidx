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

package androidx.camera.extensions.internal.sessionprocessor;

import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.extensions.impl.advanced.Camera2SessionConfigImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A builder implementation to build the {@link Camera2SessionConfig} instance.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class Camera2SessionConfigBuilder {
    private int mSessionTemplateId = CameraDevice.TEMPLATE_PREVIEW;
    private Map<CaptureRequest.Key<?>, Object> mSessionParameters = new HashMap<>();
    private List<Camera2OutputConfig> mCamera2OutputConfigs = new ArrayList<>();

    Camera2SessionConfigBuilder() {
    }

    /**
     * Adds a output config.
     */
    @NonNull
    Camera2SessionConfigBuilder addOutputConfig(
            @NonNull Camera2OutputConfig outputConfig) {
        mCamera2OutputConfigs.add(outputConfig);
        return this;
    }

    /**
     * Sets session parameters.
     */
    @NonNull
    <T> Camera2SessionConfigBuilder addSessionParameter(
            @NonNull CaptureRequest.Key<T> key, @Nullable T value) {
        mSessionParameters.put(key, value);
        return this;
    }

    /**
     * Sets the template id for session parameters request.
     */
    @NonNull
    Camera2SessionConfigBuilder setSessionTemplateId(int templateId) {
        mSessionTemplateId = templateId;
        return this;
    }

    /**
     * Gets the session template id.
     */
    int getSessionTemplateId() {
        return mSessionTemplateId;
    }

    /**
     * Gets the session parameters.
     */
    @NonNull
    Map<CaptureRequest.Key<?>, Object> getSessionParameters() {
        return mSessionParameters;
    }

    /**
     * Gets all the output configs.
     */
    @NonNull
    List<Camera2OutputConfig> getCamera2OutputConfigs() {
        return mCamera2OutputConfigs;
    }

    /**
     * Builds a {@link Camera2SessionConfigImpl} instance.
     */
    @NonNull
    Camera2SessionConfig build() {
        return new SessionConfigImpl(mSessionTemplateId, mSessionParameters, mCamera2OutputConfigs);
    }

    private static class SessionConfigImpl implements Camera2SessionConfig {
        private final int mSessionTemplateId;
        private final Map<CaptureRequest.Key<?>, Object> mSessionParameters;
        private final List<Camera2OutputConfig> mCamera2OutputConfigs;

        SessionConfigImpl(int sessionTemplateId,
                Map<CaptureRequest.Key<?>, Object> sessionParameters,
                List<Camera2OutputConfig> camera2OutputConfigs) {
            mSessionTemplateId = sessionTemplateId;
            mSessionParameters = sessionParameters;
            mCamera2OutputConfigs = camera2OutputConfigs;
        }

        @Override
        @NonNull
        public List<Camera2OutputConfig> getOutputConfigs() {
            return mCamera2OutputConfigs;
        }

        @Override
        @NonNull
        public Map<CaptureRequest.Key<?>, Object> getSessionParameters() {
            return mSessionParameters;
        }

        @Override
        public int getSessionTemplateId() {
            return mSessionTemplateId;
        }
    }
}
