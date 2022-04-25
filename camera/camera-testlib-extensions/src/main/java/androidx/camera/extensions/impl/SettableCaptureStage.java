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

package androidx.camera.extensions.impl;

import android.hardware.camera2.CaptureRequest;
import android.util.Pair;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class SettableCaptureStage implements CaptureStageImpl {
    private final int mId;

    private Map<CaptureRequest.Key, Object> mCaptureRequestKeyValueMap = new HashMap<>();

    /**
     * Constructor for a {@link CaptureStageImpl} with specific identifier.
     *
     * <p>After this {@link CaptureStageImpl} is applied to a single capture operation,
     * developers can
     * retrieve the {@link android.media.Image} object with the identifier.
     *
     * @param id The identifier for the {@link CaptureStageImpl}.
     */
    SettableCaptureStage(int id) {
        mId = id;
    }

    /** Returns the identifier for the {@link CaptureStageImpl}. */
    @Override
    public int getId() {
        return mId;
    }

    @Override
    public List<Pair<CaptureRequest.Key, Object>> getParameters() {
        List<Pair<CaptureRequest.Key, Object>> parameters = new ArrayList<>();

        for (Map.Entry<CaptureRequest.Key, Object> entry : mCaptureRequestKeyValueMap.entrySet()) {
            parameters.add(Pair.create(entry.getKey(), entry.getValue()));
        }

        return parameters;
    }

    /**
     * Adds necessary {@link CaptureRequest.Key} settings into the {@link CaptureStageImpl} object.
     */
    <T> void addCaptureRequestParameters(CaptureRequest.Key<T> key, T value) {
        mCaptureRequestKeyValueMap.put(key, value);
    }
}
