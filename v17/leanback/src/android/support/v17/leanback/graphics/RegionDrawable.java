/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.support.v17.leanback.graphics;

import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Subclass of {@link Drawable} that is used to represent a rectangular region. This class allows
 * users to specify {@link BoundsRule} for updating the bounds of the drawable whenever
 * it is updated.
 */
public abstract class RegionDrawable extends Drawable {
    protected final Paint mPaint = new Paint();
    private final BoundsRule boundsRule = new BoundsRule();
    private Rect adjustedBounds = new Rect();
    private int lastTop = Integer.MIN_VALUE;
    private int lastBottom = Integer.MIN_VALUE;
    private int lastRight = Integer.MIN_VALUE;
    private int lastLeft = Integer.MIN_VALUE;

    /**
     * Returns the instance of {@link BoundsRule} associated with this {@link RegionDrawable}.
     */
    public BoundsRule getBoundsRule() {
        return this.boundsRule;
    }

    /**
     * Updates the bounds based on the {@link BoundsRule}.
     */
    public void updateBounds(Rect bounds) {
        boundsRule.calculateBounds(bounds, adjustedBounds);
        overrideBoundsIfNecessary();
        setBounds(adjustedBounds);
    }

    private void overrideBoundsIfNecessary() {
        if (lastTop != Integer.MIN_VALUE) {
            adjustedBounds.top = lastTop;
        }
        if (lastLeft != Integer.MIN_VALUE) {
            adjustedBounds.left = lastLeft;
        }
        if (lastRight != Integer.MIN_VALUE) {
            adjustedBounds.right = lastRight;
        }
        if (lastBottom != Integer.MIN_VALUE) {
            adjustedBounds.bottom = lastBottom;
        }
    }

    /**
     * Sets the top for the current region. Once set, any updates to the bounds won't affect
     * this property.
     */
    public void setOverrideTop(int top) {
        lastTop = top;
        adjustedBounds.top = top;
        setBounds(adjustedBounds);
    }

    /**
     * Sets the bottom for the current region. Once set, any updates to the bounds won't affect
     * this property.
     */
    public void setOverrideBottom(int bottom) {
        lastBottom = bottom;
        adjustedBounds.bottom = bottom;
        setBounds(adjustedBounds);
    }

    /**
     * Sets the left for the current region. Once set, any updates to the bounds won't affect
     * this property.
     */
    public void setOverrideLeft(int left) {
        lastLeft = left;
        adjustedBounds.left = left;
        setBounds(adjustedBounds);
    }

    /**
     * Sets the right for the current region. Once set, any updates to the bounds won't affect
     * this property.
     */
    public void setOverrideRight(int right) {
        lastRight = right;
        adjustedBounds.right = right;
        setBounds(adjustedBounds);
    }
}
