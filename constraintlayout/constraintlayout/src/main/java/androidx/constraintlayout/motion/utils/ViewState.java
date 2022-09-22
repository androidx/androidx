/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.constraintlayout.motion.utils;
import android.view.View;

/**
 * Stores the bounds and rotation of a widget
 */
public class ViewState {
    public float rotation;
    public int left, top, right, bottom;

    /**
     * Get the parameters of the view
     * @param v the view
     */
    public void getState(View v) {
        left = v.getLeft();
        top = v.getTop();
        right = v.getRight();
        bottom = v.getBottom();
        rotation = v.getRotation();
    }

    /**
     * The width of the view
     * @return the width
     */
    public int width() {
        return right - left;
    }

    /**
     * the height of the view
     * @return the width
     */
    public int height() {
        return bottom - top;
    }
}
