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

package androidx.camera.core.internal.utils;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.CameraCaptureMetaData.AeState;
import androidx.camera.core.impl.CameraCaptureMetaData.AfState;
import androidx.camera.core.impl.CameraCaptureMetaData.AwbState;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraCaptureResults;

/**
 * Used for storing frames for ZSL capture.
 *
 * <p>Enqueueing process ignores frames when the quality is inadequate for ZSL capture.</p>
 *
 * <p>Adequate quality is defined as:
 * - AF Focused
 * - AE Converged
 * - AWB Converged
 * </p>
 */
public final class ZslRingBuffer extends ArrayRingBuffer<ImageProxy> {

    public ZslRingBuffer(int ringBufferCapacity,
            @NonNull OnRemoveCallback<ImageProxy> onRemoveCallback) {
        super(ringBufferCapacity, onRemoveCallback);
    }

    @Override
    public void enqueue(@NonNull ImageProxy imageProxy) {
        if (isValidZslFrame(imageProxy.getImageInfo())) {
            super.enqueue(imageProxy);
        } else {
            mOnRemoveCallback.onRemove(imageProxy);
        }
    }

    private boolean isValidZslFrame(@NonNull ImageInfo imageInfo) {
        CameraCaptureResult cameraCaptureResult =
                CameraCaptureResults.retrieveCameraCaptureResult(imageInfo);

        if (cameraCaptureResult.getAfState() != AfState.LOCKED_FOCUSED
                && cameraCaptureResult.getAfState() != AfState.PASSIVE_FOCUSED)  {
            return false;
        }

        if (cameraCaptureResult.getAeState() != AeState.CONVERGED) {
            return false;
        }

        if (cameraCaptureResult.getAwbState() != AwbState.CONVERGED) {
            return false;
        }

        return true;
    }
}
