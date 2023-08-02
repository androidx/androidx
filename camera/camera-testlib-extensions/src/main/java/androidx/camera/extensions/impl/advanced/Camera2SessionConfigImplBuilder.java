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


import android.annotation.SuppressLint;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A builder implementation to help OEM build the {@link Camera2SessionConfigImpl} instance.
 */
@SuppressLint("UnknownNullness")
public class Camera2SessionConfigImplBuilder {
    private int mSessionTemplateId = CameraDevice.TEMPLATE_PREVIEW;
    Map<CaptureRequest.Key<?>, Object> mSessionParameters = new HashMap<>();
    List<Camera2OutputConfigImpl> mCamera2OutputConfigs = new ArrayList<>();

    public Camera2SessionConfigImplBuilder() {
    }

    /**
     * Adds a output config.
     */
    public Camera2SessionConfigImplBuilder addOutputConfig(
            Camera2OutputConfigImpl outputConfig) {
        mCamera2OutputConfigs.add(outputConfig);
        return this;
    }

    /**
     * Sets session parameters.
     */
    public <T> Camera2SessionConfigImplBuilder addSessionParameter(
            CaptureRequest.Key<T> key, T value) {
        mSessionParameters.put(key, value);
        return this;
    }

    /**
     * Sets the template id for session parameters request.
     */
    public Camera2SessionConfigImplBuilder setSessionTemplateId(int templateId) {
        mSessionTemplateId = templateId;
        return this;
    }

    /**
     * Gets the session template id.
     */
    public int getSessionTemplateId() {
        return mSessionTemplateId;
    }

    /**
     * Gets the session parameters.
     */
    public Map<CaptureRequest.Key<?>, Object> getSessionParameters() {
        return mSessionParameters;
    }

    /**
     * Gets all the output configs.
     */
    public List<Camera2OutputConfigImpl> getCamera2OutputConfigs() {
        return mCamera2OutputConfigs;
    }

    /**
     * Builds a {@link Camera2SessionConfigImpl} instance.
     */
    public Camera2SessionConfigImpl build() {
        return new Camera2SessionConfigImplImpl(this);
    }

    private static class Camera2SessionConfigImplImpl implements
            Camera2SessionConfigImpl {
        int mSessionTemplateId;
        Map<CaptureRequest.Key<?>, Object> mSessionParameters;
        List<Camera2OutputConfigImpl> mCamera2OutputConfigs;

        Camera2SessionConfigImplImpl(Camera2SessionConfigImplBuilder builder) {
            mSessionTemplateId = builder.getSessionTemplateId();
            mSessionParameters = builder.getSessionParameters();
            mCamera2OutputConfigs = builder.getCamera2OutputConfigs();
        }

        @Override
        public List<Camera2OutputConfigImpl> getOutputConfigs() {
            return mCamera2OutputConfigs;
        }

        @Override
        public Map<CaptureRequest.Key<?>, Object> getSessionParameters() {
            return mSessionParameters;
        }

        @Override
        public int getSessionTemplateId() {
            return mSessionTemplateId;
        }
    }
}

