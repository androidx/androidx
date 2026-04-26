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
import androidx.compose.remote.core.operations.ComponentData;
import androidx.compose.remote.core.operations.layout.Container;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/** Calls a pattern with specific arguments */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PatternInflation extends Operation implements Container, ComponentData {
    private static final int OP_CODE = Operations.MACRO_CALL;
    private int mId;
    private final int[] mArgIds;
    @NonNull private ArrayList<Operation> mList = new ArrayList<>();

    public PatternInflation(int id, int @NonNull [] argIds) {
        mId = id;
        mArgIds = argIds;
    }

    public int getId() {
        return mId;
    }

    public int @NonNull [] getArgIds() {
        return mArgIds;
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
        PatternDefine def = loomManager.resolve(this, context.getDocument());
        if (def == null) {
            // Macro not found: emit this call unchanged (or log & skip)
            result.add(this);
            // In a real safe-expansion mode we'd emit a DebugMessage with the missing macro name.
            return;
        }

        byte[] bodyBytes = def.getBody();
        if (bodyBytes == null) {
            // Force capture if it wasn't already (e.g. if materialize pass hasn't hit def yet)
            bodyBytes = LoomManager.getMacroBuffer(def).getBuffer().cloneBytes();
            def.setBody(bodyBytes);
        }

        RemapContext ctx = context.getRemapContext().fork().withInsideMacro(true);
        // Seed param -> arg mapping.
        // The args were already translated when this MacroCall was read.
        // We want the template's paramId to map to the already-translated argId.
        int[] params = def.getParamIds();
        int[] args = mArgIds;
        for (int i = 0; i < Math.min(params.length, args.length); i++) {
            ctx.addMapping(params[i], args[i]);
        }
        // Record operation blocks so MacroArgument.materialize can find them.
        context.recordBlocks(this);

        // Re-inflate the macro body with this ctx.
        ArrayList<Operation> templateOps = context.inflateBody(bodyBytes, ctx);

        if (templateOps == null || templateOps.isEmpty()) {
            return;
        }

        //        ArrayList<Operation> actualOps = templateOps;
        //        if (templateOps.size() == 1 && templateOps.get(0) instanceof MacroDefine) {
        //            actualOps = ((MacroDefine) templateOps.get(0)).getList();
        //        }

        // Run materialize recursively so nested MacroCalls/ForEach/Arguments expand too.
        ExpansionContext child =
                new ExpansionContext(
                        loomManager, context.getDocument(), ctx, context.getBlocksForCall(this));
        child.expandRecursive(templateOps, result, loomManager);
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mId, mArgIds);
    }

    /**
     * Write the operation to the buffer
     *
     * @param buffer
     * @param id
     * @param argIds
     */
    public static void apply(@NonNull WireBuffer buffer, int id, int @NonNull [] argIds) {
        buffer.start(OP_CODE);
        buffer.writeInt(id);
        buffer.writeInt(argCount(argIds));
        for (int argId : argIds) {
            buffer.writeInt(argId);
        }
    }

    private static int argCount(int[] argIds) {
        return argIds == null ? 0 : argIds.length;
    }

    /**
     * Read the operation from the buffer
     *
     * @param buffer
     * @param operations
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int id = buffer.readId();
        int argCount = buffer.readInt();
        int[] argIds = new int[argCount];
        for (int i = 0; i < argCount; i++) {
            argIds[i] = buffer.readId();
        }
        operations.add(new PatternInflation(id, argIds));
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        // Macro calls are expanded during inflation
    }

    /**
     * Serialize the macro
     *
     * @param indent
     * @param serializer
     */
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(indent, toString());
        for (Operation op : mList) {
            if (op
                    instanceof
                    androidx.compose.remote.core.operations.layout.modifiers.ModifierOperation) {
                ((androidx.compose.remote.core.operations.layout.modifiers.ModifierOperation) op)
                        .serializeToString(indent + 1, serializer);
            }
        }
    }

    /**
     * Serialize the macro
     *
     * @param serializer
     */
    public void serialize(@NonNull MapSerializer serializer) {
        ArrayList<Integer> args = new ArrayList<>();
        if (mArgIds != null) {
            for (int id : mArgIds) {
                args.add(id);
            }
        }
        serializer.addType("MacroCall").add("id", mId).add("args", args).add("list", mList);
    }

    @Override
    public String toString() {
        return "MacroCall[" + mId + "]";
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
