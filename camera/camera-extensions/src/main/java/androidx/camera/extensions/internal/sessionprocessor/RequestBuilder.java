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

package androidx.camera.extensions.internal.sessionprocessor;

import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.RequestProcessor;
import androidx.camera.extensions.internal.RequestOptionConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A builder for building {@link androidx.camera.core.impl.RequestProcessor.Request}.
 */
class RequestBuilder {
    private List<Integer> mTargetOutputConfigIds = new ArrayList<>();
    private Map<CaptureRequest.Key<?>, Object> mParameters = new HashMap<>();
    private int mTemplateId = CameraDevice.TEMPLATE_PREVIEW;
    int mCaptureStageId;

    RequestBuilder() {
    }

    @NonNull
    RequestBuilder addTargetOutputConfigIds(int targetOutputConfigId) {
        mTargetOutputConfigIds.add(targetOutputConfigId);
        return this;
    }

    @NonNull
    RequestBuilder setParameters(@NonNull CaptureRequest.Key<?> key,
            @NonNull Object value) {
        mParameters.put(key, value);
        return this;
    }

    @NonNull
    RequestBuilder setTemplateId(int templateId) {
        mTemplateId = templateId;
        return this;
    }

    @NonNull
    public RequestBuilder setCaptureStageId(int captureStageId) {
        mCaptureStageId = captureStageId;
        return this;
    }

    @NonNull
    RequestProcessor.Request build() {
        return new RequestProcessorRequest(
                mTargetOutputConfigIds, mParameters, mTemplateId, mCaptureStageId);
    }

    static class RequestProcessorRequest implements RequestProcessor.Request {
        final List<Integer> mTargetOutputConfigIds;
        final Config mParameterConfig;
        final int mTemplateId;
        final int mCaptureStageId;

        RequestProcessorRequest(List<Integer> targetOutputConfigIds,
                Map<CaptureRequest.Key<?>, Object> parameters,
                int templateId,
                int captureStageId) {
            mTargetOutputConfigIds = targetOutputConfigIds;
            mTemplateId = templateId;
            mCaptureStageId = captureStageId;

            RequestOptionConfig.Builder requestOptionBuilder = new RequestOptionConfig.Builder();
            for (CaptureRequest.Key<?> key : parameters.keySet()) {
                @SuppressWarnings("unchecked")
                CaptureRequest.Key<Object> objKey = (CaptureRequest.Key<Object>) key;
                requestOptionBuilder.setCaptureRequestOption(objKey,
                        parameters.get(objKey));
            }
            mParameterConfig = requestOptionBuilder.build();
        }

        @Override
        @NonNull
        public List<Integer> getTargetOutputConfigIds() {
            return mTargetOutputConfigIds;
        }

        @Override
        @NonNull
        public Config getParameters() {
            return mParameterConfig;
        }

        @Override
        public int getTemplateId() {
            return mTemplateId;
        }

        public int getCaptureStageId() {
            return mCaptureStageId;
        }
    }
}
