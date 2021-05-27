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

package androidx.wear.tiles.renderer.internal;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

/**
 * A wrapper for a view, which enforces that its dimensions adhere to a set ratio if possible. Note
 * that while multiple children can be added, only the first child will be measured, laid out, and
 * drawn.
 *
 * <p>This will measure the child as normal, given the width/height MeasureSpecs assigned to this
 * object. If either (or both) the width and the height for the child are inexact (i.e.
 * WRAP_CONTENT), this wrapper will size those dimensions to be proportional to any known dimension.
 *
 * <p>As an example, say we add this wrapper to a FrameView, with width = MATCH_PARENT and height =
 * WRAP_CONTENT, with a ratio of 2 (i.e. width is double height). In this case, it will measure its
 * first child in the parent's bounds, as normal, then enforce that the height must be parentWidth /
 * 2.
 *
 * <p>Note that if both axes are exact, this container does nothing; it will simply size the child
 * and itself according to the exact MeasureSpecs.
 */
public class RatioViewWrapper extends ViewGroup {
    /**
     * An undefined aspect ratio. If {@link #setAspectRatio} is called with this value, or never
     * called, this wrapper may only be used with child views with {@code MeasureSpec.EXACTLY} for
     * both dimensions.
     */
    public static final float UNDEFINED_ASPECT_RATIO = -1;

    private static final float EPSILON = 0.00000000001f;
    private float mAspectRatio = UNDEFINED_ASPECT_RATIO;

    public RatioViewWrapper(@NonNull Context context) {
        this(context, null);
    }

    public RatioViewWrapper(@NonNull Context context, @Nullable AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public RatioViewWrapper(
            @NonNull Context context,
            @Nullable AttributeSet attributeSet,
            @AttrRes int defStyleAttr) {
        this(context, attributeSet, defStyleAttr, 0);
    }

    public RatioViewWrapper(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr,
            @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Sets the aspect ratio that this RatioViewWrapper should conform to. This will force the view
     * to have the dimensions width = aspect * height
     */
    public void setAspectRatio(float aspectRatio) {
        this.mAspectRatio = aspectRatio;
        requestLayout();
    }

    public float getAspectRatio() {
        return mAspectRatio;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getChildCount() != 1) {
            throw new IllegalStateException("RatioViewWrapper must contain a single child");
        }

        int widthMeasureMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMeasureMode = MeasureSpec.getMode(heightMeasureSpec);

        View childView = getChildAt(0);

        // Measure the child within the given bounds.
        childView.measure(widthMeasureSpec, heightMeasureSpec);

        // No aspect ratio. Trust the child and hope for the best.
        if (mAspectRatio == UNDEFINED_ASPECT_RATIO) {
            setMeasuredDimension(childView.getMeasuredWidth(), childView.getMeasuredHeight());
            combineMeasuredStates(getMeasuredState(), childView.getMeasuredState());
            return;
        }

        // If both are MeasureSpec.EXACTLY, we can't do anything else. Set our dimensions to be the
        // same and exit.
        if (widthMeasureMode == MeasureSpec.EXACTLY && heightMeasureMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(childView.getMeasuredWidth(), childView.getMeasuredHeight());
            return;
        }

        // If we've already hit our aspect ratio, exit.
        if (Math.abs(
                        (float) childView.getMeasuredWidth() / childView.getMeasuredHeight()
                                - mAspectRatio)
                <= EPSILON) {
            setMeasuredDimension(childView.getMeasuredWidth(), childView.getMeasuredHeight());
            return;
        }

        if ((widthMeasureMode == MeasureSpec.AT_MOST || widthMeasureMode == MeasureSpec.UNSPECIFIED)
                && (heightMeasureMode == MeasureSpec.AT_MOST
                        || heightMeasureMode == MeasureSpec.UNSPECIFIED)) {
            // Generally, this happens if this view has both width/height=WRAP_CONTENT. This can
            // also happen though if this view has both dimensions as MATCH_CONTENT, but the parent
            // view is WRAP_CONTENT. In that case, the parent will run a first view pass to get the
            // size of the children, then calculate its size and re-size this widget with EXACTLY
            // MeasureSpecs.
            //
            // In this case, let's just assume that the child has reached the maximum size that it
            // wants, so rescale the dimension that will make it _smaller_.
            float targetWidth = childView.getMeasuredHeight() * mAspectRatio;
            float targetHeight = childView.getMeasuredWidth() / mAspectRatio;

            if (targetWidth < childView.getMeasuredWidth()) {
                // Resize the width down
                int childWidth =
                        MeasureSpec.makeMeasureSpec((int) targetWidth, MeasureSpec.EXACTLY);
                int childHeight =
                        MeasureSpec.makeMeasureSpec(
                                childView.getMeasuredHeight(), MeasureSpec.EXACTLY);

                childView.measure(childWidth, childHeight);
                setMeasuredDimension(childView.getMeasuredWidth(), childView.getMeasuredHeight());
            } else if (targetHeight < childView.getMeasuredHeight()) {
                // Resize the height down
                int childWidth =
                        MeasureSpec.makeMeasureSpec(
                                childView.getMeasuredWidth(), MeasureSpec.EXACTLY);
                int childHeight =
                        MeasureSpec.makeMeasureSpec((int) targetHeight, MeasureSpec.EXACTLY);

                childView.measure(childWidth, childHeight);
                setMeasuredDimension(childView.getMeasuredWidth(), childView.getMeasuredHeight());
            } else {
                // This should have been picked up by the aspect ratio check above...
                throw new IllegalStateException(
                        "Neither target width nor target height was smaller than measured"
                                + " width/height");
            }
        } else if (widthMeasureMode == MeasureSpec.EXACTLY) {
            // Can't change the width, but can change height.
            float targetHeight = childView.getMeasuredWidth() / mAspectRatio;

            int childWidth =
                    MeasureSpec.makeMeasureSpec(childView.getMeasuredWidth(), MeasureSpec.EXACTLY);
            int childHeight = MeasureSpec.makeMeasureSpec((int) targetHeight, MeasureSpec.EXACTLY);

            childView.measure(childWidth, childHeight);

            // We're pulling some hacks here. We get an AT_MOST constraint, but if we oversize
            // ourselves, the parent container should do appropriate clipping.
            setMeasuredDimension(childView.getMeasuredWidth(), childView.getMeasuredHeight());
        } else if (heightMeasureMode == MeasureSpec.EXACTLY) {
            // Can't change height, change width.
            float targetWidth = childView.getMeasuredHeight() * mAspectRatio;

            int childWidth = MeasureSpec.makeMeasureSpec((int) targetWidth, MeasureSpec.EXACTLY);
            int childHeight =
                    MeasureSpec.makeMeasureSpec(childView.getMeasuredHeight(), MeasureSpec.EXACTLY);

            childView.measure(childWidth, childHeight);

            setMeasuredDimension(childView.getMeasuredWidth(), childView.getMeasuredHeight());
        } else {
            // This should never happen; the first if checks that both MeasureSpecs are either
            // AT_MOST or UNSPECIFIED. If that branch isn't taken, one of the MeasureSpecs must be
            // EXACTLY. It's technically possible to smash the flag bits though (mode == 3 is
            // invalid), so if we get here, that must have happened.
            throw new IllegalArgumentException("Unknown measure mode bits in given MeasureSpecs");
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View childView = getChildAt(0);

        // Place the child view within the bounds. If the child is greater than the bounds (i.e. one
        // of the constraints was MATCH_PARENT, and the other was free), then just align the
        // top-left for now.
        childView.layout(0, 0, childView.getMeasuredWidth(), childView.getMeasuredHeight());
    }

    // setPadding(Relative) should just pass straight through to the child; this View should just be
    // a wrapper, so should not itself introduce any extra spacing.
    //
    // We don't override the getters, since nothing in the layout tree should actually use them.
    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        View childView = getChildAt(0);
        childView.setPadding(left, top, right, bottom);
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        View childView = getChildAt(0);
        childView.setPaddingRelative(start, top, end, bottom);
    }
}
