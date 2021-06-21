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
package androidx.window

import android.graphics.Rect

/**
 * Description of a physical feature on the display.
 *
 *
 * A display feature is a distinctive physical attribute located within the display panel of
 * the device. It can intrude into the application window space and create a visual distortion,
 * visual or touch discontinuity, make some area invisible or create a logical divider or
 * separation in the screen space.
 *
 * @see FoldingFeature Represents a screen fold that intersects the application window.
 */
public interface DisplayFeature {
    /**
     * The bounding rectangle of the feature within the application window
     * in the window coordinate space.
     */
    public val bounds: Rect
}
