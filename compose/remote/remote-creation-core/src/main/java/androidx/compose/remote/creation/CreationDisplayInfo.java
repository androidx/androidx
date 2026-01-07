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

/**
 * Holds information about the display properties at the time of creation.
 *
 * <p>This includes the width, height, and density of the display where the remote UI will be
 * rendered. These properties may differ from the properties of the local UI context and may change
 * when the document is played.
 */
public class CreationDisplayInfo {
    private final int mWidth;
    private final int mHeight;
    private final int mDensityDpi;

    public CreationDisplayInfo(
            int width,
            int height,
            int mDensityDpi
    ) {
        this.mWidth = width;
        this.mHeight = height;
        this.mDensityDpi = mDensityDpi;
    }

    /**
     * Returns the logical density of the display. This is a scaling factor for the Density
     * Independent Pixel unit, where one DIP is one pixel on an approximately 160 dpi screen (for
     * example a 240x320, 1.5"x2" screen), providing the baseline of the system's display.
     * Thus on a 160dpi screen this density value will be 1; on a 120 dpi screen it would be .75;
     * etc.
     *
     * @see #getDensityDpi()
     */
    public float getDensity() {
        return mDensityDpi / 160f;
    }

    /**
     * Returns the height in pixels of the virtual display.
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Returns the width in pixels of the virtual display.
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Returns the density in DPI. The calculation is based on a baseline of 160 DPI for a density
     * of 1.0.
     */
    public int getDensityDpi() {
        return mDensityDpi;
    }
}
