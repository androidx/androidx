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
package androidx.compose.remote.core.operations;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.operations.layout.Container;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/** Represents conditional execution of a block of commands */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ConditionalOperations extends PaintOperation
        implements Container, VariableSupport, Serializable {
    private static final String CLASS_NAME = "ConditionalOperations";

    private static final int OP_CODE = Operations.CONDITIONAL_OPERATIONS;

    @NonNull public ArrayList<Operation> mList = new ArrayList<>();

    int mIndexVariableId;
    byte mType;
    float mVarA;
    float mVarB;
    float mVarAOut;
    float mVarBOut;

    /** Equality comparison */
    public static final byte TYPE_EQ = 0;

    /** Not equal comparison */
    public static final byte TYPE_NEQ = 1;

    /** Less than comparison */
    public static final byte TYPE_LT = 2;

    /** Less than or equal comparison */
    public static final byte TYPE_LTE = 3;

    /** Greater than comparison */
    public static final byte TYPE_GT = 4;

    /** Greater than or equal comparison */
    public static final byte TYPE_GTE = 5;

    private static final String[] TYPE_STR = {"EQ", "NEQ", "LT", "LTE", "GT", "GTE"};

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (Float.isNaN(mVarA)) {
            context.listensTo(Utils.idFromNan(mVarA), this);
        }
        if (Float.isNaN(mVarB)) {
            context.listensTo(Utils.idFromNan(mVarB), this);
        }
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        mVarAOut = Float.isNaN(mVarA) ? context.getFloat(Utils.idFromNan(mVarA)) : mVarA;
        mVarBOut = Float.isNaN(mVarB) ? context.getFloat(Utils.idFromNan(mVarB)) : mVarB;
        for (Operation op : mList) {
            if (op instanceof VariableSupport && op.isDirty()) {
                ((VariableSupport) op).updateVariables(context);
            }
        }
    }

    /**
     * Constructor
     *
     * @param type type of comparison
     * @param a first value
     * @param b second value
     */
    public ConditionalOperations(byte type, float a, float b) {
        mType = type;
        mVarAOut = mVarA = a;
        mVarBOut = mVarB = b;
    }

    @Override
    @NonNull
    public ArrayList<Operation> getList() {
        return mList;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mType, mVarA, mVarB);
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder =
                new StringBuilder(
                        CLASS_NAME
                                + " "
                                + TYPE_STR[mType]
                                + "("
                                + Utils.idFromNan(mVarA)
                                + ","
                                + Utils.idFromNan(mVarB)
                                + ")\n");
        for (Operation operation : mList) {
            builder.append("  ");
            builder.append(operation);
            builder.append("\n");
        }
        return builder.toString();
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return (indent != null ? indent : "") + toString();
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        RemoteContext remoteContext = context.getContext();
        RemoteContext ctx = context.getContext();
        for (Operation op : mList) {
            if (op instanceof VariableSupport && op.isDirty()) {
                op.markNotDirty();
                ((VariableSupport) op).updateVariables(ctx);
            }
        }
        boolean run = false;
        switch (mType) {
            case TYPE_EQ:
                run = mVarAOut == mVarBOut;
                break;
            case TYPE_NEQ:
                run = mVarAOut != mVarBOut;
                break;
            case TYPE_LT:
                run = mVarAOut < mVarBOut;
                break;
            case TYPE_LTE:
                run = mVarAOut <= mVarBOut;
                break;
            case TYPE_GT:
                run = mVarAOut > mVarBOut;
                break;
            case TYPE_GTE:
                run = mVarAOut >= mVarBOut;
                break;
        }
        if (run) {
            for (Operation op : mList) {
                remoteContext.incrementOpCount();
                op.apply(context.getContext());
            }
        }
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return CLASS_NAME;
    }

    /**
     * Write the operation on the buffer
     *
     * @param type type of operation
     * @param a first value
     * @param b second value
     * @param buffer the buffer to write to
     */
    public static void apply(@NonNull WireBuffer buffer, byte type, float a, float b) {
        buffer.start(OP_CODE);
        buffer.writeByte(type);
        buffer.writeFloat(a);
        buffer.writeFloat(b);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        byte type = (byte) buffer.readByte();
        float a = buffer.readFloat();
        float b = buffer.readFloat();
        operations.add(new ConditionalOperations(type, a, b));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Operations", OP_CODE, name())
                .description("Run if the condition is true")
                .field(DocumentedOperation.BYTE, "type", "type of comparison")
                .field(DocumentedOperation.FLOAT, "a", "first value")
                .field(DocumentedOperation.FLOAT, "b", "second value");
    }

    /**
     * Calculate and estimate of the number of iterations
     *
     * @return number of loops or 10 if based on variables
     */
    public int estimateIterations() {
        return 1; // this is a generic estmate if the values are variables;
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("type", mType)
                .add("varA", mVarA, mVarAOut)
                .add("VarB", mVarB, mVarBOut)
                .add("list", mList);
    }
}
