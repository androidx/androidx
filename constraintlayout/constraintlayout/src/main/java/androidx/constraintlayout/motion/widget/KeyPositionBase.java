/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.constraintlayout.motion.widget;

import android.graphics.RectF;
import android.view.View;

import java.util.HashSet;

/**
 * Defines a KeyPositionBase abstract base class KeyPositionBase elements provide
 *
 */

abstract class KeyPositionBase extends Key {
    protected static final float SELECTION_SLOPE = 20;
    int mCurveFit = UNSET;

    /**
     * Get the position of the view
     *
     *
     * @param layoutWidth
     * @param layoutHeight
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     *
     */
    abstract void calcPosition(int layoutWidth,
                               int layoutHeight,
                               float startX,
                               float startY,
                               float endX,
                               float endY);

    /**
     * @return
     *
     */
    abstract float getPositionX();

    /**
     * @return
     *
     */
    abstract float getPositionY();

    @Override
    void getAttributeNames(HashSet<String> attributes) {
    }

    /**
     *
     * @param view
     * @param start
     * @param end
     * @param x
     * @param y
     * @param attribute
     * @param value
     *
     */
    abstract void positionAttributes(View view,
                                     RectF start,
                                     RectF end,
                                     float x,
                                     float y,
                                     String[] attribute,
                                     float[] value);

    /**
     *
     * @param layoutWidth
     * @param layoutHeight
     * @param start
     * @param end
     * @param x
     * @param y
     * @return
     *
     */
    public abstract boolean intersects(int layoutWidth,
                                       int layoutHeight,
                                       RectF start,
                                       RectF end,
                                       float x,
                                       float y);
}
