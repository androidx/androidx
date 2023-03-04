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
import androidx.annotation.Nullable;
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

    private static final long INVALID_TIMESTAMP = -1;

    @Nullable
    private final CameraCaptureResult mBaseCameraCaptureResult;
    @NonNull
    private final TagBundle mTagBundle;
    private final long mTimestamp;

    /**
     * Creates an instance based on another {@link CameraCaptureResult}.
     *
     * @param baseCameraCaptureResult Most of the fields return the value of the base instance.
     * @param tagBundle               the overridden value for the {@link #getTagBundle()} field.
     */
    public VirtualCameraCaptureResult(
            @NonNull TagBundle tagBundle,
            @Nullable CameraCaptureResult baseCameraCaptureResult) {
        this(baseCameraCaptureResult, tagBundle, INVALID_TIMESTAMP);
    }

    /**
     * Creates empty instance with timestamp overridden.
     *
     * @param tagBundle the overridden value for the {@link #getTagBundle()} field.
     * @param timestamp the overridden value for the {@link #getTimestamp()} field.
     */
    public VirtualCameraCaptureResult(
            @NonNull TagBundle tagBundle,
            long timestamp) {
        this(/*baseCameraCaptureResult*/null, tagBundle, timestamp);
    }

    private VirtualCameraCaptureResult(
            @Nullable CameraCaptureResult baseCameraCaptureResult,
            @NonNull TagBundle tagBundle,
            long timestamp) {
        mBaseCameraCaptureResult = baseCameraCaptureResult;
        mTagBundle = tagBundle;
        mTimestamp = timestamp;
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
        return mBaseCameraCaptureResult != null ? mBaseCameraCaptureResult.getAfMode() :
                CameraCaptureMetaData.AfMode.UNKNOWN;
    }

    @NonNull
    @Override
    public CameraCaptureMetaData.AfState getAfState() {
        return mBaseCameraCaptureResult != null ? mBaseCameraCaptureResult.getAfState() :
                CameraCaptureMetaData.AfState.UNKNOWN;
    }

    @NonNull
    @Override
    public CameraCaptureMetaData.AeState getAeState() {
        return mBaseCameraCaptureResult != null ? mBaseCameraCaptureResult.getAeState() :
                CameraCaptureMetaData.AeState.UNKNOWN;
    }

    @NonNull
    @Override
    public CameraCaptureMetaData.AwbState getAwbState() {
        return mBaseCameraCaptureResult != null ? mBaseCameraCaptureResult.getAwbState() :
                CameraCaptureMetaData.AwbState.UNKNOWN;
    }

    @NonNull
    @Override
    public CameraCaptureMetaData.FlashState getFlashState() {
        return mBaseCameraCaptureResult != null ? mBaseCameraCaptureResult.getFlashState() :
                CameraCaptureMetaData.FlashState.UNKNOWN;
    }

    @Override
    public long getTimestamp() {
        if (mBaseCameraCaptureResult != null) {
            return mBaseCameraCaptureResult.getTimestamp();
        } else if (mTimestamp != INVALID_TIMESTAMP) {
            return mTimestamp;
        }
        throw new IllegalStateException("No timestamp is available.");
    }
}
