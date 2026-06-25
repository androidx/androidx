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
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.LayoutComponent;
import androidx.compose.remote.core.operations.layout.managers.policies.BaseModernMeasurePolicy;
import androidx.compose.remote.core.operations.layout.managers.policies.EnforceConstraintsMeasurePolicy;
import androidx.compose.remote.core.operations.layout.managers.policies.InlineExpressionMeasurePolicy;
import androidx.compose.remote.core.operations.layout.managers.policies.InsetWrapMeasurePolicy;
import androidx.compose.remote.core.operations.layout.managers.policies.LegacyMeasurePolicy;
import androidx.compose.remote.core.operations.layout.managers.policies.MeasurePolicy;
import androidx.compose.remote.core.operations.layout.measure.ComponentMeasure;
import androidx.compose.remote.core.operations.layout.measure.Measurable;
import androidx.compose.remote.core.operations.layout.measure.MeasurePass;
import androidx.compose.remote.core.operations.layout.measure.Size;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Base class for layout managers -- resizable components. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class LayoutManager extends LayoutComponent implements Measurable {

    private final @NonNull Size mCachedWrapSize = new Size(0f, 0f);

    @SuppressWarnings("UnusedVariable")
    private static final int INSET_WRAP_MEASURE = 2;

    @SuppressWarnings("UnusedVariable")
    private static final int INLINE_EXPRESSION_MEASURE = 3;
    private static final int ENFORCE_CONSTRAINTS = 4;

    public static final int DEFAULT_MEASURE_TYPE = ENFORCE_CONSTRAINTS;

    public static final int FIX_TOUCH_EVENT = 1;
    public static final int DEFAULT_TOUCH_VERSION = FIX_TOUCH_EVENT;

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
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            boolean horizontalWrap,
            boolean verticalWrap,
            @NonNull MeasurePass measure,
            @NonNull Size size) {
        // nothing here
    }

    /** Computes wrap size for this layout manager and returns the measured size. */
    public @NonNull Size runComputeWrapSize(
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            boolean horizontalWrap,
            boolean verticalWrap,
            @NonNull MeasurePass measure) {
        mCachedWrapSize.setWidth(0f);
        mCachedWrapSize.setHeight(0f);
        computeWrapSize(
                context,
                minWidth,
                maxWidth,
                minHeight,
                maxHeight,
                horizontalWrap,
                verticalWrap,
                measure,
                mCachedWrapSize);
        return mCachedWrapSize;
    }

    @Override
    public float minIntrinsicHeight(@NonNull RemoteContext context) {
        float height = computeModifierDefinedHeight(context, true);
        for (Component c : mChildrenComponents) {
            height = Math.max(c.minIntrinsicHeight(context), height);
        }
        return height;
    }

    @Override
    public float minIntrinsicWidth(@NonNull RemoteContext context) {
        float width = computeModifierDefinedWidth(context, true);
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

    public boolean isInFillParentMaxWidth() {
        return mWidthModifier.isFillParentMaxWidth();
    }

    public boolean isInVerticalFill() {
        return mHeightModifier.isFill();
    }

    public boolean isInFillParentMaxHeight() {
        return mHeightModifier.isFillParentMaxHeight();
    }

    /** Updates component variables and applies operations based on measured dimensions. */
    public void updateComponentValues(
            @NonNull PaintContext context, float measuredWidth, float measuredHeight) {
        Component prev = context.getContext().mLastComponent;
        context.getContext().mLastComponent = this;
        updateComponentValues(context.getContext(), measuredWidth, measuredHeight);
        for (Operation operation : mList) {
            if (operation.isDirty() && operation instanceof VariableSupport) {
                ((VariableSupport) operation).updateVariables(context.getContext());
                operation.apply(context.getContext());
            }
        }
        context.getContext().mLastComponent = prev;
    }

    private static final MeasurePolicy[] POLICIES = new MeasurePolicy[] {
        LegacyMeasurePolicy.INSTANCE,               // Version 0 (v0_4_0)
        BaseModernMeasurePolicy.INSTANCE,           // Version 1 (Base Modern)
        InsetWrapMeasurePolicy.INSTANCE,            // Version 2 (Inset Wrapping)
        InlineExpressionMeasurePolicy.INSTANCE,     // Version 3 (Inline Expressions)
        EnforceConstraintsMeasurePolicy.INSTANCE    // Version 4 (Constraints Enforcement)
    };

    /** Base implementation of the measure resolution */
    @Override
    public void measure(
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            @NonNull MeasurePass measure) {
        int version = Math.min(context.getMeasureVersion(), POLICIES.length - 1);
        POLICIES[version].measure(this, context, minWidth, maxWidth, minHeight, maxHeight, measure);
    }

    /** Returns whether the component has horizontal scrolling enabled. */
    public boolean hasHorizontalScroll() {
        return mComponentModifiers.hasHorizontalScroll();
    }

    /** Returns whether the component has intrinsic horizontal dimensions. */
    public boolean hasHorizontalIntrinsicDimension() {
        return hasHorizontalScroll();
    }

    /** Returns whether the component has intrinsic vertical dimensions. */
    public boolean hasVerticalIntrinsicDimension() {
        return hasVerticalScroll();
    }

    /** Returns whether the component has vertical scrolling enabled. */
    public boolean hasVerticalScroll() {
        return mComponentModifiers.hasVerticalScroll();
    }

    public @Nullable Component getContent() {
        return mContent;
    }

    public int getScheduledVisibility() {
        return mScheduledVisibility;
    }

    public void setScheduledVisibility(int visibility) {
        mScheduledVisibility = visibility;
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

    /** Only layout self, not children */
    public void selfLayout(@NonNull RemoteContext context, @NonNull MeasurePass measure) {
        super.layout(context, measure);
        ComponentMeasure self = measure.get(this);

        mComponentModifiers.layout(context, this, self.getW(), self.getH());
        this.mNeedsMeasure = false;
    }
}
