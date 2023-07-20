/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.test.uiautomator;

/**
 * Specifies the orientation state of a display. Used with
 * {@link UiDevice#setOrientation(Orientation)} and
 * {@link UiDevice#setOrientation(Orientation, int)}.
 */
public enum Orientation {
    /** Sets the rotation to be the natural orientation. */
    ROTATION_0,
    /** Sets the rotation to be 90 degrees clockwise to the natural orientation. */
    ROTATION_90,
    /** Sets the rotation to be 180 degrees clockwise to the natural orientation. */
    ROTATION_180,
    /** Sets the rotation to be 270 degrees clockwise to the natural orientation. */
    ROTATION_270,
    /** Sets the rotation so that the display height will be >= the display width. */
    PORTRAIT,
    /** Sets the rotation so that the display height will be <= the display width. */
    LANDSCAPE,
    /** Freezes the current rotation. */
    FROZEN,
    /** Unfreezes the current rotation. Need to wait a short period for the rotation animation to
     *  complete before performing another operation. */
    UNFROZEN;
}
