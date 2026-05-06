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
package androidx.compose.remote.core.operations.loom;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/** Placeholder for an operation-block argument in a macro definition */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PatternArgument extends Operation {
    private static final int OP_CODE = Operations.MACRO_ARGUMENT;
    private final int mParamIndex;

    public PatternArgument(int paramIndex) {
        mParamIndex = paramIndex;
    }

    public int getParamIndex() {
        return mParamIndex;
    }

    @Override
    public void materialize(
            @NonNull ExpansionContext context,
            @NonNull ArrayList<Operation> result,
            @NonNull LoomManager loomManager) {
        ArrayList<Operation> block = context.getBlock(mParamIndex);
        if (block != null) {
            context.expandRecursive(block, result, loomManager);
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mParamIndex);
    }

    /**
     * write the operation to the buffer
     *
     * @param buffer
     * @param paramIndex
     */
    public static void apply(@NonNull WireBuffer buffer, int paramIndex) {
        buffer.start(OP_CODE);
        buffer.writeInt(paramIndex);
    }

    /**
     * Read the operation from the buffer
     *
     * @param buffer
     * @param operations
     */

    /**
     * Read the operation from the buffer
     *
     * @param buffer
     * @param operations
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int paramIndex = buffer.readInt();
        operations.add(new PatternArgument(paramIndex));
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        // Handled during expansion
    }

    @Override
    public String toString() {
        return "MacroArgument[" + mParamIndex + "]";
    }

    @Override
    public @NonNull String deepToString(@NonNull String indent) {
        return indent + toString();
    }
}
