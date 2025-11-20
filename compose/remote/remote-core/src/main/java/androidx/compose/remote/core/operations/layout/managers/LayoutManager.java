/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.core.operations.layout.managers;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.LayoutComponent;
import androidx.compose.remote.core.operations.layout.measure.ComponentMeasure;
import androidx.compose.remote.core.operations.layout.measure.Measurable;
import androidx.compose.remote.core.operations.layout.measure.MeasurePass;
import androidx.compose.remote.core.operations.layout.measure.Size;
import androidx.compose.remote.core.operations.layout.modifiers.HeightInModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.WidthInModifierOperation;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Base class for layout managers -- resizable components. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class LayoutManager extends LayoutComponent implements Measurable {

    @NonNull Size mCachedWrapSize = new Size(0f, 0f);

    public LayoutManager(
            @Nullable Component parent,
            int componentId,
            int animationId,
            float x,
            float y,
            float width,
            float height) {
        super(parent, componentId, animationId, x, y, width, height);
    }

    /**
     * Allows layout managers to override elements visibility
     *
     * @param selfWidth intrinsic width of the layout manager content
     * @param selfHeight intrinsic height of the layout manager content
     * @param measure measure pass
     */
    public boolean applyVisibility(
            float selfWidth, float selfHeight, @NonNull MeasurePass measure) {
        return false;
    }

    /** Implemented by subclasses to provide a layout/measure pass */
    public void internalLayoutMeasure(@NonNull PaintContext context, @NonNull MeasurePass measure) {
        // nothing here
    }

    /** Subclasses can implement this to provide wrap sizing */
    public void computeWrapSize(
            @NonNull PaintContext context,
            float maxWidth,
            float maxHeight,
            boolean horizontalWrap,
            boolean verticalWrap,
            @NonNull MeasurePass measure,
            @NonNull Size size) {
        // nothing here
    }

    @Override
    public float minIntrinsicHeight(@Nullable RemoteContext context) {
        float height = computeModifierDefinedHeight(context);
        for (Component c : mChildrenComponents) {
            height = Math.max(c.minIntrinsicHeight(context), height);
        }
        return height;
    }

    @Override
    public float minIntrinsicWidth(@Nullable RemoteContext context) {
        float width = computeModifierDefinedWidth(context);
        for (Component c : mChildrenComponents) {
            width = Math.max(c.minIntrinsicWidth(context), width);
        }
        return width;
    }

    /** Subclasses can implement this when not in wrap sizing */
    public void computeSize(
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            @NonNull MeasurePass measure) {
        // nothing here
    }

    protected boolean childrenHaveHorizontalWeights() {
        for (Component c : mChildrenComponents) {
            if (c instanceof LayoutManager) {
                LayoutManager m = (LayoutManager) c;
                if (m.getWidthModifier() != null && m.getWidthModifier().hasWeight()) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean childrenHaveVerticalWeights() {
        for (Component c : mChildrenComponents) {
            if (c instanceof LayoutManager) {
                LayoutManager m = (LayoutManager) c;
                if (m.getHeightModifier() != null && m.getHeightModifier().hasWeight()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isInHorizontalFill() {
        return mWidthModifier.isFill();
    }

    public boolean isInVerticalFill() {
        return mHeightModifier.isFill();
    }

    private void measure_v0_4_0(
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            @NonNull MeasurePass measure) {
        boolean hasWrap = true;

        float measuredWidth = Math.min(maxWidth, computeModifierDefinedWidth(context.getContext()));
        float measuredHeight =
                Math.min(maxHeight, computeModifierDefinedHeight(context.getContext()));

        if (mWidthModifier.isIntrinsicMin()) {
            maxWidth = minIntrinsicWidth(context.getContext()) + mPaddingLeft + mPaddingRight;
        }
        if (mHeightModifier.isIntrinsicMin()) {
            maxHeight = minIntrinsicHeight(context.getContext()) + mPaddingTop + mPaddingBottom;
        }

        float insetMaxWidth = maxWidth - mPaddingLeft - mPaddingRight;
        float insetMaxHeight = maxHeight - mPaddingTop - mPaddingBottom;

        boolean hasHorizontalWrap = mWidthModifier.isWrap();
        boolean hasVerticalWrap = mHeightModifier.isWrap();
        if (hasHorizontalWrap || hasVerticalWrap) { // TODO: potential npe -- bbade@
            mCachedWrapSize.setWidth(0f);
            mCachedWrapSize.setHeight(0f);
            computeWrapSize(
                    context,
                    insetMaxWidth,
                    insetMaxHeight,
                    mWidthModifier.isWrap(),
                    mHeightModifier.isWrap(),
                    measure,
                    mCachedWrapSize);
            int selfVisibilityAfterMeasure = measure.get(this).getVisibility();
            if (Visibility.hasOverride(selfVisibilityAfterMeasure)
                    && mScheduledVisibility != selfVisibilityAfterMeasure) {
                mScheduledVisibility = selfVisibilityAfterMeasure;
            }
            measuredWidth = mCachedWrapSize.getWidth();
            if (hasHorizontalWrap) {
                measuredWidth += mPaddingLeft + mPaddingRight;
            }
            measuredHeight = mCachedWrapSize.getHeight();
            if (hasVerticalWrap) {
                measuredHeight += mPaddingTop + mPaddingBottom;
            }
        } else {
            hasWrap = false;
        }

        if (isInHorizontalFill()) {
            measuredWidth = maxWidth;
        } else if (mWidthModifier.hasWeight()) {
            measuredWidth =
                    Math.max(measuredWidth, computeModifierDefinedWidth(context.getContext()));
        } else {
            measuredWidth = Math.max(measuredWidth, minWidth);
            measuredWidth = Math.min(measuredWidth, maxWidth);
        }
        if (isInVerticalFill()) { // todo: potential npe -- bbade@
            measuredHeight = maxHeight;
        } else if (mHeightModifier.hasWeight()) {
            measuredHeight =
                    Math.max(measuredHeight, computeModifierDefinedHeight(context.getContext()));
        } else {
            measuredHeight = Math.max(measuredHeight, minHeight);
            measuredHeight = Math.min(measuredHeight, maxHeight);
        }
        if (minWidth == maxWidth) {
            measuredWidth = maxWidth;
        }
        if (minHeight == maxHeight) {
            measuredHeight = maxHeight;
        }

        if (!hasWrap) {
            if (hasHorizontalIntrinsicDimension()) {
                mCachedWrapSize.setWidth(0f);
                mCachedWrapSize.setHeight(0f);
                computeWrapSize(
                        context,
                        Float.MAX_VALUE,
                        maxHeight,
                        false,
                        false,
                        measure,
                        mCachedWrapSize);
                float w = mCachedWrapSize.getWidth();
                if (hasHorizontalScroll()) {
                    computeSize(context, 0f, w, 0, measuredHeight, measure);
                    mComponentModifiers.setHorizontalScrollDimension(measuredWidth, w);
                } else {
                    computeSize(
                            context,
                            0f,
                            Math.min(measuredWidth, insetMaxWidth),
                            0,
                            Math.min(measuredHeight, insetMaxHeight),
                            measure);
                }
            } else if (hasVerticalIntrinsicDimension()) {
                mCachedWrapSize.setWidth(0f);
                mCachedWrapSize.setHeight(0f);
                computeWrapSize(
                        context, maxWidth, Float.MAX_VALUE, false, false, measure, mCachedWrapSize);
                float h = mCachedWrapSize.getHeight();
                if (hasVerticalScroll()) {
                    computeSize(context, 0f, measuredWidth, 0, h, measure);
                    mComponentModifiers.setVerticalScrollDimension(measuredHeight, h);
                } else {
                    computeSize(
                            context,
                            0f,
                            Math.min(measuredWidth, insetMaxWidth),
                            0,
                            Math.min(measuredHeight, insetMaxHeight),
                            measure);
                }
            } else {
                float maxChildWidth = measuredWidth - mPaddingLeft - mPaddingRight;
                float maxChildHeight = measuredHeight - mPaddingTop - mPaddingBottom;
                computeSize(context, 0f, maxChildWidth, 0f, maxChildHeight, measure);
            }
        }

        if (mContent != null) {
            ComponentMeasure cm = measure.get(mContent);
            cm.setX(0f);
            cm.setY(0f);
            cm.setW(measuredWidth);
            cm.setH(measuredHeight);
        }

        ComponentMeasure m = measure.get(this);
        m.setW(measuredWidth);
        m.setH(measuredHeight);
        m.setVisibility(mScheduledVisibility);

        internalLayoutMeasure(context, measure);
    }

    private void measure_v0_4_1(
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            @NonNull MeasurePass measure) {

        float measuredWidth = Math.min(maxWidth, computeModifierDefinedWidth(context.getContext()));
        float measuredHeight =
                Math.min(maxHeight, computeModifierDefinedHeight(context.getContext()));

        if (mWidthModifier.isIntrinsicMin()) {
            maxWidth = minIntrinsicWidth(context.getContext()) + mPaddingLeft + mPaddingRight;
        }
        if (mHeightModifier.isIntrinsicMin()) {
            maxHeight = minIntrinsicHeight(context.getContext()) + mPaddingTop + mPaddingBottom;
        }
        WidthInModifierOperation widthIn = mWidthModifier.getWidthIn();
        if (widthIn != null) {
            minWidth = Math.max(minWidth, widthIn.getMin());
            maxWidth = Math.min(maxWidth, widthIn.getMax());
        }
        HeightInModifierOperation heightIn = mHeightModifier.getHeightIn();
        if (heightIn != null) {
            minHeight = Math.max(minHeight, heightIn.getMin());
            maxHeight = Math.min(maxHeight, heightIn.getMax());
        }

        float insetMaxWidth = maxWidth - mPaddingLeft - mPaddingRight;
        float insetMaxHeight = maxHeight - mPaddingTop - mPaddingBottom;

        boolean hasHorizontalWrap = false;
        boolean hasVerticalWrap = false;

        if (isInHorizontalFill()) {
            measuredWidth = maxWidth;
        } else if (mWidthModifier.hasWeight()) {
            measuredWidth =
                    Math.max(measuredWidth, computeModifierDefinedWidth(context.getContext()));
        } else {
            measuredWidth = Math.max(measuredWidth, minWidth);
            measuredWidth = Math.min(measuredWidth, maxWidth);
            hasHorizontalWrap = mWidthModifier.isWrap() || mWidthModifier.isIntrinsicMin();
        }

        if (isInVerticalFill()) {
            measuredHeight = maxHeight;
        } else if (mHeightModifier.hasWeight()) {
            measuredHeight =
                    Math.max(measuredHeight, computeModifierDefinedHeight(context.getContext()));
        } else {
            measuredHeight = Math.max(measuredHeight, minHeight);
            measuredHeight = Math.min(measuredHeight, maxHeight);
            hasVerticalWrap = mHeightModifier.isWrap() || mHeightModifier.isIntrinsicMin();
        }

        if (minWidth == maxWidth) {
            measuredWidth = maxWidth;
        }
        if (minHeight == maxHeight) {
            measuredHeight = maxHeight;
        }

        if (hasHorizontalWrap || hasVerticalWrap) {
            mCachedWrapSize.setWidth(0f);
            mCachedWrapSize.setHeight(0f);
            computeWrapSize(
                    context,
                    insetMaxWidth,
                    insetMaxHeight,
                    mWidthModifier.isWrap(),
                    mHeightModifier.isWrap(),
                    measure,
                    mCachedWrapSize);
            int selfVisibilityAfterMeasure = measure.get(this).getVisibility();
            if (Visibility.hasOverride(selfVisibilityAfterMeasure)
                    && mScheduledVisibility != selfVisibilityAfterMeasure) {
                mScheduledVisibility = selfVisibilityAfterMeasure;
            }
            if (hasHorizontalWrap) {
                measuredWidth = mCachedWrapSize.getWidth();
                measuredWidth += mPaddingLeft + mPaddingRight;
                measuredWidth = Math.max(measuredWidth, minWidth);
            }
            if (hasVerticalWrap) {
                measuredHeight = mCachedWrapSize.getHeight();
                measuredHeight += mPaddingTop + mPaddingBottom;
                measuredHeight = Math.max(measuredHeight, minHeight);
            }
        } else {
            if (hasHorizontalIntrinsicDimension()) {
                mCachedWrapSize.setWidth(0f);
                mCachedWrapSize.setHeight(0f);
                computeWrapSize(
                        context,
                        Float.MAX_VALUE,
                        maxHeight,
                        false,
                        false,
                        measure,
                        mCachedWrapSize);
                float w = mCachedWrapSize.getWidth();
                if (hasHorizontalScroll()) {
                    computeSize(context, 0f, w, 0, measuredHeight, measure);
                    mComponentModifiers.setHorizontalScrollDimension(measuredWidth, w);
                } else {
                    computeSize(
                            context,
                            0f,
                            Math.min(measuredWidth, insetMaxWidth),
                            0f,
                            Math.min(measuredHeight, insetMaxHeight),
                            measure);
                }
            } else if (hasVerticalIntrinsicDimension()) {
                mCachedWrapSize.setWidth(0f);
                mCachedWrapSize.setHeight(0f);
                computeWrapSize(
                        context, maxWidth, Float.MAX_VALUE, false, false, measure, mCachedWrapSize);
                float h = mCachedWrapSize.getHeight();
                if (hasVerticalScroll()) {
                    computeSize(context, 0f, measuredWidth, 0, h, measure);
                    mComponentModifiers.setVerticalScrollDimension(measuredHeight, h);
                } else {
                    computeSize(
                            context,
                            0f,
                            Math.min(measuredWidth, insetMaxWidth),
                            0f,
                            Math.min(measuredHeight, insetMaxHeight),
                            measure);
                }
            } else {
                float maxChildWidth = measuredWidth - mPaddingLeft - mPaddingRight;
                float maxChildHeight = measuredHeight - mPaddingTop - mPaddingBottom;
                computeSize(context, 0, maxChildWidth, 0, maxChildHeight, measure);
            }
        }

        if (mContent != null) {
            ComponentMeasure cm = measure.get(mContent);
            cm.setX(0f);
            cm.setY(0f);
            cm.setW(measuredWidth);
            cm.setH(measuredHeight);
        }

        measuredWidth = Math.max(measuredWidth, minWidth);
        measuredHeight = Math.max(measuredHeight, minHeight);

        ComponentMeasure m = measure.get(this);
        m.setW(measuredWidth);
        m.setH(measuredHeight);
        m.setVisibility(mScheduledVisibility);

        internalLayoutMeasure(context, measure);
    }

    /** Base implementation of the measure resolution */
    @Override
    public void measure(
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            @NonNull MeasurePass measure) {

        if (context.supportsVersion(0, 4, 1)) {
            measure_v0_4_1(context, minWidth, maxWidth, minHeight, maxHeight, measure);
        } else {
            measure_v0_4_0(context, minWidth, maxWidth, minHeight, maxHeight, measure);
        }
    }

    private boolean hasHorizontalScroll() {
        return mComponentModifiers.hasHorizontalScroll();
    }

    protected boolean hasHorizontalIntrinsicDimension() {
        return hasHorizontalScroll();
    }

    protected boolean hasVerticalIntrinsicDimension() {
        return hasVerticalScroll();
    }

    private boolean hasVerticalScroll() {
        return mComponentModifiers.hasVerticalScroll();
    }

    /** basic layout of internal components */
    @Override
    public void layout(@NonNull RemoteContext context, @NonNull MeasurePass measure) {
        super.layout(context, measure);
        ComponentMeasure self = measure.get(this);

        mComponentModifiers.layout(context, this, self.getW(), self.getH());
        for (Component c : mChildrenComponents) {
            c.layout(context, measure);
        }
        this.mNeedsMeasure = false;
    }

    /**
     * Only layout self, not children
     *
     * @param context
     * @param measure
     */
    public void selfLayout(@NonNull RemoteContext context, @NonNull MeasurePass measure) {
        super.layout(context, measure);
        ComponentMeasure self = measure.get(this);

        mComponentModifiers.layout(context, this, self.getW(), self.getH());
        this.mNeedsMeasure = false;
    }
}
