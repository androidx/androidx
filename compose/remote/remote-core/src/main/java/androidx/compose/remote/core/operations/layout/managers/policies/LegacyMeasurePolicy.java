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

import org.jspecify.annotations.NonNull;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LegacyMeasurePolicy implements MeasurePolicy {
    public static final LegacyMeasurePolicy INSTANCE = new LegacyMeasurePolicy();

    private LegacyMeasurePolicy() {}

    @Override
    public void measure(
            @NonNull LayoutManager layout,
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            @NonNull MeasurePass measure) {
        boolean hasWrap = true;

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

        float insetMaxWidth = maxWidth - layout.getPaddingLeft() - layout.getPaddingRight();
        float insetMaxHeight = maxHeight - layout.getPaddingTop() - layout.getPaddingBottom();

        boolean hasHorizontalWrap = layout.getWidthModifier().isWrap();
        boolean hasVerticalWrap = layout.getHeightModifier().isWrap();
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
            measuredWidth = wrapSize.getWidth();
            if (hasHorizontalWrap) {
                measuredWidth += layout.getPaddingLeft() + layout.getPaddingRight();
            }
            measuredHeight = wrapSize.getHeight();
            if (hasVerticalWrap) {
                measuredHeight += layout.getPaddingTop() + layout.getPaddingBottom();
            }
        } else {
            hasWrap = false;
        }

        if (layout.isInHorizontalFill()) {
            measuredWidth = maxWidth;
        } else if (layout.getWidthModifier().hasWeight()) {
            measuredWidth = Math.max(measuredWidth,
                    layout.computeModifierDefinedWidth(context.getContext()));
        } else {
            measuredWidth = Math.max(measuredWidth, minWidth);
            measuredWidth = Math.min(measuredWidth, maxWidth);
        }
        if (layout.isInVerticalFill()) {
            measuredHeight = maxHeight;
        } else if (layout.getHeightModifier().hasWeight()) {
            measuredHeight = Math.max(measuredHeight,
                    layout.computeModifierDefinedHeight(context.getContext()));
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
            if (layout.hasHorizontalIntrinsicDimension()) {
                float w = layout.runComputeWrapSize(
                        context,
                        minWidth,
                        Float.MAX_VALUE,
                        minHeight,
                        maxHeight,
                        false,
                        false,
                        measure).getWidth();
                if (layout.hasHorizontalScroll()) {
                    layout.computeSize(context, 0f, w, 0, measuredHeight, measure);
                    layout.getComponentModifiers().setHorizontalScrollDimension(measuredWidth, w);
                } else {
                    layout.computeSize(
                            context,
                            0f,
                            Math.min(measuredWidth, insetMaxWidth),
                            0,
                            Math.min(measuredHeight, insetMaxHeight),
                            measure);
                }
            } else if (layout.hasVerticalIntrinsicDimension()) {
                float h = layout.runComputeWrapSize(
                        context,
                        minWidth,
                        maxWidth,
                        minHeight,
                        Float.MAX_VALUE,
                        false,
                        false,
                        measure).getHeight();
                if (layout.hasVerticalScroll()) {
                    layout.computeSize(context, 0f, measuredWidth, 0, h, measure);
                    layout.getComponentModifiers().setVerticalScrollDimension(measuredHeight, h);
                } else {
                    layout.computeSize(
                            context,
                            0f,
                            Math.min(measuredWidth, insetMaxWidth),
                            0,
                            Math.min(measuredHeight, insetMaxHeight),
                            measure);
                }
            } else {
                float maxChildWidth = measuredWidth
                        - layout.getPaddingLeft() - layout.getPaddingRight();
                float maxChildHeight = measuredHeight
                        - layout.getPaddingTop() - layout.getPaddingBottom();
                layout.computeSize(context, 0f, maxChildWidth, 0f, maxChildHeight, measure);
            }
        }

        if (layout.getContent() != null) {
            ComponentMeasure cm = measure.get(layout.getContent());
            cm.setX(0f);
            cm.setY(0f);
            cm.setW(measuredWidth);
            cm.setH(measuredHeight);
        }

        ComponentMeasure m = measure.get(layout);
        m.setW(measuredWidth);
        m.setH(measuredHeight);
        m.setVisibility(layout.getScheduledVisibility());

        layout.internalLayoutMeasure(context, measure);
    }
}
