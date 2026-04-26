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
import androidx.compose.remote.core.RemoteComposeBuffer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * LoomManager coordinates the registration and expansion of pattern templates within the document.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LoomManager {

    private final HashMap<Integer, PatternDefine> mMacros = new HashMap<>();
    private final HashMap<String, PatternDefine> mMacroNames = new HashMap<>();
    private boolean mSafeMode = false;

    /**
     * Returns a buffer containing the operations of the given macro
     *
     * @param macro the macro definition
     * @return a buffer containing the macro operations
     */
    public static @NonNull RemoteComposeBuffer getMacroBuffer(@NonNull PatternDefine macro) {
        if (macro.getBody() != null) {
            RemoteComposeBuffer buffer = new RemoteComposeBuffer();
            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(macro.getBody());
            RemoteComposeBuffer.read(bis, buffer);
            return buffer;
        }
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        // Skipped adding header for in-document macros
        for (Operation op : macro.getList()) {
            Operation.writeRecursive(op, buffer.getBuffer());
        }
        buffer.getBuffer().setIndex(0);
        return buffer;
    }

    /**
     * Add macro definitions to the document contained in a buffer
     *
     * @param name the name of the macro
     * @param buffer the buffer containing the macro definition(s)
     */
    public void addMacroFromBuffer(@NonNull String name, @NonNull RemoteComposeBuffer buffer) {
        ArrayList<Operation> ops = new ArrayList<>();
        buffer.inflateFromBuffer(ops);
        for (Operation op : ops) {
            if (op instanceof PatternDefine) {
                add((PatternDefine) op, name);
                break;
            }
        }
    }

    /** Sets whether to use safe expansion mode. */
    public void setSafeMode(boolean safeMode) {
        mSafeMode = safeMode;
    }

    /** Adds a macro definition. */
    public void add(@NonNull PatternDefine macro, @Nullable String name) {
        if (!mMacros.containsKey(macro.getId())
                || (mMacros.get(macro.getId()).getBody() == null && macro.getBody() != null)) {
            mMacros.put(macro.getId(), macro);
        }
        if (name != null) {
            if (!mMacroNames.containsKey(name)
                    || (mMacroNames.get(name).getBody() == null && macro.getBody() != null)) {
                mMacroNames.put(name, macro);
            }
        }
    }

    /** Returns a collection of all defined macros by name. */
    @NonNull
    public Map<String, PatternDefine> getNamedMacros() {
        return mMacroNames;
    }

    /** Top-level entry point to expand all macros and references in a list of operations. */
    @NonNull
    public ArrayList<Operation> expandAll(
            @NonNull ArrayList<Operation> operations, @NonNull CoreDocument document) {
        ExpansionContext expansionContext =
                new ExpansionContext(
                        this, document, new RemapContext(document), new HashMap<>(), mSafeMode);
        return expansionContext.expandRecursive(operations, this);
    }

    /** Resolves a MacroDefine for the given call. */
    @Nullable
    public PatternDefine resolve(@NonNull PatternInflation call, @NonNull CoreDocument document) {
        PatternDefine definition = mMacros.get(call.getId());
        if (definition == null) {
            String name = document.getText(call.getId());
            if (name != null) {
                definition = mMacroNames.get(name);
            }
        }
        return definition;
    }
}
