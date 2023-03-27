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

package androidx.camera.core.streamsharing;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.CameraCaptureMetaData;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.TagBundle;

/**
 * A virtual {@link CameraCaptureResult} which based on a real instance with some fields
 * overridden.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class VirtualCameraCaptureResult implements CameraCaptureResult {

    @NonNull
    private final CameraCaptureResult mBaseCameraCaptureResult;
    @NonNull
    private final TagBundle mTagBundle;

    /**
     * @param baseCameraCaptureResult Most of the fields return the value of the base instance.
     * @param tagBundle               the overridden value for the {@link #getTagBundle()} field.
     */
    VirtualCameraCaptureResult(
            @NonNull CameraCaptureResult baseCameraCaptureResult,
            @NonNull TagBundle tagBundle) {
        mBaseCameraCaptureResult = baseCameraCaptureResult;
        mTagBundle = tagBundle;
    }

    @NonNull
    @Override
    public TagBundle getTagBundle() {
        // Returns the overridden value.
        return mTagBundle;
    }

    @NonNull
    @Override
    public CameraCaptureMetaData.AfMode getAfMode() {
        return mBaseCameraCaptureResult.getAfMode();
    }

    @NonNull
    @Override
    public CameraCaptureMetaData.AfState getAfState() {
        return mBaseCameraCaptureResult.getAfState();
    }

    @NonNull
    @Override
    public CameraCaptureMetaData.AeState getAeState() {
        return mBaseCameraCaptureResult.getAeState();
    }

    @NonNull
    @Override
    public CameraCaptureMetaData.AwbState getAwbState() {
        return mBaseCameraCaptureResult.getAwbState();
    }

    @NonNull
    @Override
    public CameraCaptureMetaData.FlashState getFlashState() {
        return mBaseCameraCaptureResult.getFlashState();
    }

    @Override
    public long getTimestamp() {
        return mBaseCameraCaptureResult.getTimestamp();
    }
}
