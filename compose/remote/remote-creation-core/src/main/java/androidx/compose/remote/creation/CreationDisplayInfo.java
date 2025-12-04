/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation;

import androidx.annotation.RestrictTo;

/**
 * Details about the connection to the virtual display at Creation time. May not match the
 * properties of the local UI Context.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CreationDisplayInfo {
    private final int mWidth;
    private final int mHeight;
    private final float mDensity;

    public CreationDisplayInfo(
            int width,
            int height,
            float density
    ) {
        this.mWidth = width;
        this.mHeight = height;
        this.mDensity = density;
    }

    public float getDensity() {
        return mDensity;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getWidth() {
        return mWidth;
    }

    /**
     * Returns the density in DPI. The calculation is based on a baseline of 160 DPI for a density
     * of 1.0.
     */
    public int getDensityDpi() {
        return (int) (160 * mDensity);
    }
}
