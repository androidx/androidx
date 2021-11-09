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

package androidx.camera.core.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageInfo;

/**
 * A processing step that updates the necessary {@link CaptureStage} based on the current
 * {@link ImageInfo}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface ImageInfoProcessor {
    /**
     * Returns the {@link CaptureStage} which should be issued for the processing.
     *
     * <p> This will be updated whenever {@link #process(ImageInfo)} is called and returns true.
     */
    @Nullable
    CaptureStage getCaptureStage();

    /**
     * Processes the metadata from a capture and updates the {@link CaptureStage} used for
     * subsequent captures if necessary.
     *
     * @return if true then the output of {@link #getCaptureStage()} will be updated. This means
     * the stream which produced the processed {@link ImageInfo} should be updated to use the new
     * CaptureStage.
     */
    boolean process(@NonNull ImageInfo imageInfo);
}
