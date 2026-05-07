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
import androidx.compose.remote.core.operations.utilities.ArrayAccess;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/** Iterates over a collection parameter and expands its children for each item */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PatternForEach extends Operation implements Container {
    private static final int OP_CODE = Operations.MACRO_FOR_EACH;
    private int mCollectionId;
    private int mLocalItemId;
    @NonNull private ArrayList<Operation> mList = new ArrayList<>();
    private byte[] mBodyBytes;

    public PatternForEach(int collectionId, int localItemId) {
        mCollectionId = collectionId;
        mLocalItemId = localItemId;
    }

    public int getCollectionId() {
        return mCollectionId;
    }

    public int getLocalItemId() {
        return mLocalItemId;
    }

    @Override
    public @NonNull ArrayList<Operation> getList() {
        return mList;
    }

    @Override
    public void materialize(
            @NonNull ExpansionContext context,
            @NonNull ArrayList<Operation> result,
            @NonNull LoomManager loomManager) {
        ArrayAccess array = context.getDocument().getArray(mCollectionId);
        if (array == null) {
            return;
        }

        if (mBodyBytes == null) {
            androidx.compose.remote.core.RemoteComposeBuffer buffer =
                    new androidx.compose.remote.core.RemoteComposeBuffer();
            buffer.addHeader(
                    new short[] {androidx.compose.remote.core.operations.Header.DOC_PROFILES},
                    new Object[] {
                        androidx.compose.remote.core.RcProfiles.PROFILE_EXPERIMENTAL
                                | androidx.compose.remote.core.RcProfiles.PROFILE_ANDROIDX
                    });
            for (Operation op : mList) {
                Operation.writeRecursive(op, buffer.getBuffer());
            }
            mBodyBytes = buffer.getBuffer().cloneBytes();
        }

        for (int i = 0; i < array.getLength(); i++) {
            RemapContext ctx = context.getRemapContext().fork().withInsideMacro(true);
            ctx.addMapping(mLocalItemId, array.getId(i));

            ArrayList<Operation> nested = context.inflateBody(mBodyBytes, ctx);

            // Remove the generated Header before expanding the body
            ArrayList<Operation> templateContent = new ArrayList<>();
            for (Operation op : nested) {
                if (!(op instanceof androidx.compose.remote.core.operations.Header)) {
                    templateContent.add(op);
                }
            }

            ExpansionContext childContext =
                    new ExpansionContext(
                            context.getMacroManager(),
                            context.getDocument(),
                            ctx,
                            new java.util.HashMap<>());
            childContext.expandRecursive(templateContent, result, loomManager);
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mCollectionId, mLocalItemId);
    }

    /**
     * Write the operation to the buffer
     *
     * @param buffer
     * @param collectionId
     * @param localItemId
     */
    public static void apply(@NonNull WireBuffer buffer, int collectionId, int localItemId) {
        buffer.start(OP_CODE);
        buffer.writeInt(collectionId);
        buffer.writeInt(localItemId);
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
        int collectionId = buffer.readId();
        int localItemId = buffer.readId();
        operations.add(new PatternForEach(collectionId, localItemId));
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        // Handled during expansion
    }

    @Override
    public String toString() {
        return "MacroForEach[coll=" + mCollectionId + ", item=" + mLocalItemId + "]";
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
