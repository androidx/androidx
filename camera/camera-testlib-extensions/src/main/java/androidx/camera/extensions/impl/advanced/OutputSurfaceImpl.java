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

package androidx.camera.extensions.impl.advanced;

import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * For specifying output surface of the extension.
 *
 * @since 1.2
 */
public interface OutputSurfaceImpl {
    /**
     * Gets the surface. It returns null if output surface is not specified.
     */
    @Nullable
    Surface getSurface();


    /**
     * Gets the size.
     */
    @NonNull
    Size getSize();

    /**
     * Gets the image format.
     */
    int getImageFormat();
}
