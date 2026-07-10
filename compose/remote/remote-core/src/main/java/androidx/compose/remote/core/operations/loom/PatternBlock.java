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
import androidx.compose.remote.core.operations.layout.Container;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/** Contains an operation-block argument for a macro call */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PatternBlock extends Operation implements Container {
    private static final int OP_CODE = Operations.MACRO_BLOCK;
    private final int mParamIndex;
    @NonNull private ArrayList<Operation> mList = new ArrayList<>();

    public PatternBlock(int paramIndex) {
        mParamIndex = paramIndex;
    }

    public int getParamIndex() {
        return mParamIndex;
    }

    @Override
    public @NonNull ArrayList<Operation> getList() {
        return mList;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mParamIndex);
    }

    /**
     * Write the operation to the buffer
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
        operations.add(new PatternBlock(paramIndex));
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        // Handled during expansion
    }

    @Override
    public String toString() {
        return "MacroBlock[" + mParamIndex + "]";
    }

    @Override
    public @NonNull String deepToString(@NonNull String indent) {
        StringBuilder builder = new StringBuilder();
        builder.append(indent).append(toString()).append("\n");
        for (Operation op : mList) {
            builder.append(op.deepToString(indent + "  ")).append("\n");
        }
        return builder.toString();
    }
}
