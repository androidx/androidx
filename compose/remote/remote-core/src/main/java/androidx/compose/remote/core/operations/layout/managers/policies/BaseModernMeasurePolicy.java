/*
 * Copyright (C) 2026 The Android Open Source Project
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
package androidx.compose.remote.core.operations.layout.managers.policies;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.managers.LayoutManager;
import androidx.compose.remote.core.operations.layout.measure.ComponentMeasure;
import androidx.compose.remote.core.operations.layout.measure.MeasurePass;
import androidx.compose.remote.core.operations.layout.measure.Size;
import androidx.compose.remote.core.operations.layout.modifiers.DimensionInModifierOperation;

import org.jspecify.annotations.NonNull;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BaseModernMeasurePolicy implements MeasurePolicy {
    public static final BaseModernMeasurePolicy INSTANCE = new BaseModernMeasurePolicy();

    protected BaseModernMeasurePolicy() {}

    protected boolean shouldApplyInsetWrap() {
        return false;
    }

    protected boolean shouldUpdateComponentValues() {
        return false;
    }

    protected boolean shouldEnforceConstraints() {
        return false;
    }

    @Override
    public void measure(
            @NonNull LayoutManager layout,
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            @NonNull MeasurePass measure) {

        float measuredWidth = Math.min(maxWidth,
                layout.computeModifierDefinedWidth(context.getContext()));
        float measuredHeight =
                Math.min(maxHeight, layout.computeModifierDefinedHeight(context.getContext()));

        if (layout.getWidthModifier().isIntrinsicMin()) {
            maxWidth = layout.minIntrinsicWidth(context.getContext())
                    + layout.getPaddingLeft() + layout.getPaddingRight();
        }
        if (layout.getHeightModifier().isIntrinsicMin()) {
            maxHeight = layout.minIntrinsicHeight(context.getContext())
                    + layout.getPaddingTop() + layout.getPaddingBottom();
        }
        DimensionInModifierOperation widthIn = layout.getWidthModifier().getWidthIn();
        if (widthIn != null) {
            minWidth = Math.max(minWidth, widthIn.getMin());
            maxWidth = Math.min(maxWidth, widthIn.getMax());
        }
        DimensionInModifierOperation heightIn = layout.getHeightModifier().getHeightIn();
        if (heightIn != null) {
            minHeight = Math.max(minHeight, heightIn.getMin());
            maxHeight = Math.min(maxHeight, heightIn.getMax());
        }

        float insetMaxWidth = maxWidth - layout.getPaddingLeft() - layout.getPaddingRight();
        float insetMaxHeight = maxHeight - layout.getPaddingTop() - layout.getPaddingBottom();

        float oldViewportWidth = context.getContext().mViewportWidth;
        float oldViewportHeight = context.getContext().mViewportHeight;

        boolean hasHorizontalWrap = false;
        boolean hasVerticalWrap = false;

        if (layout.isInHorizontalFill()) {
            float fraction = layout.getWidthModifier().getValue();
            if (Float.isNaN(fraction) || layout.getWidthModifier().isExact()) {
                measuredWidth = maxWidth;
                minWidth = insetMaxWidth;
            } else {
                measuredWidth = maxWidth * fraction;
                minWidth = measuredWidth - layout.getPaddingLeft() - layout.getPaddingRight();
            }
        } else if (layout.isInFillParentMaxWidth()) {
            measuredWidth = maxWidth;
            float fraction = layout.getWidthModifier().getValue();
            if (Float.isNaN(fraction)) {
                fraction = 1f;
            }
            measuredWidth = context.getContext().mViewportWidth * fraction;
            minWidth = measuredWidth - layout.getPaddingLeft() - layout.getPaddingRight();
        } else if (layout.getWidthModifier().hasWeight()) {
            measuredWidth = Math.max(measuredWidth,
                    layout.computeModifierDefinedWidth(context.getContext()));
        } else {
            measuredWidth = Math.max(measuredWidth, minWidth);
            measuredWidth = Math.min(measuredWidth, maxWidth);
            hasHorizontalWrap = layout.getWidthModifier().isWrap()
                    || layout.getWidthModifier().isIntrinsicMin();
            if (shouldApplyInsetWrap()) {
                if (!hasHorizontalWrap) {
                    insetMaxWidth = measuredWidth
                            - layout.getPaddingLeft() - layout.getPaddingRight();
                }
            }
        }

        if (layout.isInVerticalFill()) {
            float fraction = layout.getHeightModifier().getValue();
            if (Float.isNaN(fraction) || layout.getHeightModifier().isExact()) {
                measuredHeight = maxHeight;
                minHeight = insetMaxHeight;
            } else {
                measuredHeight = maxHeight * fraction;
                minHeight = measuredHeight - layout.getPaddingTop() - layout.getPaddingBottom();
            }
        } else if (layout.isInFillParentMaxHeight()) {
            measuredHeight = maxHeight;
            float fraction = layout.getHeightModifier().getValue();
            if (Float.isNaN(fraction)) {
                fraction = 1f;
            }
            measuredHeight = context.getContext().mViewportHeight * fraction;
            minHeight = measuredHeight - layout.getPaddingTop() - layout.getPaddingBottom();
        } else if (layout.getHeightModifier().hasWeight()) {
            measuredHeight = Math.max(measuredHeight,
                    layout.computeModifierDefinedHeight(context.getContext()));
        } else {
            measuredHeight = Math.max(measuredHeight, minHeight);
            measuredHeight = Math.min(measuredHeight, maxHeight);
            hasVerticalWrap = layout.getHeightModifier().isWrap()
                    || layout.getHeightModifier().isIntrinsicMin();
            if (shouldApplyInsetWrap()) {
                if (!hasVerticalWrap) {
                    insetMaxHeight = measuredHeight
                            - layout.getPaddingTop() - layout.getPaddingBottom();
                }
            }
        }

        if (layout.hasHorizontalScroll()) {
            context.getContext().mViewportWidth = Math.min(measuredWidth, insetMaxWidth);
        }
        if (layout.hasVerticalScroll()) {
            context.getContext().mViewportHeight = Math.min(measuredHeight, insetMaxHeight);
        }

        if (shouldEnforceConstraints()) {
            measuredWidth = Math.max(measuredWidth, minWidth);
            measuredWidth = Math.min(measuredWidth, maxWidth);
            measuredHeight = Math.max(measuredHeight, minHeight);
            measuredHeight = Math.min(measuredHeight, maxHeight);
        }

        if (minWidth == maxWidth) {
            measuredWidth = maxWidth;
        }
        if (minHeight == maxHeight) {
            measuredHeight = maxHeight;
        }

        if (shouldUpdateComponentValues()) {
            layout.updateComponentValues(context, measuredWidth, measuredHeight);
        }

        if (hasHorizontalWrap || hasVerticalWrap) {
            Size wrapSize = layout.runComputeWrapSize(
                    context,
                    minWidth,
                    insetMaxWidth,
                    minHeight,
                    insetMaxHeight,
                    layout.getWidthModifier().isWrap(),
                    layout.getHeightModifier().isWrap(),
                    measure);
            int selfVisibilityAfterMeasure = measure.get(layout).getVisibility();
            if (Component.Visibility.hasOverride(selfVisibilityAfterMeasure)
                    && layout.getScheduledVisibility() != selfVisibilityAfterMeasure) {
                layout.setScheduledVisibility(selfVisibilityAfterMeasure);
            }
            if (hasHorizontalWrap) {
                measuredWidth = wrapSize.getWidth();
                measuredWidth += layout.getPaddingLeft() + layout.getPaddingRight();
                measuredWidth = Math.max(measuredWidth, minWidth);
            }
            if (hasVerticalWrap) {
                measuredHeight = wrapSize.getHeight();
                measuredHeight += layout.getPaddingTop() + layout.getPaddingBottom();
                measuredHeight = Math.max(measuredHeight, minHeight);
            }
            if (layout.hasHorizontalScroll()) {
                float w = layout.runComputeWrapSize(
                        context,
                        minWidth,
                        Float.MAX_VALUE,
                        minHeight,
                        maxHeight,
                        false,
                        false,
                        measure).getWidth();
                float internalHeight = Math.min(measuredHeight, insetMaxHeight);
                float hostWidth = Math.min(measuredWidth, insetMaxWidth);
                layout.computeSize(context, 0f, w, 0, internalHeight, measure);
                layout.getComponentModifiers().setHorizontalScrollDimension(hostWidth, w);
            }
            if (layout.hasVerticalScroll()) {
                float h;
                if (shouldApplyInsetWrap()) {
                    h = layout.runComputeWrapSize(
                            context,
                            insetMaxWidth,
                            insetMaxWidth,
                            insetMaxHeight,
                            Float.MAX_VALUE,
                            false,
                            false,
                            measure).getHeight();
                } else {
                    h = layout.runComputeWrapSize(
                            context,
                            minWidth,
                            maxWidth,
                            minHeight,
                            Float.MAX_VALUE,
                            false,
                            false,
                            measure).getHeight();
                }
                float internalWidth = Math.min(measuredWidth, insetMaxWidth);
                float hostHeight = Math.min(measuredHeight, insetMaxHeight);
                layout.computeSize(context, 0f, internalWidth, 0, h, measure);
                layout.getComponentModifiers().setVerticalScrollDimension(hostHeight, h);
            }
        } else {
            if (layout.hasHorizontalIntrinsicDimension()) {
                float w;
                if (shouldApplyInsetWrap()) {
                    w = layout.runComputeWrapSize(
                            context,
                            minWidth,
                            Float.MAX_VALUE,
                            minHeight,
                            insetMaxHeight,
                            false,
                            false,
                            measure).getWidth();
                } else {
                    w = layout.runComputeWrapSize(
                            context,
                            minWidth,
                            Float.MAX_VALUE,
                            minHeight,
                            maxHeight,
                            false,
                            false,
                            measure).getWidth();
                }
                if (layout.hasHorizontalScroll()) {
                    float internalHeight = Math.min(measuredHeight, insetMaxHeight);
                    float hostWidth = Math.min(measuredWidth, insetMaxWidth);
                    layout.computeSize(context, 0f, w, 0, internalHeight, measure);
                    layout.getComponentModifiers().setHorizontalScrollDimension(hostWidth, w);
                } else {
                    layout.computeSize(
                            context,
                            0f,
                            Math.min(measuredWidth, insetMaxWidth),
                            0f,
                            Math.min(measuredHeight, insetMaxHeight),
                            measure);
                }
            } else if (layout.hasVerticalIntrinsicDimension()) {
                float h;
                if (shouldApplyInsetWrap()) {
                    h = layout.runComputeWrapSize(
                            context,
                            minWidth,
                            insetMaxWidth,
                            minHeight,
                            Float.MAX_VALUE,
                            false,
                            false,
                            measure).getHeight();
                } else {
                    h = layout.runComputeWrapSize(
                            context,
                            minWidth,
                            maxWidth,
                            minHeight,
                            Float.MAX_VALUE,
                            false,
                            false,
                            measure).getHeight();
                }
                if (layout.hasVerticalScroll()) {
                    float internalWidth = Math.min(measuredWidth, insetMaxWidth);
                    float hostHeight = Math.min(measuredHeight, insetMaxHeight);
                    layout.computeSize(context, 0f, internalWidth, 0, h, measure);
                    layout.getComponentModifiers().setVerticalScrollDimension(hostHeight, h);
                } else {
                    layout.computeSize(
                            context,
                            0f,
                            Math.min(measuredWidth, insetMaxWidth),
                            0f,
                            Math.min(measuredHeight, insetMaxHeight),
                            measure);
                }
            } else {
                float maxChildWidth = measuredWidth
                        - layout.getPaddingLeft() - layout.getPaddingRight();
                float maxChildHeight = measuredHeight
                        - layout.getPaddingTop() - layout.getPaddingBottom();
                layout.computeSize(context, 0, maxChildWidth, 0, maxChildHeight, measure);
                int selfVisibilityAfterMeasure = measure.get(layout).getVisibility();
                if (Component.Visibility.hasOverride(selfVisibilityAfterMeasure)
                        && layout.getScheduledVisibility() != selfVisibilityAfterMeasure) {
                    layout.setScheduledVisibility(selfVisibilityAfterMeasure);
                }
            }
        }

        context.getContext().mViewportWidth = oldViewportWidth;
        context.getContext().mViewportHeight = oldViewportHeight;

        if (layout.getContent() != null) {
            ComponentMeasure cm = measure.get(layout.getContent());
            cm.setX(0f);
            cm.setY(0f);
            cm.setW(measuredWidth);
            cm.setH(measuredHeight);
        }

        measuredWidth = Math.max(measuredWidth, minWidth);
        measuredHeight = Math.max(measuredHeight, minHeight);

        if (shouldEnforceConstraints()) {
            measuredWidth = Math.min(measuredWidth, maxWidth);
            measuredHeight = Math.min(measuredHeight, maxHeight);

            measuredWidth = layout.applyWidthConstraints(measuredWidth);
            measuredHeight = layout.applyHeightConstraints(measuredHeight);
        }
        ComponentMeasure m = measure.get(layout);
        m.setW(measuredWidth);
        m.setH(measuredHeight);
        m.setVisibility(layout.getScheduledVisibility());

        layout.internalLayoutMeasure(context, measure);
    }
}
