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

import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.LayoutComponent;
import androidx.compose.remote.core.operations.layout.measure.ComponentMeasure;
import androidx.compose.remote.core.operations.layout.measure.MeasurePass;
import androidx.compose.remote.core.operations.layout.measure.Size;
import androidx.compose.remote.core.operations.layout.modifiers.HeightInModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.WidthInModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** FitBox layout implementation -- only display the child that fits in the available space */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FitBoxLayout extends LayoutManager {

    public static final int START = 1;
    public static final int CENTER = 2;
    public static final int END = 3;
    public static final int TOP = 4;
    public static final int BOTTOM = 5;

    int mHorizontalPositioning;
    int mVerticalPositioning;

    public FitBoxLayout(
            @Nullable Component parent,
            int componentId,
            int animationId,
            float x,
            float y,
            float width,
            float height,
            int horizontalPositioning,
            int verticalPositioning) {
        super(parent, componentId, animationId, x, y, width, height);
        mHorizontalPositioning = horizontalPositioning;
        mVerticalPositioning = verticalPositioning;
    }

    public FitBoxLayout(
            @Nullable Component parent,
            int componentId,
            int animationId,
            int horizontalPositioning,
            int verticalPositioning) {
        this(
                parent,
                componentId,
                animationId,
                0,
                0,
                0,
                0,
                horizontalPositioning,
                verticalPositioning);
    }

    @NonNull
    @Override
    public String toString() {
        return "FITBOX ["
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
        return "FITBOX";
    }

    @SuppressWarnings("UnusedVariable")
    private void computeWrapSizeOriginal(
            @NonNull PaintContext context,
            float minWidth, float maxWidth,
            float minHeight, float maxHeight,
            boolean horizontalWrap,
            boolean verticalWrap,
            @NonNull MeasurePass measure,
            @NonNull Size size) {

        boolean found = false;
        ComponentMeasure self = measure.get(this);
        for (Component c : mChildrenComponents) {
            float cw = 0f; // c.intrinsicWidth(context.getContext());
            float ch = 0f; // c.intrinsicHeight(context.getContext());
            if (c instanceof LayoutComponent) {
                LayoutComponent lc = (LayoutComponent) c;
                WidthModifierOperation widthModifier = lc.getWidthModifier();
                if (widthModifier != null) {
                    WidthInModifierOperation widthIn = lc.getWidthModifier().getWidthIn();
                    if (widthIn != null) {
                        cw = widthIn.getMin();
                    }
                }
                HeightModifierOperation heightModifier = lc.getHeightModifier();
                if (heightModifier != null) {
                    HeightInModifierOperation heightIn = lc.getHeightModifier().getHeightIn();
                    if (heightIn != null) {
                        ch = heightIn.getMin();
                    }
                }
            }
            c.measure(context, 0f, maxWidth, 0f, maxHeight, measure);
            ComponentMeasure m = measure.get(c);
            if (!found && cw <= maxWidth && ch <= maxHeight) {
                found = true;
                m.addVisibilityOverride(Visibility.OVERRIDE_VISIBLE);
                size.setWidth(m.getW());
                size.setHeight(m.getH());
            } else {
                m.addVisibilityOverride(Visibility.OVERRIDE_GONE);
            }
        }
        if (!found) {
            self.setVisibility(Visibility.GONE);
        } else {
            self.setVisibility(Visibility.VISIBLE);
        }
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

        if (context.useFeature(Header.FEATURE_PRIORITY_FIX)) {
            computeWrapSizePriorityFix(context, minWidth, maxWidth, minHeight, maxHeight,
                    horizontalWrap, verticalWrap, measure, size);
        } else {
            computeWrapSizeOriginal(context, minWidth, maxWidth, minHeight, maxHeight,
                    horizontalWrap, verticalWrap, measure, size);
        }
    }

    @SuppressWarnings("UnusedVariable")
    private void computeWrapSizePriorityFix(
            @NonNull PaintContext context,
            float minWidth, float maxWidth,
            float minHeight, float maxHeight,
            boolean horizontalWrap,
            boolean verticalWrap,
            @NonNull MeasurePass measure,
            @NonNull Size size) {

        boolean found = false;
        ComponentMeasure self = measure.get(this);
        self.clearVisibilityOverride();

        for (Component c : mChildrenComponents) {
            ComponentMeasure m = measure.get(c);
            m.clearVisibilityOverride();

            if (!found) {
                float childMinWidth = c.minIntrinsicWidth(context.getContext());
                float childMinHeight = c.minIntrinsicHeight(context.getContext());

                if (childMinWidth <= maxWidth && childMinHeight <= maxHeight) {
                    // Measure with actual constraints
                    c.measure(context, childMinWidth, maxWidth, childMinHeight, maxHeight, measure);
                    // Check if it fits (without shrinking)
                    if (m.getW() <= maxWidth && m.getH() <= maxHeight) {
                        found = true;
                        m.addVisibilityOverride(Visibility.OVERRIDE_VISIBLE);
                        size.setWidth(m.getW());
                        size.setHeight(m.getH());
                    } else {
                        m.addVisibilityOverride(Visibility.OVERRIDE_GONE);
                    }
                } else {
                    m.addVisibilityOverride(Visibility.OVERRIDE_GONE);
                }
            } else {
                m.addVisibilityOverride(Visibility.OVERRIDE_GONE);
            }
        }

        if (!found) {
            self.addVisibilityOverride(Visibility.OVERRIDE_GONE);
        } else {
            self.addVisibilityOverride(Visibility.OVERRIDE_VISIBLE);
        }
    }

    @Override
    public void computeSize(
            @NonNull PaintContext context,
            float minWidth, float maxWidth,
            float minHeight, float maxHeight,
            @NonNull MeasurePass measure) {
        if (context.useFeature(Header.FEATURE_PRIORITY_FIX)) {
            computeSizePriorityFix(context, minWidth, maxWidth, minHeight, maxHeight, measure);
        } else {
            computeSizeOriginal(context, minWidth, maxWidth, minHeight, maxHeight, measure);
        }
    }


    private void computeSizeOriginal(
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            @NonNull MeasurePass measure) {
        boolean found = false;
        for (Component c : mChildrenComponents) {
            float cw = 0f;
            float ch = 0f;
            if (c instanceof LayoutComponent) {
                LayoutComponent lc = (LayoutComponent) c;
                WidthModifierOperation widthModifier = lc.getWidthModifier();
                if (widthModifier != null) {
                    WidthInModifierOperation widthIn = lc.getWidthModifier().getWidthIn();
                    if (widthIn != null) {
                        cw = widthIn.getMin();
                    }
                }
                HeightModifierOperation heightModifier = lc.getHeightModifier();
                if (heightModifier != null) {
                    HeightInModifierOperation heightIn = lc.getHeightModifier().getHeightIn();
                    if (heightIn != null) {
                        ch = heightIn.getMin();
                    }
                }
            }
            c.measure(context, minWidth, maxWidth, minHeight, maxHeight, measure);
            //                child.measure(context, minWidth, Float.MAX_VALUE, minHeight,
            // Float.MAX_VALUE, measure);
            //               m.getVisibility().clearOverride();
            ComponentMeasure m = measure.get(c);
            //                m.setVisibility(Visibility.GONE);
            //                m.getVisibility().add(Visibility.OVERRIDE_GONE);
            // m.getVisibility().add(Visibility.OVERRIDE_GONE);
            m.clearVisibilityOverride();
            if (!found && cw <= maxWidth && ch <= maxHeight) {
                found = true;
                m.addVisibilityOverride(Visibility.OVERRIDE_VISIBLE);
            } else {
                m.addVisibilityOverride(Visibility.OVERRIDE_GONE);
            }
        }
    }

    @SuppressWarnings("UnusedVariable")
    private void computeSizePriorityFix(
            @NonNull PaintContext context,
            float minWidth, float maxWidth,
            float minHeight, float maxHeight,
            @NonNull MeasurePass measure) {
        boolean found = false;
        ComponentMeasure self = measure.get(this);
        self.clearVisibilityOverride();

        for (Component c : mChildrenComponents) {
            ComponentMeasure m = measure.get(c);
            m.clearVisibilityOverride();

            if (!found) {
                float childMinWidth = c.minIntrinsicWidth(context.getContext());
                float childMinHeight = c.minIntrinsicHeight(context.getContext());

                if (childMinWidth <= maxWidth && childMinHeight <= maxHeight) {
                    // Measure with actual constraints
                    c.measure(context, childMinWidth, maxWidth, childMinHeight, maxHeight, measure);
                    if (m.getW() <= maxWidth && m.getH() <= maxHeight) {
                        found = true;
                        m.addVisibilityOverride(Visibility.OVERRIDE_VISIBLE);
                    } else {
                        m.addVisibilityOverride(Visibility.OVERRIDE_GONE);
                    }
                } else {
                    m.addVisibilityOverride(Visibility.OVERRIDE_GONE);
                }
            } else {
                m.addVisibilityOverride(Visibility.OVERRIDE_GONE);
            }
        }

        if (!found) {
            self.addVisibilityOverride(Visibility.OVERRIDE_GONE);
        } else {
            self.addVisibilityOverride(Visibility.OVERRIDE_VISIBLE);
        }
    }

    @Override
    public void internalLayoutMeasure(@NonNull PaintContext context, @NonNull MeasurePass measure) {
        ComponentMeasure selfMeasure = measure.get(this);
        float selfWidth = selfMeasure.getW() - mPaddingLeft - mPaddingRight;
        float selfHeight = selfMeasure.getH() - mPaddingTop - mPaddingBottom;
        applyVisibility(selfWidth, selfHeight, measure);
        for (Component child : mChildrenComponents) {
            ComponentMeasure m = measure.get(child);
            float tx = 0f;
            float ty = 0f;
            switch (mVerticalPositioning) {
                case TOP:
                    ty = 0f;
                    break;
                case CENTER:
                    ty = (selfHeight - m.getH()) / 2f;
                    break;
                case BOTTOM:
                    ty = selfHeight - m.getH();
                    break;
            }
            switch (mHorizontalPositioning) {
                case START:
                    tx = 0f;
                    break;
                case CENTER:
                    tx = (selfWidth - m.getW()) / 2f;
                    break;
                case END:
                    tx = selfWidth - m.getW();
                    break;
            }
            m.setX(tx);
            m.setY(ty);
        }
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return "FitBoxLayout";
    }

    /**
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return Operations.LAYOUT_FIT_BOX;
    }

    /**
     * Write the operation to the buffer
     *
     * @param buffer                a WireBuffer
     * @param componentId           the component id
     * @param animationId           the component animation id
     * @param horizontalPositioning the horizontal positioning rules
     * @param verticalPositioning   the vertical positioning rules
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int componentId,
            int animationId,
            int horizontalPositioning,
            int verticalPositioning) {
        buffer.start(id());
        buffer.writeInt(componentId);
        buffer.writeInt(animationId);
        buffer.writeInt(horizontalPositioning);
        buffer.writeInt(verticalPositioning);
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
        operations.add(
                new FitBoxLayout(
                        null,
                        componentId,
                        animationId,
                        horizontalPositioning,
                        verticalPositioning));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Layout Managers", id(), name())
                .additionalDocumentation("fitbox")
                .description(
                        "FitBox layout implementation.\n\n"
                                + "Only displays the first child component that fits in the "
                                + "available"
                                + " space.")
                .field(INT, "componentId", "Unique ID for this component")
                .field(
                        INT,
                        "animationId",
                        "ID used to match components for animation purposes")
                .field(INT, "horizontalPositioning", "Horizontal positioning value")
                .possibleValues("START", START)
                .possibleValues("CENTER", CENTER)
                .possibleValues("END", END)
                .field(INT, "verticalPositioning", "Vertical positioning value")
                .possibleValues("TOP", TOP)
                .possibleValues("CENTER", CENTER)
                .possibleValues("BOTTOM", BOTTOM);
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mComponentId, mAnimationId, mHorizontalPositioning, mVerticalPositioning);
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        super.serialize(serializer);
        serializer.add("verticalPositioning", getPositioningString(mVerticalPositioning));
        serializer.add("horizontalPositioning", getPositioningString(mHorizontalPositioning));
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
            default:
                return "NONE";
        }
    }
}