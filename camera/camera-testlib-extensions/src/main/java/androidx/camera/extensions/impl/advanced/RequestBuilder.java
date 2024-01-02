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

package androidx.camera.extensions.impl.advanced;

import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A builder to build the {@link RequestProcessorImpl.Request} instance.
 */
public class RequestBuilder {
    List<Integer> mTargetOutputConfigIds = new ArrayList<>();
    Map<CaptureRequest.Key<?>, Object> mParameters = new HashMap<>();
    int mTemplateId = CameraDevice.TEMPLATE_PREVIEW;

    /**
     * Construct the builder with default settings.
     */
    public RequestBuilder() {
    }

    /**
     * Construct the builder.
     */
    public RequestBuilder(int targetOutputConfigId, int templateId) {
        addTargetOutputConfigIds(targetOutputConfigId);
        setTemplateId(templateId);
    }


    /**
     * Adds the target outputconfig ids.
     */
    @NonNull
    public RequestBuilder addTargetOutputConfigIds(int targetOutputConfigId) {
        mTargetOutputConfigIds.add(targetOutputConfigId);
        return this;
    }

    /**
     * Sets the parameters
     */
    @NonNull
    public RequestBuilder setParameters(@NonNull CaptureRequest.Key<?> key,
            @NonNull Object value) {
        mParameters.put(key, value);
        return this;
    }

    /**
     * Sets the template id.
     */
    @NonNull
    public RequestBuilder setTemplateId(int templateId) {
        mTemplateId = templateId;
        return this;
    }

    /**
     * construct {@link RequestProcessorImpl.Request} instance.
     */
    @NonNull
    public RequestProcessorImpl.Request build() {
        return new RequestProcessorRequest(
                mTargetOutputConfigIds, mParameters, mTemplateId);
    }

    static class RequestProcessorRequest implements RequestProcessorImpl.Request {
        final List<Integer> mTargetOutputConfigIds;
        final Map<CaptureRequest.Key<?>, Object> mParameters;
        final int mTemplateId;

        RequestProcessorRequest(List<Integer> targetOutputConfigIds,
                Map<CaptureRequest.Key<?>, Object> parameters,
                int templateId) {
            mTargetOutputConfigIds = targetOutputConfigIds;
            mParameters = parameters;
            mTemplateId = templateId;
        }

        @Override
        @NonNull
        public List<Integer> getTargetOutputConfigIds() {
            return mTargetOutputConfigIds;
        }

        @Override
        @NonNull
        public Map<CaptureRequest.Key<?>, Object> getParameters() {
            return mParameters;
        }

        @Override
        @NonNull
        public Integer getTemplateId() {
            return mTemplateId;
        }
    }
}
