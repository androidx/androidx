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

import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Utility class for size related operations.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class SizeUtil {
    public static final Size VGA_SIZE = new Size(640, 480);

    private SizeUtil() {}

    /**
     * Returns the area of the supplied size.
     */
    public static int getArea(@NonNull Size size) {
        return size.getWidth() * size.getHeight();
    }
}
