/*
 * Copyright (C) 2023 The Android Open Source Project
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
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.LayoutComponent;
import androidx.compose.remote.core.operations.layout.measure.ComponentMeasure;
import androidx.compose.remote.core.operations.layout.measure.MeasurePass;
import androidx.compose.remote.core.operations.layout.measure.Size;
import androidx.compose.remote.core.operations.layout.modifiers.CollapsiblePriorityModifierOperation;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CollapsibleColumnLayout extends ColumnLayout {

    public CollapsibleColumnLayout(
            @Nullable Component parent,
            int componentId,
            int animationId,
            float x,
            float y,
            float width,
            float height,
            int horizontalPositioning,
            int verticalPositioning,
            float spacedBy) {
        super(
                parent,
                componentId,
                animationId,
                x,
                y,
                width,
                height,
                horizontalPositioning,
                verticalPositioning,
                spacedBy);
    }

    public CollapsibleColumnLayout(
            @Nullable Component parent,
            int componentId,
            int animationId,
            int horizontalPositioning,
            int verticalPositioning,
            float spacedBy) {
        super(
                parent,
                componentId,
                animationId,
                horizontalPositioning,
                verticalPositioning,
                spacedBy);
    }

    @NonNull
    @Override
    protected String getSerializedName() {
        return "COLLAPSIBLE_COLUMN";
    }

    /**
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return Operations.LAYOUT_COLLAPSIBLE_COLUMN;
    }

    /**
     * Write the operation to the buffer
     *
     * @param buffer wire buffer
     * @param componentId component id
     * @param animationId animation id (-1 if not set)
     * @param horizontalPositioning horizontal positioning rules
     * @param verticalPositioning vertical positioning rules
     * @param spacedBy spaced by value
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int componentId,
            int animationId,
            int horizontalPositioning,
            int verticalPositioning,
            float spacedBy) {
        buffer.start(id());
        buffer.writeInt(componentId);
        buffer.writeInt(animationId);
        buffer.writeInt(horizontalPositioning);
        buffer.writeInt(verticalPositioning);
        buffer.writeFloat(spacedBy);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int componentId = buffer.readInt();
        int animationId = buffer.readInt();
        int horizontalPositioning = buffer.readInt();
        int verticalPositioning = buffer.readInt();
        float spacedBy = buffer.readFloat();
        operations.add(
                new CollapsibleColumnLayout(
                        null,
                        componentId,
                        animationId,
                        horizontalPositioning,
                        verticalPositioning,
                        spacedBy));
    }

    @Override
    public float minIntrinsicHeight(@NonNull RemoteContext context) {
        float height = computeModifierDefinedHeight(context);
        if (!mChildrenComponents.isEmpty()) {
            height += mChildrenComponents.get(0).minIntrinsicHeight(context);
        }
        return height;
    }

    @Override
    public float minIntrinsicWidth(@NonNull RemoteContext context) {
        float width = computeModifierDefinedWidth(context);
        if (!mChildrenComponents.isEmpty()) {
            width += mChildrenComponents.get(0).minIntrinsicWidth(context);
        }
        return width;
    }

    @Override
    public boolean hasVerticalIntrinsicDimension() {
        return true;
    }

    @Override
    public void computeWrapSize(
            @NonNull PaintContext context,
            float maxWidth,
            float maxHeight,
            boolean horizontalWrap,
            boolean verticalWrap,
            @NonNull MeasurePass measure,
            @NonNull Size size) {
        computeVisibleChildren(context, maxWidth, maxHeight, verticalWrap, measure, size);
    }

    @Override
    public void computeSize(
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            @NonNull MeasurePass measure) {
        computeVisibleChildren(context, maxWidth, maxHeight, false, measure, null);
    }

    @Override
    public void internalLayoutMeasure(@NonNull PaintContext context, @NonNull MeasurePass measure) {
        // if needed, take care of weight calculations
        super.internalLayoutMeasure(context, measure);
        // Check again for visibility
        ComponentMeasure m = measure.get(this);
        computeVisibleChildren(context, m.getW(), m.getH(), false, measure, null);
    }

    private void computeVisibleChildren(
            @NonNull PaintContext context,
            float maxWidth,
            float maxHeight,
            boolean verticalWrap,
            @NonNull MeasurePass measure,
            @Nullable Size size) {
        int visibleChildren = 0;
        ComponentMeasure self = measure.get(this);
        self.addVisibilityOverride(Visibility.OVERRIDE_VISIBLE);
        float currentMaxHeight = maxHeight;
        boolean hasPriorities = false;
        for (Component c : mChildrenComponents) {
            if (!measure.contains(c.getComponentId())) {
                // No need to remeasure here if already done
                if (c instanceof CollapsibleColumnLayout) {
                    c.measure(context, 0f, maxWidth, 0f, currentMaxHeight, measure);
                } else {
                    c.measure(context, 0f, maxWidth, 0f, Float.MAX_VALUE, measure);
                }
            }

            ComponentMeasure m = measure.get(c);
            if (!m.isGone()) {
                if (size != null) {
                    size.setWidth(Math.max(size.getWidth(), m.getW()));
                    size.setHeight(size.getHeight() + m.getH());
                }
                visibleChildren++;
                currentMaxHeight -= m.getH();
            }
            if (c instanceof LayoutComponent) {
                LayoutComponent lc = (LayoutComponent) c;
                CollapsiblePriorityModifierOperation priority =
                        lc.selfOrModifier(CollapsiblePriorityModifierOperation.class);
                if (priority != null) {
                    hasPriorities = true;
                }
            }
        }
        if (!mChildrenComponents.isEmpty() && size != null) {
            size.setHeight(size.getHeight() + (mSpacedBy * (visibleChildren - 1)));
        }

        float childrenWidth = 0f;
        float childrenHeight = 0f;

        boolean overflow = false;
        ArrayList<Component> children = mChildrenComponents;
        if (hasPriorities) {
            // TODO: We need to cache this.
            children =
                    CollapsiblePriority.sortWithPriorities(
                            mChildrenComponents, CollapsiblePriority.VERTICAL);
        }
        for (Component child : children) {
            ComponentMeasure childMeasure = measure.get(child);
            if (overflow || childMeasure.isGone()) {
                childMeasure.addVisibilityOverride(Visibility.OVERRIDE_GONE);
                continue;
            }
            float childHeight = childMeasure.getH();
            boolean childDoesNotFits = childrenHeight + childHeight > maxHeight;
            if (childDoesNotFits) {
                childMeasure.addVisibilityOverride(Visibility.OVERRIDE_GONE);
                overflow = true;
            } else {
                childrenHeight += childHeight;
                childrenWidth = Math.max(childrenWidth, childMeasure.getW());
                visibleChildren++;
            }
        }
        if (verticalWrap && size != null) {
            size.setHeight(Math.min(maxHeight, childrenHeight));
        }
        if (visibleChildren == 0 || (size != null && size.getHeight() <= 0f)) {
            self.addVisibilityOverride(Visibility.OVERRIDE_GONE);
        }
    }
}
