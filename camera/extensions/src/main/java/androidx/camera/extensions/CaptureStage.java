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

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CaptureConfig;

/**
 * The set of parameters that defines a single capture that will be sent to the camera.
 */
public final class CaptureStage implements androidx.camera.core.CaptureStage {
    private final int mId;
    private final CaptureConfig.Builder mCaptureConfigBuilder = new CaptureConfig.Builder();
    private final Camera2Config.Builder mCamera2ConfigBuilder = new Camera2Config.Builder();

    /**
     * Constructor for a {@link CaptureStage} with specific identifier.
     *
     * <p>After this {@link CaptureStage} is applied to a single capture operation, developers can
     * retrieve the {@link androidx.camera.core.ImageProxy} object with the identifier by
     * {@link androidx.camera.core.ImageProxyBundle#getImageProxy(int)}.
     *
     * @param id The identifier for the {@link CaptureStage}.
     * */
    public CaptureStage(int id) {
        mCaptureConfigBuilder.setTag(id);
        mId = id;
    }

    /** Returns the identifier for the {@link CaptureStage}. */
    @Override
    public int getId() {
        return mId;
    }

    /**
     * Returns the {@link CaptureConfig} for the {@link CaptureStage} object.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public CaptureConfig getCaptureConfig() {
        mCaptureConfigBuilder.addImplementationOptions(mCamera2ConfigBuilder.build());
        return mCaptureConfigBuilder.build();
    }

    /**
     * Adds necessary {@link CaptureRequest.Key} settings into the {@link CaptureStage} object.
     */
    public <T> void addCaptureRequestParameters(CaptureRequest.Key<T> key, T value) {
        mCamera2ConfigBuilder.setCaptureRequestOption(key, value);
    }
}
