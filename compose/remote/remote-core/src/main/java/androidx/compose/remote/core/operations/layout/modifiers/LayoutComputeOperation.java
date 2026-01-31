/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.core.operations.layout.modifiers;

import static androidx.compose.remote.core.documentation.DocumentedOperation.BOOLEAN;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.LayoutCompute;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.DataDynamicListFloat;
import androidx.compose.remote.core.operations.layout.Container;
import androidx.compose.remote.core.operations.layout.LayoutComponent;
import androidx.compose.remote.core.operations.layout.measure.ComponentMeasure;
import androidx.compose.remote.core.operations.utilities.ArrayAccess;
import androidx.compose.remote.core.operations.utilities.CollectionsAccess;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Compute component position and measure via expressions */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LayoutComputeOperation extends Operation
        implements ModifierOperation, Container, VariableSupport, LayoutCompute {
    private static final int OP_CODE = Operations.LAYOUT_COMPUTE;
    private static final String CLASS_NAME = "LayoutComputeOperation";
    private static final boolean DEBUG = false;

    @NonNull
    public ArrayList<Operation> mList = new ArrayList<>();

    public static final int TYPE_MEASURE = 0;
    public static final int TYPE_POSITION = 1;

    private final int mType;
    private final int mBoundsId;
    private final boolean mAnimateChanges;
    private LayoutComponent mParent;

    public LayoutComputeOperation(int type, int boundsId, boolean animateChanges) {
        mType = type;
        mBoundsId = boundsId;
        mAnimateChanges = animateChanges;
    }

    @NonNull
    @Override
    public String toString() {
        if (mList.isEmpty()) {
            return "LayoutComputeOperation()";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("LayoutComputeOperation()");
        for (Operation op : mList) {
            builder.append('\n');
            builder.append(op);
        }
        return builder.toString();
    }

    /**
     * The returns a list to be filled
     *
     * @return list to be filled
     */
    @NonNull
    @Override
    public ArrayList<Operation> getList() {
        return mList;
    }

    /**
     * Returns the serialized name for this operation
     *
     * @return the serialized name
     */
    @NonNull
    public String serializedName() {
        return "LAYOUT_COMPUTE";
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("list", mList);
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        for (Operation op : mList) {
            if (op instanceof VariableSupport) {
                ((VariableSupport) op).registerListening(context);
            }
        }
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        boolean needsInvalidate = false;
        for (Operation op : mList) {
            if (op instanceof VariableSupport && op.isDirty()) {
                ((VariableSupport) op).updateVariables(context);
                op.apply(context);
                op.markNotDirty();
                needsInvalidate = true;
            }
        }
        if (needsInvalidate) {
            mParent.invalidateMeasure();
        }
    }

    @Override
    public void markDirty() {
        // nothing
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mType, mBoundsId, mAnimateChanges);
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        // nothing
    }

    /**
     * Write the operation to the buffer
     *
     * @param buffer a WireBuffer
     */
    public static void apply(@NonNull WireBuffer buffer, int type, int boundsId,
            boolean animateChanges) {
        buffer.start(OP_CODE);
        buffer.writeInt(type);
        buffer.writeInt(boundsId);
        buffer.writeBoolean(animateChanges);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer     the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int type = buffer.readInt();
        int boundsId = buffer.readInt();
        boolean animateChanges = buffer.readBoolean();
        operations.add(new LayoutComputeOperation(type, boundsId, animateChanges));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Modifier Operations", OP_CODE, "LayoutCompute")
                .addedVersion(7)
                .experimental(true)
                .description("Compute component position and measure via dynamic expressions")
                .field(INT, "type", "Type of computation (0=MEASURE, 1=POSITION)")
                .field(INT, "boundsId", "The ID of the float list variable to store the bounds")
                .field(BOOLEAN,
                        "animateChanges", "Whether to animate layout changes");
    }

    @Override
    public boolean evaluateInLayout(@NonNull RemoteContext context) {
        updateVariables(context);
        return false;
    }

    public void setParent(@Nullable LayoutComponent parent) {
        mParent = parent;
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(indent, serializedName());
    }

    float[] mBounds = new float[6];

    /**
     * Aoply the modifier to the component measure
     */
    public boolean applyToMeasure(@NonNull PaintContext context, @NonNull ComponentMeasure m,
            @NonNull ComponentMeasure parent) {
        if (DEBUG) {
            System.out.println("apply to measure, type " + mType);
        }
        CollectionsAccess access = context.getContext().getCollectionsAccess();
        if (access == null) {
            return false;
        }
        ArrayAccess array = access.getArray(mBoundsId);
        if (array == null) {
            return false;
        }
        mBounds[0] = m.getX();
        mBounds[1] = m.getY();
        mBounds[2] = m.getW();
        mBounds[3] = m.getH();
        mBounds[4] = parent.getW();
        mBounds[5] = parent.getH();
        if (array instanceof DataDynamicListFloat) {
            DataDynamicListFloat listFloat = (DataDynamicListFloat) array;
            listFloat.updateValues(mBounds);
        }
        for (Operation operation : mList) {
            if (operation instanceof VariableSupport && operation.isDirty()) {
                ((VariableSupport) operation).updateVariables(context.getContext());
            }
            operation.apply(context.getContext());
        }
        float[] bounds = array.getFloats();
        if (bounds == null) {
            return false;
        }
        switch (mType) {
            case TYPE_MEASURE:
                m.setW(bounds[2]);
                m.setH(bounds[3]);
                break;
            case TYPE_POSITION:
                m.setX(bounds[0]);
                m.setY(bounds[1]);
                break;
            default:
                m.setX(bounds[0]);
                m.setY(bounds[1]);
                m.setW(bounds[2]);
                m.setH(bounds[3]);
        }

        if (DEBUG) {
            if (mType == TYPE_MEASURE) {
                System.out.println("Compute Measure " + bounds[2] + " x " + bounds[3]);
            } else {
                System.out.println("Compute Layout " + bounds[0] + " , " + bounds[1]);
            }
        }

        m.setAllowsAnimation(isAnimating());
        boolean positionChanged = bounds[0] != mBounds[0] || bounds[1] != mBounds[1];
        boolean dimensionChanged = bounds[2] != mBounds[2] || bounds[3] != mBounds[3];
        if (mType == TYPE_MEASURE && dimensionChanged) {
            return true;
        }
        if (mType == TYPE_POSITION && positionChanged) {
            return true;
        }
        return dimensionChanged || positionChanged;
    }

    private boolean isAnimating() {
        return mAnimateChanges;
    }

    public int getType() {
        return mType;
    }
}
