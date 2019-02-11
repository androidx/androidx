/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/**
 * This class used to constant values corresponding to the internal defined image format value used
 * in StreamConfigurationMap.java.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class ImageFormatConstants {
    // Internal format in StreamConfigurationMap.java that will be mapped to public ImageFormat.JPEG
    public static final int INTERNAL_DEFINED_IMAGE_FORMAT_JPEG = 0x21;

    // Internal format HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED (0x22) in StreamConfigurationMap.java
    // that will be mapped to public ImageFormat.PRIVATE after android level 23.
    public static final int INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE = 0x22;

    private ImageFormatConstants() {
    }

    ;
}
