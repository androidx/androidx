/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core.internal.compat;

import android.media.ImageWriter;
import android.view.Surface;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(29)
final class ImageWriterCompatApi29Impl {

    @NonNull
    static ImageWriter newInstance(@NonNull Surface surface, @IntRange(from = 1) int maxImages,
            int format) {
        return ImageWriter.newInstance(surface, maxImages, format);
    }

    // Class should not be instantiated.
    private ImageWriterCompatApi29Impl() {
    }
}

