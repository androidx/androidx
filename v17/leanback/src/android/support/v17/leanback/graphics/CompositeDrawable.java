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

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Property;

import java.util.ArrayList;

/**
 * Generic drawable class that can be composed of multiple children. Whenever the bounds changes
 * for this class, it updates those of it's children by calling {@link ChildDrawable#updateBounds}.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class CompositeDrawable extends Drawable implements Drawable.Callback {
    protected ArrayList<ChildDrawable> children = new ArrayList();

    /**
     * Adds the supplied region.
     */
    public void addChildDrawable(Drawable drawable) {
        children.add(new ChildDrawable(drawable, this));
        drawable.setCallback(this);
    }

    /**
     * Returns the {@link Drawable} for the given index.
     */
    public Drawable getDrawable(int index) {
        return children.get(index).mDrawable;
    }

    /**
     * Returns the {@link ChildDrawable} at the given index.
     */
    public ChildDrawable getChildAt(int index) {
        return children.get(index);
    }

    /**
     * Removes the child corresponding to the given index.
     */
    public void removeChild(int index) {
        children.remove(index);
    }

    /**
     * Removes the given region.
     */
    public void removeDrawable(Drawable drawable) {
        for (int i = 0; i < children.size(); i++) {
            if (drawable == children.get(i).mDrawable) {
                children.get(i).mDrawable.setCallback(null);
                children.remove(i);
                return;
            }
        }
    }

    /**
     * Returns the total number of children.
     */
    public int getChildCount() {
        return children.size();
    }

    @Override
    public void draw(Canvas canvas) {
        for (int i = 0; i < children.size(); i++) {
            children.get(i).mDrawable.draw(canvas);
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        updateBounds(bounds);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        for (int i = 0; i < children.size(); i++) {
            children.get(i).mDrawable.setColorFilter(colorFilter);
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }

    @Override
    public void setAlpha(int alpha) {
        for (int i = 0; i < children.size(); i++) {
            children.get(i).mDrawable.setAlpha(alpha);
        }
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        invalidateSelf();
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        scheduleSelf(what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        unscheduleSelf(what);
    }

    /**
     * Updates the bounds based on the {@link BoundsRule}.
     */
    void updateBounds(Rect bounds) {
        for (int i = 0; i < children.size(); i++) {
            ChildDrawable childDrawable = children.get(i);
            childDrawable.updateBounds(bounds);
        }
    }

    /**
     * Wrapper class holding a drawable object and {@link BoundsRule} to update drawable bounds
     * when parent bound changes.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static final class ChildDrawable {
        private final BoundsRule boundsRule = new BoundsRule();
        private final Drawable mDrawable;
        private final Rect adjustedBounds = new Rect();
        private final CompositeDrawable mParent;

        public ChildDrawable(Drawable drawable, CompositeDrawable parent) {
            this.mDrawable = drawable;
            this.mParent = parent;
        }

        /**
         * Returns the instance of {@link BoundsRule}.
         */
        public BoundsRule getBoundsRule() {
            return this.boundsRule;
        }

        /**
         * Returns the {@link Drawable}.
         */
        public Drawable getDrawable() {
            return mDrawable;
        }

        /**
         * Updates the bounds based on the {@link BoundsRule}.
         */
        void updateBounds(Rect bounds) {
            boundsRule.calculateBounds(bounds, adjustedBounds);
            mDrawable.setBounds(adjustedBounds);
        }

        void recomputeBounds() {
            updateBounds(mParent.getBounds());
        }

        /**
         * Implementation of {@link Property} for overrideTop attribute.
         */
        public static final Property<CompositeDrawable.ChildDrawable, Integer> TOP_ABSOLUTE
                = new Property<CompositeDrawable.ChildDrawable, Integer>(Integer.class, "absoluteTop") {

            @Override
            public void set(CompositeDrawable.ChildDrawable obj, Integer value) {
                if (obj.getBoundsRule().mTop == null) {
                    obj.getBoundsRule().mTop = BoundsRule.absoluteValue(value);
                } else {
                    obj.getBoundsRule().mTop.setAbsoluteValue(value);
                }

                obj.recomputeBounds();
            }

            @Override
            public Integer get(CompositeDrawable.ChildDrawable obj) {
                return obj.getBoundsRule().mTop.getAbsoluteValue();
            }
        };


        /**
         * Implementation of {@link Property} for overrideBottom attribute.
         */
        public static final Property<CompositeDrawable.ChildDrawable, Integer> BOTTOM_ABSOLUTE
                = new Property<CompositeDrawable.ChildDrawable, Integer>(
                Integer.class, "absoluteBottom") {

            @Override
            public void set(CompositeDrawable.ChildDrawable obj, Integer value) {
                if (obj.getBoundsRule().mBottom == null) {
                    obj.getBoundsRule().mBottom = BoundsRule.absoluteValue(value);
                } else {
                    obj.getBoundsRule().mBottom.setAbsoluteValue(value);
                }

                obj.recomputeBounds();
            }

            @Override
            public Integer get(CompositeDrawable.ChildDrawable obj) {
                return obj.getBoundsRule().mBottom.getAbsoluteValue();
            }
        };


        /**
         * Implementation of {@link Property} for overrideLeft attribute.
         */
        public static final Property<CompositeDrawable.ChildDrawable, Integer> LEFT_ABSOLUTE
                = new Property<CompositeDrawable.ChildDrawable, Integer>(
                Integer.class, "absoluteLeft") {

            @Override
            public void set(CompositeDrawable.ChildDrawable obj, Integer value) {
                if (obj.getBoundsRule().mLeft == null) {
                    obj.getBoundsRule().mLeft = BoundsRule.absoluteValue(value);
                } else {
                    obj.getBoundsRule().mLeft.setAbsoluteValue(value);
                }

                obj.recomputeBounds();
            }

            @Override
            public Integer get(CompositeDrawable.ChildDrawable obj) {
                return obj.getBoundsRule().mLeft.getAbsoluteValue();
            }
        };

        /**
         * Implementation of {@link Property} for overrideRight attribute.
         */
        public static final Property<CompositeDrawable.ChildDrawable, Integer> RIGHT_ABSOLUTE
                = new Property<CompositeDrawable.ChildDrawable, Integer>(
                Integer.class, "absoluteRight") {

            @Override
            public void set(CompositeDrawable.ChildDrawable obj, Integer value) {
                if (obj.getBoundsRule().mRight == null) {
                    obj.getBoundsRule().mRight = BoundsRule.absoluteValue(value);
                } else {
                    obj.getBoundsRule().mRight.setAbsoluteValue(value);
                }

                obj.recomputeBounds();
            }

            @Override
            public Integer get(CompositeDrawable.ChildDrawable obj) {
                return obj.getBoundsRule().mRight.getAbsoluteValue();
            }
        };

        /**
         * Implementation of {@link Property} for overwriting the bottom attribute of
         * {@link BoundsRule} associated with this {@link ChildDrawable}. This allows users to
         * change the bounds rules as a percentage of parent size. This is preferable over
         * {@see PROPERTY_TOP_ABSOLUTE} when the exact start/end position of scroll movement
         * isn't available at compile time.
         */
        public static final Property<CompositeDrawable.ChildDrawable, Float> TOP_FRACTION
                = new Property<CompositeDrawable.ChildDrawable, Float>(Float.class, "fractionTop") {

            @Override
            public void set(CompositeDrawable.ChildDrawable obj, Float value) {
                if (obj.getBoundsRule().mTop == null) {
                    obj.getBoundsRule().mTop = BoundsRule.inheritFromParent(value);
                } else {
                    obj.getBoundsRule().mTop.setFraction(value);
                }

                obj.recomputeBounds();
            }

            @Override
            public Float get(CompositeDrawable.ChildDrawable obj) {
                return obj.getBoundsRule().mTop.getFraction();
            }
        };

        /**
         * Implementation of {@link Property} for overwriting the bottom attribute of
         * {@link BoundsRule} associated with this {@link ChildDrawable}. This allows users to
         * change the bounds rules as a percentage of parent size. This is preferable over
         * {@see PROPERTY_BOTTOM_ABSOLUTE} when the exact start/end position of scroll movement
         * isn't available at compile time.
         */
        public static final Property<CompositeDrawable.ChildDrawable, Float> BOTTOM_FRACTION
                = new Property<CompositeDrawable.ChildDrawable, Float>(
                Float.class, "fractionBottom") {

            @Override
            public void set(CompositeDrawable.ChildDrawable obj, Float value) {
                if (obj.getBoundsRule().mBottom == null) {
                    obj.getBoundsRule().mBottom = BoundsRule.inheritFromParent(value);
                } else {
                    obj.getBoundsRule().mBottom.setFraction(value);
                }

                obj.recomputeBounds();
            }

            @Override
            public Float get(CompositeDrawable.ChildDrawable obj) {
                return obj.getBoundsRule().mBottom.getFraction();
            }
        };

        /**
         * Implementation of {@link Property} for overwriting the bottom attribute of
         * {@link BoundsRule} associated with this {@link ChildDrawable}. This allows users to
         * change the bounds rules as a percentage of parent size. This is preferable over
         * {@see PROPERTY_LEFT_ABSOLUTE} when the exact start/end position of scroll movement
         * isn't available at compile time.
         */
        public static final Property<CompositeDrawable.ChildDrawable, Float> LEFT_FRACTION
                = new Property<CompositeDrawable.ChildDrawable, Float>(Float.class, "fractionLeft") {

            @Override
            public void set(CompositeDrawable.ChildDrawable obj, Float value) {
                if (obj.getBoundsRule().mLeft == null) {
                    obj.getBoundsRule().mLeft = BoundsRule.inheritFromParent(value);
                } else {
                    obj.getBoundsRule().mLeft.setFraction(value);
                }

                obj.recomputeBounds();
            }

            @Override
            public Float get(CompositeDrawable.ChildDrawable obj) {
                return obj.getBoundsRule().mLeft.getFraction();
            }
        };

        /**
         * Implementation of {@link Property} for overwriting the bottom attribute of
         * {@link BoundsRule} associated with this {@link ChildDrawable}. This allows users to
         * change the bounds rules as a percentage of parent size. This is preferable over
         * {@see PROPERTY_RIGHT_ABSOLUTE} when the exact start/end position of scroll movement
         * isn't available at compile time.
         */
        public static final Property<CompositeDrawable.ChildDrawable, Float> RIGHT_FRACTION
                = new Property<CompositeDrawable.ChildDrawable, Float>(
                Float.class, "fractoinRight") {

            @Override
            public void set(CompositeDrawable.ChildDrawable obj, Float value) {
                if (obj.getBoundsRule().mRight == null) {
                    obj.getBoundsRule().mRight = BoundsRule.inheritFromParent(value);
                } else {
                    obj.getBoundsRule().mRight.setFraction(value);
                }

                obj.recomputeBounds();
            }

            @Override
            public Float get(CompositeDrawable.ChildDrawable obj) {
                return obj.getBoundsRule().mRight.getFraction();
            }
        };
    }
}
