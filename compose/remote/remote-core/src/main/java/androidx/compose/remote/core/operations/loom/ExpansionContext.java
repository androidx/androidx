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
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.operations.DebugMessage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the state and logic required for materializing operations during macro expansion.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ExpansionContext {
    private final LoomManager mLoomManager;
    private final CoreDocument mDocument;
    private final RemapContext mRemapContext;
    private final Map<Integer, ArrayList<Operation>> mBlocks;
    private final int mDepth;
    private boolean mSafeMode = false;

    public ExpansionContext(
            @NonNull LoomManager loomManager,
            @NonNull CoreDocument document,
            @NonNull RemapContext remapContext,
            @NonNull Map<Integer, ArrayList<Operation>> blocks) {
        this(loomManager, document, remapContext, blocks, false, 0);
    }

    public ExpansionContext(
            @NonNull LoomManager loomManager,
            @NonNull CoreDocument document,
            @NonNull RemapContext remapContext,
            @NonNull Map<Integer, ArrayList<Operation>> blocks,
            boolean safeMode) {
        this(loomManager, document, remapContext, blocks, safeMode, 0);
    }

    public ExpansionContext(
            @NonNull LoomManager loomManager,
            @NonNull CoreDocument document,
            @NonNull RemapContext remapContext,
            @NonNull Map<Integer, ArrayList<Operation>> blocks,
            boolean safeMode,
            int depth) {
        mLoomManager = loomManager;
        mDocument = document;
        mRemapContext = remapContext;
        mBlocks = blocks;
        mSafeMode = safeMode;
        mDepth = depth;
    }

    public int getDepth() {
        return mDepth;
    }

    public boolean isSafeMode() {
        return mSafeMode;
    }

    @NonNull
    public LoomManager getMacroManager() {
        return mLoomManager;
    }

    public @NonNull CoreDocument getDocument() {
        return mDocument;
    }

    @NonNull
    public RemapContext getRemapContext() {
        return mRemapContext;
    }

    /** Returns the operation block associated with the given parameter index. */
    @Nullable
    public ArrayList<Operation> getBlock(int paramIndex) {
        return mBlocks.get(paramIndex);
    }

    /** Creates a new ExpansionContext for a loop iteration with a forked RemapContext. */
    @NonNull
    public ExpansionContext fork() {
        return new ExpansionContext(
                mLoomManager, mDocument, mRemapContext.fork(), mBlocks, mSafeMode, mDepth);
    }

    /**
     * Top-level entry point for recursive expansion. Returns a list of operations that might still
     * need re-nesting if they contain expanded containers.
     */
    public @NonNull ArrayList<Operation> expandRecursive(
            @NonNull ArrayList<Operation> operations, @NonNull LoomManager loomManager) {
        ArrayList<Operation> result = new ArrayList<>();
        expandRecursive(operations, result, loomManager);
        return result;
    }

    private static final int MAX_EXPANSION_DEPTH = 64;

    public void expandRecursive(
            @NonNull ArrayList<Operation> operations,
            @NonNull ArrayList<Operation> result,
            @NonNull LoomManager loomManager) {
        if (mDepth > MAX_EXPANSION_DEPTH) {
            if (mSafeMode) {
                result.add(new DebugMessage(-1, Float.NaN, 0));
                return;
            }
            throw new RuntimeException("Maximum macro expansion depth exceeded");
        }
        for (Operation op : operations) {
            try {
                op.materialize(this, result, loomManager);
            } catch (Throwable t) {
                if (mSafeMode) {
                    // Create a debug message describing the failure.
                    // We try to capture as much context as possible.
                    // Note: In safe mode, we log but continue expansion.
                    result.add(new DebugMessage(-1, Float.NaN, 0));
                } else {
                    // In standard mode, rethrow to provide feedback about corrupted data.
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException) t;
                    }
                    throw new RuntimeException(t);
                }
            }
        }
    }

    /**
     * Records the operation blocks from a MacroCall. These blocks are used during expansion when a
     * MacroArgument is encountered.
     *
     * @param call the macro call containing the blocks
     */
    public void recordBlocks(@NonNull PatternInflation call) {
        for (Operation callChild : call.getList()) {
            if (callChild instanceof PatternBlock) {
                mBlocks.put(
                        ((PatternBlock) callChild).getParamIndex(),
                        ((PatternBlock) callChild).getList());
            }
        }
    }

    /**
     * Returns the blocks associated with a macro call.
     *
     * @param call the macro call
     * @return the map of blocks
     */
    public @NonNull Map<Integer, ArrayList<Operation>> getBlocksForCall(
            @NonNull PatternInflation call) {
        HashMap<Integer, ArrayList<Operation>> blocks = new HashMap<>();
        for (Operation callChild : call.getList()) {
            if (callChild instanceof PatternBlock) {
                blocks.put(
                        ((PatternBlock) callChild).getParamIndex(),
                        ((PatternBlock) callChild).getList());
            }
        }
        return blocks;
    }

    /**
     * Nest containers from a flat list of operations using standard document logic.
     *
     * @param operations the flat list of operations
     * @return a nested list of operations
     */
    public @NonNull ArrayList<Operation> nestContainers(@NonNull ArrayList<Operation> operations) {
        return androidx.compose.remote.core.CoreDocument.nestContainers(
                operations, true, mDocument);
    }

    /**
     * Inflates a macro body from its binary representation using the provided remapping context.
     *
     * @param bodyBytes the binary representation of the macro body
     * @param remapContext the remapping context to use during inflation
     * @return the list of nested operations
     */
    public @NonNull ArrayList<Operation> inflateBody(
            byte @NonNull [] bodyBytes, @NonNull RemapContext remapContext) {
        androidx.compose.remote.core.RemoteComposeBuffer buffer =
                new androidx.compose.remote.core.RemoteComposeBuffer();
        buffer.setProfileMask(mDocument.getProfileMask());
        java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(bodyBytes);
        androidx.compose.remote.core.RemoteComposeBuffer.read(bis, buffer);

        ArrayList<Operation> flatBody = new ArrayList<>();
        buffer.inflateFromBuffer(flatBody, remapContext);

        return nestContainers(flatBody);
    }
}
