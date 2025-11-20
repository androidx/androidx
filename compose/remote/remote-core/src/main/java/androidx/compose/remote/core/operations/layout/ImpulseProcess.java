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
package androidx.compose.remote.core.operations.layout;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/** Represents the repeating part of an Impulse. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ImpulseProcess extends PaintOperation
        implements VariableSupport, Container, Serializable {
    private static final int OP_CODE = Operations.IMPULSE_PROCESS;
    private static final String CLASS_NAME = "ImpulseProcess";

    @NonNull public ArrayList<Operation> mList = new ArrayList<>();

    /** The constructor */
    public ImpulseProcess() {}

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        for (Operation operation : mList) {
            if (operation instanceof VariableSupport) {
                VariableSupport variableSupport = (VariableSupport) operation;
                variableSupport.registerListening(context);
            }
        }
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        for (Operation operation : mList) {
            if (operation instanceof VariableSupport) {
                VariableSupport variableSupport = (VariableSupport) operation;
                variableSupport.updateVariables(context);
            }
        }
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

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer);
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(CLASS_NAME + "\n");
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
        for (Operation op : mList) {
            if (op instanceof VariableSupport && op.isDirty()) {
                ((VariableSupport) op).updateVariables(context.getContext());
            }
            remoteContext.incrementOpCount();
            op.apply(context.getContext());
        }
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return "Loop";
    }

    /**
     * Apply this operation to the buffer
     *
     * @param buffer
     */
    public static void apply(@NonNull WireBuffer buffer) {
        buffer.start(OP_CODE);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        operations.add(new ImpulseProcess());
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Operations", OP_CODE, name())
                .description("Impulse Process that runs a list of operations");
    }

    /**
     * Calculate and estimate of the number of iterations
     *
     * @return number of loops or 10 if based on variables
     */
    public int estimateIterations() {
        return 1;
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("list", mList);
    }
}
