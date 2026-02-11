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

import static androidx.compose.remote.core.documentation.DocumentedOperation.FLOAT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.LayoutComponent;
import androidx.compose.remote.core.operations.layout.measure.ComponentMeasure;
import androidx.compose.remote.core.operations.layout.measure.MeasurePass;
import androidx.compose.remote.core.operations.layout.measure.Size;
import androidx.compose.remote.core.operations.layout.modifiers.AlignByModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.WidthInModifierOperation;
import androidx.compose.remote.core.operations.layout.utils.DebugLog;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Simple Row layout implementation - also supports weight and horizontal/vertical positioning */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RowLayout extends LayoutManager {
    public static final int START = 1;
    public static final int CENTER = 2;
    public static final int END = 3;
    public static final int TOP = 4;
    public static final int BOTTOM = 5;
    public static final int SPACE_BETWEEN = 6;
    public static final int SPACE_EVENLY = 7;
    public static final int SPACE_AROUND = 8;

    int mHorizontalPositioning;
    int mVerticalPositioning;
    float mSpacedBy = 0f;

    public RowLayout(
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
        super(parent, componentId, animationId, x, y, width, height);
        mHorizontalPositioning = horizontalPositioning;
        mVerticalPositioning = verticalPositioning;
        mSpacedBy = spacedBy;
    }

    public RowLayout(
            @Nullable Component parent,
            int componentId,
            int animationId,
            int horizontalPositioning,
            int verticalPositioning,
            float spacedBy) {
        this(
                parent,
                componentId,
                animationId,
                0,
                0,
                0,
                0,
                horizontalPositioning,
                verticalPositioning,
                spacedBy);
    }

    @NonNull
    @Override
    public String toString() {
        return getSerializedName()
                + " ["
                + mComponentId
                + ":"
                + mAnimationId
                + "] ("
                + mX
                + ", "
                + mY
                + " - "
                + mWidth
                + " x "
                + mHeight
                + ") "
                + mVisibility;
    }

    @NonNull
    @Override
    protected String getSerializedName() {
        return "ROW";
    }

    @Override
    public boolean isInHorizontalFill() {
        return super.isInHorizontalFill() || childrenHaveHorizontalWeights();
    }

    @Override
    public void computeWrapSize(
            @NonNull PaintContext context,
            float minWidth, float maxWidth,
            float minHeight, float maxHeight,
            boolean horizontalWrap,
            boolean verticalWrap,
            @NonNull MeasurePass measure,
            @NonNull Size size) {
        computeWrapSize(context, minWidth, maxWidth, minHeight, maxHeight, horizontalWrap,
                verticalWrap,
                measure, size, mChildrenComponents);
    }

    /**
     * Compute the size of the component in wrap
     */
    public void computeWrapSize(
            @NonNull PaintContext context,
            float minWidth, float maxWidth,
            float minHeight, float maxHeight,
            boolean horizontalWrap,
            boolean verticalWrap,
            @NonNull MeasurePass measure,
            @NonNull Size size,
            @NonNull ArrayList<Component> components) {
        DebugLog.s(() -> "COMPUTE WRAP SIZE in " + this + " (" + mComponentId + ")");
        size.clear();
        int visibleChildrens = 0;
        float currentMaxWidth = maxWidth;

        float totalWeights = 0f;
        boolean hasWeights = false;
        for (Component child : components) {
            ComponentMeasure childMeasure = measure.get(child);
            if (childMeasure.isGone()) {
                continue;
            }
            if (child instanceof LayoutComponent
                    && ((LayoutComponent) child).getWidthModifier().hasWeight()) {
                hasWeights = true;
                totalWeights += ((LayoutComponent) child).getWidthModifier().getValue();
            }
        }

        if (hasWeights) {
            // we have to measure all children first that do not have weight
            for (Component c : components) {
                if (c instanceof LayoutComponent
                        && ((LayoutComponent) c).getWidthModifier().hasWeight()) {
                    continue;
                }
                c.measure(context, 0f, currentMaxWidth, 0f, maxHeight, measure);
                ComponentMeasure m = measure.get(c);
                if (!m.isGone()) {
                    size.setWidth(size.getWidth() + m.getW());
                    size.setHeight(Math.max(size.getHeight(), m.getH()));
                    visibleChildrens++;
                    currentMaxWidth -= m.getW();
                }
            }
            // Then we can measure the children with weight
            for (Component c : components) {
                if (!(c instanceof LayoutComponent
                        && ((LayoutComponent) c).getWidthModifier().hasWeight())) {
                    continue;
                }
                float childWeight = ((LayoutComponent) c).getWidthModifier().getValue();
                float childMinWidth = (childWeight * currentMaxWidth) / totalWeights;
                float childMaxWidth = childMinWidth;

                c.measure(context, childMinWidth, childMaxWidth, 0f, maxHeight, measure);
                ComponentMeasure m = measure.get(c);
                if (!m.isGone()) {
                    size.setWidth(size.getWidth() + m.getW());
                    size.setHeight(Math.max(size.getHeight(), m.getH()));
                    visibleChildrens++;
                }
            }
        } else {
            for (Component c : components) {
                c.measure(context, 0f, currentMaxWidth, 0f, maxHeight, measure);
                ComponentMeasure m = measure.get(c);
                if (!m.isGone()) {
                    size.setWidth(size.getWidth() + m.getW());
                    size.setHeight(Math.max(size.getHeight(), m.getH()));
                    visibleChildrens++;
                    currentMaxWidth -= m.getW();
                }
            }
        }

        if (!components.isEmpty()) {
            size.setWidth(size.getWidth() + (mSpacedBy * (visibleChildrens - 1)));
        }
        DebugLog.e();
    }

    @Override
    public void computeSize(
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            @NonNull MeasurePass measure) {
        computeSize(context, minWidth, maxWidth, minHeight, maxHeight, measure,
                mChildrenComponents);
    }

    protected void computeSize(
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            @NonNull MeasurePass measure,
            @NonNull ArrayList<Component> components) {
        DebugLog.s(() -> "COMPUTE SIZE in " + this + " (" + mComponentId + ")");
        float mw = maxWidth;
        for (Component child : components) {
            child.measure(context, minWidth, mw, minHeight, maxHeight, measure);
            // TODO: Check if correct with FlowLayout
            ComponentMeasure m = measure.get(child);
            if (!m.isGone()) {
                mw -= m.getW();
            }
        }
        DebugLog.e();
    }

    @Override
    public float minIntrinsicWidth(@Nullable RemoteContext context) {
        return minIntrinsicWidth(context, mChildrenComponents);
    }

    protected float minIntrinsicWidth(@Nullable RemoteContext context,
            @NonNull ArrayList<Component> components) {
        float width = computeModifierDefinedWidth(context);
        float componentWidths = 0f;
        for (Component c : components) {
            componentWidths += c.minIntrinsicWidth(context);
        }
        return Math.max(width, componentWidths);
    }

    @Override
    public float minIntrinsicHeight(@Nullable RemoteContext context) {
        return minIntrinsicHeight(context, mChildrenComponents);
    }

    protected float minIntrinsicHeight(@Nullable RemoteContext context,
            @NonNull ArrayList<Component> components) {
        float height = computeModifierDefinedHeight(context);
        float componentHeights = 0f;
        for (Component c : components) {
            componentHeights = Math.max(componentHeights, c.minIntrinsicHeight(context));
        }
        return Math.max(height, componentHeights);
    }

    @Override
    public void internalLayoutMeasure(@NonNull PaintContext context, @NonNull MeasurePass measure) {
        ComponentMeasure selfMeasure = measure.get(this);
        DebugLog.s(
                () ->
                        "INTERNAL LAYOUT "
                                + this
                                + " ("
                                + mComponentId
                                + ") children: "
                                + mChildrenComponents.size()
                                + " size ("
                                + selfMeasure.getW()
                                + " x "
                                + selfMeasure.getH()
                                + ")");

        float selfWidth = selfMeasure.getW() - mPaddingLeft - mPaddingRight;
        float selfHeight = selfMeasure.getH() - mPaddingTop - mPaddingBottom;
        if (mComponentModifiers.hasHorizontalScroll()) {
            selfWidth =
                    mComponentModifiers.getHorizontalScrollDimension()
                            - mPaddingLeft
                            - mPaddingRight;
        }
        if (mComponentModifiers.hasVerticalScroll()) {
            selfHeight =
                    mComponentModifiers.getVerticalScrollDimension() - mPaddingTop - mPaddingBottom;
        }

        internalLayoutMeasure(context, measure, mChildrenComponents, selfWidth, selfHeight, 0f, 0f,
                null);
    }

    protected void internalLayoutMeasure(@NonNull PaintContext context,
            @NonNull MeasurePass measure,
            @NonNull ArrayList<Component> components, float selfWidth, float selfHeight,
            float positionX, float positionY,
            @Nullable Size size) {
        if (components.isEmpty()) {
            DebugLog.e();
            return;
        }
        float childrenWidth = 0f;
        float childrenHeight = 0f;

        boolean checkWeights = true;

        while (checkWeights) {
            checkWeights = false;
            childrenWidth = 0f;
            childrenHeight = 0f;
            boolean hasWeights = false;
            float totalWeights = 0f;
            for (Component child : components) {
                ComponentMeasure childMeasure = measure.get(child);
                if (childMeasure.isGone()) {
                    continue;
                }
                if (child instanceof LayoutComponent
                        && ((LayoutComponent) child).getWidthModifier().hasWeight()) {
                    hasWeights = true;
                    totalWeights += ((LayoutComponent) child).getWidthModifier().getValue();
                } else {
                    childrenWidth += childMeasure.getW();
                }
            }

            // TODO: need to move the weight measuring in the measure function,
            // currently we'll measure unnecessarily
            if (hasWeights) {
                float availableSpace = selfWidth - childrenWidth;
                for (Component child : components) {
                    if (child instanceof LayoutComponent
                            && ((LayoutComponent) child).getWidthModifier().hasWeight()) {
                        ComponentMeasure childMeasure = measure.get(child);
                        if (childMeasure.isGone()) {
                            continue;
                        }
                        float weight = ((LayoutComponent) child).getWidthModifier().getValue();
                        float childWidth = (weight * availableSpace) / totalWeights;
                        WidthInModifierOperation widthInConstraints =
                                ((LayoutComponent) child).getWidthModifier().getWidthIn();
                        if (widthInConstraints != null) {
                            float min = widthInConstraints.getMin();
                            float max = widthInConstraints.getMax();
                            if (min != -1) {
                                childWidth = Math.max(min, childWidth);
                            }
                            if (max != -1) {
                                childWidth = Math.min(max, childWidth);
                            }
                        }
                        childMeasure.setW(childWidth);
                        child.measure(
                                context,
                                childMeasure.getW(),
                                childMeasure.getW(),
                                childMeasure.getH(),
                                childMeasure.getH(),
                                measure);
                    }
                }
            }

            if (applyVisibility(selfWidth, selfHeight, measure) && hasWeights) {
                checkWeights = true;
            }
        }

        childrenWidth = 0f;
        int visibleChildrens = 0;
        boolean hasAlignBy = false;
        float alignByValue = 0f;
        for (Component child : components) {
            ComponentMeasure childMeasure = measure.get(child);
            if (childMeasure.isGone()) {
                continue;
            }
            childrenWidth += childMeasure.getW();
            childrenHeight = Math.max(childrenHeight, childMeasure.getH());
            visibleChildrens++;
            AlignByModifierOperation alignByModifier = child.selfOrModifier(
                    AlignByModifierOperation.class);
            if (alignByModifier != null) {
                hasAlignBy = true;
                alignByValue = Math.max(alignByValue, alignByModifier.getValue(context));
            }
        }

        childrenWidth += mSpacedBy * (visibleChildrens - 1);

        float tx = 0f;
        float ty = 0f;

        float horizontalGap = 0f;
        float total = 0f;

        switch (mHorizontalPositioning) {
            case START:
                tx = 0f;
                break;
            case END:
                tx = selfWidth - childrenWidth;
                break;
            case CENTER:
                tx = (selfWidth - childrenWidth) / 2f;
                break;
            case SPACE_BETWEEN:
                for (Component child : components) {
                    ComponentMeasure childMeasure = measure.get(child);
                    if (childMeasure.isGone()) {
                        continue;
                    }
                    total += childMeasure.getW();
                }
                if (visibleChildrens > 1) {
                    horizontalGap = (selfWidth - total) / (visibleChildrens - 1);
                } else {
                    // we center the element
                    tx = (selfWidth - childrenWidth) / 2f;
                }
                break;
            case SPACE_EVENLY:
                for (Component child : components) {
                    ComponentMeasure childMeasure = measure.get(child);
                    if (childMeasure.isGone()) {
                        continue;
                    }
                    total += childMeasure.getW();
                }
                horizontalGap = (selfWidth - total) / (visibleChildrens + 1);
                tx = horizontalGap;
                break;
            case SPACE_AROUND:
                for (Component child : components) {
                    ComponentMeasure childMeasure = measure.get(child);
                    if (childMeasure.isGone()) {
                        continue;
                    }
                    total += childMeasure.getW();
                }
                horizontalGap = (selfWidth - total) / visibleChildrens;
                tx = horizontalGap / 2f;
                break;
        }

        for (Component child : components) {
            ComponentMeasure childMeasure = measure.get(child);
            float alignByOffset = 0f;
            if (hasAlignBy) {
                AlignByModifierOperation alignByModifier = child.selfOrModifier(
                        AlignByModifierOperation.class);
                if (alignByModifier != null) {
                    alignByOffset = alignByModifier.getValue(context);
                }
            }
            switch (mVerticalPositioning) {
                case TOP:
                    ty = 0f;
                    if (hasAlignBy) {
                        ty += alignByValue - alignByOffset;
                    }
                    break;
                case CENTER:
                    ty = (selfHeight - childMeasure.getH()) / 2f;
                    if (hasAlignBy) {
                        ty = (selfHeight - childrenHeight) / 2f;
                        ty += alignByValue - alignByOffset;
                    }
                    break;
                case BOTTOM:
                    ty = selfHeight - childMeasure.getH();
                    if (hasAlignBy) {
                        ty = (selfHeight - childrenHeight);
                        ty += alignByValue - alignByOffset;
                    }
                    break;
            }
            childMeasure.setX(tx + positionX);
            childMeasure.setY(ty + positionY);
            if (childMeasure.isGone()) {
                continue;
            }
            tx += childMeasure.getW();
            if (mHorizontalPositioning == SPACE_BETWEEN
                    || mHorizontalPositioning == SPACE_AROUND
                    || mHorizontalPositioning == SPACE_EVENLY) {
                tx += horizontalGap;
            }
            tx += mSpacedBy;
        }
        if (size != null) {
            size.setWidth(childrenWidth);
            size.setHeight(childrenHeight);
        }
        DebugLog.e();
    }

    @Override
    public void getLocationInWindow(float @NonNull [] value, boolean forSelf) {
        super.getLocationInWindow(value, forSelf);

        if (!forSelf && mHorizontalScrollDelegate instanceof ScrollModifierOperation) {
            ScrollModifierOperation smo = (ScrollModifierOperation) mHorizontalScrollDelegate;

            value[0] += smo.getScrollX();
        }
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return "RowLayout";
    }

    /**
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return Operations.LAYOUT_ROW;
    }

    /**
     * Write the operation to the buffer
     *
     * @param buffer                wire buffer
     * @param componentId           component id
     * @param animationId           animation id (-1 if not set)
     * @param horizontalPositioning horizontal positioning rules
     * @param verticalPositioning   vertical positioning rules
     * @param spacedBy              spaced by value
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
     * @param buffer     the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int componentId = buffer.readInt();
        int animationId = buffer.readInt();
        int horizontalPositioning = buffer.readInt();
        int verticalPositioning = buffer.readInt();
        float spacedBy = buffer.readFloat();
        operations.add(
                new RowLayout(
                        null,
                        componentId,
                        animationId,
                        horizontalPositioning,
                        verticalPositioning,
                        spacedBy));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Layout Managers", id(), name())
                .additionalDocumentation("row")
                .description(
                        "Row layout implementation, positioning components one"
                                + " after the other horizontally.\n\n"
                                + "It supports weight and horizontal/vertical positioning.")
                .examplesDimension(400, 100)
                .exampleImage("Start", "layout-RowLayout-start-top.png")
                .exampleImage("Center", "layout-RowLayout-center-top.png")
                .exampleImage("End", "layout-RowLayout-end-top.png")
                .exampleImage("SpaceEvenly", "layout-RowLayout-space-evenly-top.png")
                .exampleImage("SpaceAround", "layout-RowLayout-space-around-top.png")
                .exampleImage("SpaceBetween", "layout-RowLayout-space-between-top.png")
                .field(INT, "componentId", "Unique ID for this component")
                .field(
                        INT,
                        "animationId",
                        "ID used to match components for animation purposes")
                .field(INT, "horizontalPositioning", "Horizontal positioning value")
                .possibleValues("START", START)
                .possibleValues("CENTER", CENTER)
                .possibleValues("END", END)
                .possibleValues("SPACE_BETWEEN", SPACE_BETWEEN)
                .possibleValues("SPACE_EVENLY", SPACE_EVENLY)
                .possibleValues("SPACE_AROUND", SPACE_AROUND)
                .field(INT, "verticalPositioning", "Vertical positioning value")
                .possibleValues("TOP", TOP)
                .possibleValues("CENTER", CENTER)
                .possibleValues("BOTTOM", BOTTOM)
                .field(FLOAT, "spacedBy", "Horizontal spacing between components");
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(
                buffer,
                mComponentId,
                mAnimationId,
                mHorizontalPositioning,
                mVerticalPositioning,
                mSpacedBy);
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        super.serialize(serializer);
        serializer.add("verticalPositioning", getPositioningString(mVerticalPositioning));
        serializer.add("horizontalPositioning", getPositioningString(mHorizontalPositioning));
        serializer.add("spacedBy", mSpacedBy);
    }

    private String getPositioningString(int pos) {
        switch (pos) {
            case START:
                return "START";
            case CENTER:
                return "CENTER";
            case END:
                return "END";
            case TOP:
                return "TOP";
            case BOTTOM:
                return "BOTTOM";
            case SPACE_BETWEEN:
                return "SPACE_BETWEEN";
            case SPACE_EVENLY:
                return "SPACE_EVENLY";
            case SPACE_AROUND:
                return "SPACE_AROUND";
            default:
                return "NONE";
        }
    }
}
