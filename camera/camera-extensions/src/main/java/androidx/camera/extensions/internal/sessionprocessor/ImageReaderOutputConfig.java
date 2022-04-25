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

package androidx.camera.extensions.internal.sessionprocessor;

import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Surface will be created by constructing an ImageReader.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface ImageReaderOutputConfig extends Camera2OutputConfig {
    /**
     * Returns the size of the surface.
     */
    @NonNull
    Size getSize();

    /**
     * Gets the image format of the surface.
     */
    int getImageFormat();

    /**
     * Gets the capacity for the image reader.
     */
    int getMaxImages();
}
