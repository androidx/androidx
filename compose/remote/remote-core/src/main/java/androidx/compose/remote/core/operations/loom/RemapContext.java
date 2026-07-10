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

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.operations.Utils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the ID mapping and uniqueification logic during macro expansion.
 *
 * <p>The system uses a Tiered ID system to manage variables and components:
 *
 * <ul>
 *   <li><b>Tier 1: System Globals (0-41)</b>. These IDs (e.g., ID_WINDOW_WIDTH) have fixed meanings
 *       across all documents and are never remapped.
 *   <li><b>Tier 2: Macro-Local IDs (0x4000-0x4FFF)</b>. These are used as placeholders within a
 *       macro template (e.g., parameters). They must be uniqueified during expansion to prevent
 *       collisions between different macro instances or nested calls.
 *   <li><b>Regular IDs (others)</b>. These are standard component or variable IDs. When expanding a
 *       macro, these are also uniqueified to ensure that local definitions within a macro don't
 *       clash with the surrounding scope.
 * </ul>
 *
 * <p>RemapContext maintains two modes of operation:
 *
 * <ol>
 *   <li><b>Lookup (resolveId)</b>: Used to find the translated value of an ID that should have
 *       already been defined (e.g., a parameter reference).
 *   <li><b>Allocation (declareId)</b>: Used when reading an ID definition off the wire. If the ID
 *       requires uniqueification (Tier 2 or any ID inside a macro that isn't Tier 1), a new unique
 *       ID is allocated and recorded.
 * </ol>
 */
@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP)
public class RemapContext {
    private Map<Integer, Integer> mIdMap;
    private boolean mIsMapShared;
    private final CoreDocument mDocument;
    private final boolean mIsInsideMacro;

    /**
     * Returns a RemapContext that translates every id to itself. Used by callers that read from the
     * wire outside of macro expansion.
     */
    @NonNull
    public static RemapContext identity() {
        return new IdentityRemapContext();
    }

    /**
     * Returns a new WireBuffer that wraps the specified buffer and handles ID remapping using this
     * context.
     *
     * @param buffer the buffer to wrap
     * @return a remapping WireBuffer
     */
    @NonNull
    public WireBuffer wrap(@NonNull WireBuffer buffer) {
        return new LoomWireBuffer(buffer, this);
    }

    private static final class IdentityRemapContext extends RemapContext {
        IdentityRemapContext() {
            super(new HashMap<>(), null, false);
        }

        @NonNull
        @Override
        public WireBuffer wrap(@NonNull WireBuffer buffer) {
            return buffer;
        }

        @Override
        public int declareId(int id) {
            return id;
        }

        @Override
        public int resolveId(int id) {
            return id;
        }

        @Override
        public float resolveNanId(float v) {
            return v;
        }

        @Override
        public long resolveLongNanId(long v) {
            return v;
        }

        @NonNull
        @Override
        public RemapContext withInsideMacro(boolean insideMacro) {
            return this;
        }
    }

    public RemapContext(@NonNull CoreDocument document) {
        this(new HashMap<>(), document, false);
    }

    private RemapContext(
            @NonNull Map<Integer, Integer> idMap,
            @Nullable CoreDocument document,
            boolean isInsideMacro) {
        mIdMap = idMap;
        mDocument = document;
        mIsInsideMacro = isInsideMacro;
        mIsMapShared = false;
    }

    /** Returns a new RemapContext with the specified insideMacro state. */
    @NonNull
    public RemapContext withInsideMacro(boolean insideMacro) {
        if (mIsInsideMacro == insideMacro) {
            return this;
        }
        mIsMapShared = true;
        RemapContext next = new RemapContext(mIdMap, mDocument, insideMacro);
        next.mIsMapShared = true;
        return next;
    }

    public boolean isInsideMacro() {
        return mIsInsideMacro;
    }

    /**
     * Creates a new RemapContext that inherits the current mappings. Useful for unrolling loops
     * where each iteration needs a fresh local mapping but should still respect the parent
     * mappings.
     */
    @NonNull
    public RemapContext fork() {
        mIsMapShared = true;
        RemapContext forked = new RemapContext(mIdMap, mDocument, mIsInsideMacro);
        forked.mIsMapShared = true;
        return forked;
    }

    private void ensureMapWritable() {
        if (mIsMapShared) {
            mIdMap = new HashMap<>(mIdMap);
            mIsMapShared = false;
        }
    }

    /** Adds a mapping between an original ID and a new ID. */
    public void addMapping(int originalId, int newId) {
        ensureMapWritable();
        mIdMap.put(originalId, newId);
    }

    /**
     * Resolves a single ID using the current mappings. Returns the original ID if no mapping
     * exists.
     */
    public int resolveId(int id) {
        Integer remapped = mIdMap.get(id);
        return remapped != null ? remapped : id;
    }

    /** Resolve an ID that is NaN-encoded inside a float. Non-NaN values pass through unchanged. */
    public float resolveNanId(float v) {
        if (!Float.isNaN(v)) {
            return v;
        }
        int id = Utils.idFromNan(v);
        int mapped = resolveId(id);
        return mapped == id ? v : Utils.asNan(mapped);
    }

    /** Resolve an ID that is NaN-encoded inside a long (see Utils.longIdFromNaN). */
    public long resolveLongNanId(long v) {
        // Long NaN encoding mirrors Utils.longIdFromNan; only translate if the value decodes to a
        // known id in the map.
        long decoded = Utils.idFromLong(v);
        int id = (int) decoded;
        if (id != decoded) {
            return v; // not an encoded id;
        }
        int mapped = resolveId(id);
        return mapped == id ? v : ((long) mapped) + 0x100000000L;
    }

    /**
     * Declare an ID as it is read off the wire during macro expansion.
     *
     * <p>Remapping logic:
     *
     * <ul>
     *   <li>If the ID already has an explicit mapping, returns the mapped value.
     *   <li>If the ID is macro-local (Tier 2), allocates a fresh unique ID.
     *   <li>If we are inside a macro expansion and the ID is not system-global (Tier 1), allocates
     *       a fresh unique ID.
     *   <li>Otherwise returns the original ID unchanged.
     * </ul>
     */
    public int declareId(int originalId) {
        if (originalId == -1) {
            return -1;
        }
        Integer mapped = mIdMap.get(originalId);
        if (mapped != null) {
            return mapped;
        }

        if (Utils.isMacroLocal(originalId)) {
            return handleMacroLocalId(originalId);
        }

        if (mIsInsideMacro && !Utils.isSystemGlobal(originalId)) {
            return handleRegularIdInsideMacro(originalId);
        }

        return originalId;
    }

    private int handleMacroLocalId(int originalId) {
        return allocateNewId(originalId);
    }

    private int handleRegularIdInsideMacro(int originalId) {
        return allocateNewId(originalId);
    }

    private int allocateNewId(int originalId) {
        if (mDocument == null) {
            throw new IllegalStateException("Cannot allocate ID without a document");
        }
        int newId = mDocument.getNextId();
        addMapping(originalId, newId);
        return newId;
    }

    /** Returns the internal ID mapping. */
    @NonNull
    public Map<Integer, Integer> getIdMap() {
        return mIdMap;
    }
}
