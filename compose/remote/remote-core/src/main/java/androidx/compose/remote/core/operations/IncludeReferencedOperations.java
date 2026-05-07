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
package androidx.compose.remote.core.operations;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.operations.layout.Container;
import androidx.compose.remote.core.operations.layout.modifiers.ModifierOperation;
import androidx.compose.remote.core.operations.loom.ExpansionContext;
import androidx.compose.remote.core.operations.loom.LoomManager;
import androidx.compose.remote.core.operations.loom.RemapContext;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/** An operation that references a ReferencedOperations container by ID and includes its content */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class IncludeReferencedOperations extends Operation implements ModifierOperation {
    private static final int OP_CODE = Operations.INCLUDE_REFERENCED_OPERATIONS;
    private int mId;

    public IncludeReferencedOperations(int id) {
        mId = id;
    }

    public int getReferenceId() {
        return mId;
    }

    @Override
    public void materialize(
            @NonNull ExpansionContext context,
            @NonNull ArrayList<Operation> result,
            @NonNull LoomManager loomManager) {
        Operation op = context.getDocument().getReferencedOperationsObject(mId);
        if (op instanceof ReferencedOperations) {
            ReferencedOperations def = (ReferencedOperations) op;
            if (def.getBody() != null) {
                byte[] bodyBytes = def.getBody();
                RemapContext ctx = context.getRemapContext().fork().withInsideMacro(true);

                ArrayList<Operation> nested = context.inflateBody(bodyBytes, ctx);

                // We want to skip the Header (if any)
                ArrayList<Operation> templateContent = new ArrayList<>();
                for (Operation opBody : nested) {
                    if (!(opBody instanceof androidx.compose.remote.core.operations.Header)) {
                        templateContent.add(opBody);
                    }
                }

                ExpansionContext childContext =
                        new ExpansionContext(
                                context.getMacroManager(),
                                context.getDocument(),
                                ctx,
                                new java.util.HashMap<>());
                childContext.expandRecursive(templateContent, result, loomManager);
                return;
            }
        }
        // If we reach here, resolution failed.
        // In safe mode we might want to emit a DebugMessage.
        super.materialize(context, result, loomManager);
    }

    private void flattenRecursive(
            @NonNull ArrayList<Operation> operations, @NonNull ArrayList<Operation> result) {
        for (Operation op : operations) {
            result.add(op);
            if (op instanceof Container) {
                flattenRecursive(((Container) op).getList(), result);
                result.add(new androidx.compose.remote.core.operations.layout.ContainerEnd());
            }
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mId);
    }

    /** write the operation to the buffer */
    public static void apply(@NonNull WireBuffer buffer, int id) {
        buffer.start(OP_CODE);
        buffer.writeInt(id);
    }

    /** Read the operation from the buffer */

    /**
     * Read the operation from the buffer
     *
     * @param buffer
     * @param operations
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int id = buffer.readId();
        operations.add(new IncludeReferencedOperations(id));
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        // Expanded during inflation/macro-expansion
    }

    @Override
    public @NonNull String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType("IncludeReferencedOperations").add("id", mId);
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(indent, "INCLUDE_REFERENCED_OPERATIONS[" + mId + "]");
    }
}
