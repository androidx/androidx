/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static androidx.compose.remote.core.documentation.DocumentedOperation.FLOAT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** Operation to deal with Text data */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TextSubtext extends Operation implements VariableSupport, Serializable {
    private static final int OP_CODE = Operations.TEXT_SUBTEXT; // TEXT_SUBRANGE;
    private static final String CLASS_NAME = "TextSubtext";
    private int mTextId;
    private int mSrcId1;
    private float mStart;
    private float mLen;
    private float mOutStart;
    private float mOutLen;

    public TextSubtext(int textId, int srcId1, float start, float len) {
        mTextId = textId;
        mSrcId1 = srcId1;
        mOutStart = mStart = start;
        mOutLen = mLen = len;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mTextId, mSrcId1, mStart, mLen);
    }

    @NonNull
    @Override
    public String toString() {
        return "TextSubrange[" + mTextId + "] = [" + mSrcId1 + " ] +  " + mStart + " - " + mLen;
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return CLASS_NAME;
    }

    /**
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return OP_CODE;
    }

    /**
     * Writes out the operation to the buffer
     *
     * @param buffer buffer to write to
     * @param textId id of the text
     * @param srcId1 source text 1
     * @param start of the subrange
     * @param len of the subrange exclusive -1 for end of string
     */
    public static void apply(
            @NonNull WireBuffer buffer, int textId, int srcId1, float start, float len) {
        buffer.start(OP_CODE);
        buffer.writeInt(textId);
        buffer.writeInt(srcId1);
        buffer.writeFloat(start);
        buffer.writeFloat(len);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int textId = buffer.readInt();
        int srcId1 = buffer.readInt();
        float start = buffer.readFloat();
        float len = buffer.readFloat();

        operations.add(new TextSubtext(textId, srcId1, start, len));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Data Operations", OP_CODE, CLASS_NAME)
                .description("Merge two string into one")
                .field(DocumentedOperation.INT, "textId", "id of the text")
                .field(INT, "srcTextId1", "id of the path")
                .field(FLOAT, "start", "the start of the subrange")
                .field(FLOAT, "end", "the end of the subrange exclusive -1 for end of string");
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        String str = context.getText(mSrcId1);
        String strOut;
        if (mOutLen == -1) {
            strOut = str.substring((int) mOutStart, str.length());
        } else {
            strOut = str.substring((int) mOutStart, (int) (mOutStart + mOutLen));
        }
        context.loadText(mTextId, strOut);
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {

        if (Float.isNaN(mStart)) {
            mOutStart = context.getFloat(Utils.idFromNan(mStart));
        }
        if (Float.isNaN(mLen)) {
            mOutLen = context.getFloat(Utils.idFromNan(mLen));
        }
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        context.listensTo(mSrcId1, this);
        if (Float.isNaN(mStart)) {
            context.listensTo(Utils.idFromNan(mStart), this);
        }
        if (Float.isNaN(mLen)) {
            context.listensTo(Utils.idFromNan(mLen), this);
        }
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + this;
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("id", mTextId)
                .add("source", mSrcId1)
                .add("start", mStart)
                .add("end", mLen);
    }
}
