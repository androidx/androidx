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
import androidx.compose.remote.core.RemoteComposeBuffer;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.operations.layout.Container;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/** Defines a pattern template */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PatternDefine extends Operation implements Container {
    private static final int OP_CODE = Operations.MACRO_DEFINE;
    private final int mId;
    private final int[] mParamIds;
    @NonNull private ArrayList<Operation> mList = new ArrayList<>();

    private byte[] mBodyBytes;

    /**
     * Set the body of the macro as a byte array
     *
     * @param bytes the body of the macro
     */
    public void setBody(byte @NonNull [] bytes) {
        mBodyBytes = bytes;
    }

    /**
     * Get the body of the macro as a byte array
     *
     * @return the body of the macro
     */
    public byte @NonNull [] getBody() {
        return mBodyBytes;
    }

    public PatternDefine(int id, int @NonNull [] paramIds) {
        mId = id;
        mParamIds = paramIds;
    }

    public int getId() {
        return mId;
    }

    public int @NonNull [] getParamIds() {
        return mParamIds;
    }

    @Override
    public @NonNull ArrayList<Operation> getList() {
        return mList;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        if (mBodyBytes != null) {
            int offset = apply(buffer, mId, mParamIds);
            buffer.write(mBodyBytes);
            applyEnd(buffer, offset);
        } else {
            apply(buffer, mId, mParamIds);
        }
    }

    /** Write the operation to the buffer */
    public static int apply(@NonNull WireBuffer buffer, int id, int @NonNull [] paramIds) {
        buffer.start(OP_CODE);
        buffer.writeInt(id);
        buffer.writeInt(paramIds.length);
        for (int paramId : paramIds) {
            buffer.writeInt(paramId);
        }
        int offset = buffer.getIndex();
        buffer.writeInt(0); // placeholder for skipLength
        return offset;
    }

    /** Update the skipLength value */
    public static void applyEnd(@NonNull WireBuffer buffer, int offset) {
        int current = buffer.getIndex();
        buffer.overwriteInt(offset, current - (offset + 4));
    }

    /**
     * Read the operation from the buffer
     *
     * @param buffer
     * @param operations
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int id = buffer.readId();
        int paramCount = buffer.readInt();
        int[] paramIds = new int[paramCount];
        for (int i = 0; i < paramCount; i++) {
            paramIds[i] = buffer.readId();
        }
        int skipLength = buffer.readInt();
        PatternDefine macro = new PatternDefine(id, paramIds);
        operations.add(macro);
        if (skipLength > 0) {
            byte[] bodyBytes = new byte[skipLength];
            System.arraycopy(buffer.getBuffer(), buffer.getIndex(), bodyBytes, 0, skipLength);
            macro.setBody(bodyBytes);
            buffer.setIndex(buffer.getIndex() + skipLength);
        }
    }

    @Override
    public void materialize(
            @NonNull ExpansionContext context,
            @NonNull ArrayList<Operation> result,
            @NonNull LoomManager loomManager) {
        String name = context.getDocument().getText(mId);
        if (mBodyBytes == null) {
            mBodyBytes = LoomManager.getMacroBuffer(this).getBuffer().cloneBytes();
        }
        loomManager.add(this, name);
        if (name != null) {
            RemoteComposeBuffer buffer = new RemoteComposeBuffer();
            this.write(buffer.getBuffer());
            buffer.addContainerEnd();
            buffer.getBuffer().setIndex(0);
            context.getDocument().onMacroFound(name, buffer);
        }
        // Definition is not added to the materialized operation list
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        // Definitions are handled during inflation
    }

    @Override
    public String toString() {
        return "MacroDefine[" + mId + "]";
    }

    @Override
    public @NonNull String deepToString(@NonNull String indent) {
        StringBuilder builder = new StringBuilder();
        builder.append(indent).append(toString());
        if (mBodyBytes != null) {
            builder.append(" [body: ").append(mBodyBytes.length).append(" bytes]");
        }
        builder.append("\n");
        for (Operation op : mList) {
            builder.append(op.deepToString(indent + "  ")).append("\n");
        }
        return builder.toString();
    }
}
