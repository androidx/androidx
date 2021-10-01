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

import android.hardware.camera2.TotalCaptureResult;

import androidx.annotation.RequiresApi;

/**
 * Processes a {@link TotalCaptureResult} to update a CaptureStage.
 *
 * @since 1.0
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface RequestUpdateProcessorImpl extends ProcessorImpl {
    /**
     * Process the {@link TotalCaptureResult} to update the {@link CaptureStageImpl}
     *
     * @param result The metadata associated with the image. Can be null if the image and meta have
     *               not been synced.
     * @return The updated parameters used for the repeating requests. If this is {@code null} then
     * the previous parameters will be used.
     */
    CaptureStageImpl process(TotalCaptureResult result);
}
