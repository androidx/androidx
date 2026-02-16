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

/** Operation to manipulate text */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TextTransform extends Operation implements VariableSupport, Serializable {
    private static final int OP_CODE = Operations.TEXT_TRANSFORM;
    private static final String CLASS_NAME = "TextTransform";
    private int mTextId;
    private int mSrcId1;
    private float mStart;
    private float mLen;
    private float mOutStart;
    private float mOutLen;
    private final int mOperation;

    /** converts to all lower case */
    public static final int TEXT_TO_LOWERCASE = 1;
    /** converts to all upper case */
    public static final int TEXT_TO_UPPERCASE = 2;
    /** trim white spaces from the ends */
    public static final int TEXT_TRIM = 3;
    /** converts to first letter of each word to upper case */
    public static final int TEXT_CAPITALIZE = 4;
    /** Makes the first character uppercase */
    public static final int TEXT_UPPERCASE_FIRST_CHAR = 5;


    public TextTransform(int textId, int srcId1, float start, float len, int op) {
        mTextId = textId;
        mSrcId1 = srcId1;
        mOutStart = mStart = start;
        mOutLen = mLen = len;
        mOperation = op;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mTextId, mSrcId1, mStart, mLen, mOperation);
    }

    @NonNull
    @Override
    public String toString() {
        return "TextSubrange[" + mTextId + "] = ["
                + mSrcId1 + " ] +  " + mStart + " - " + mLen + " " + mOperation;
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
     * @param start  of the subrange
     * @param len    of the subrange exclusive -1 for end of string
     */
    public static void apply(
            @NonNull WireBuffer buffer, int textId, int srcId1, float start, float len,
            int operation) {
        buffer.start(OP_CODE);
        buffer.writeInt(textId);
        buffer.writeInt(srcId1);
        buffer.writeFloat(start);
        buffer.writeFloat(len);
        buffer.writeInt(operation);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer     the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int textId = buffer.readInt();
        int srcId1 = buffer.readInt();
        float start = buffer.readFloat();
        float len = buffer.readFloat();
        int operation = buffer.readInt();

        operations.add(new TextTransform(textId, srcId1, start, len, operation));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Text Operations", OP_CODE, CLASS_NAME)
                .addedVersion(7)
                .experimental(true)
                .description("Transform a string (case conversion, trimming, etc.)")
                .field(DocumentedOperation.INT, "textId",
                        "The ID of the resulting transformed text")
                .field(INT, "srcId1", "The ID of the source string")
                .field(FLOAT, "start", "The start index of the transformation range")
                .field(FLOAT, "len", "The length of the transformation range")
                .field(INT, "operation", "The type of transformation to apply")
                .possibleValues("TEXT_TO_LOWERCASE", TEXT_TO_LOWERCASE)
                .possibleValues("TEXT_TO_UPPERCASE", TEXT_TO_UPPERCASE)
                .possibleValues("TEXT_TRIM", TEXT_TRIM)
                .possibleValues("TEXT_CAPITALIZE", TEXT_CAPITALIZE)
                .possibleValues("TEXT_UPPERCASE_FIRST_CHAR", TEXT_UPPERCASE_FIRST_CHAR);
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
        switch (mOperation) {
            case TEXT_TO_LOWERCASE:
                strOut = strOut.toLowerCase();
                break;
            case TEXT_TO_UPPERCASE:
                strOut = strOut.toUpperCase();
                break;
            case TEXT_TRIM:
                strOut = strOut.trim();
                break;
            case TEXT_CAPITALIZE:
                strOut = capitalizeWords(strOut);
                break;
            case TEXT_UPPERCASE_FIRST_CHAR:
                strOut = capitalizeFirstWord(strOut);
                break;
        }
        context.loadText(mTextId, strOut);
    }

    private static @NonNull String capitalizeWords(@NonNull String input) {
        if (input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        boolean nextIsUpper = true;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c)) {
                nextIsUpper = true;
                result.append(c);
            } else if (nextIsUpper) {
                result.append(Character.toTitleCase(c));
                nextIsUpper = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }


    private static @NonNull String capitalizeFirstWord(@NonNull String input) {
        if (input.isEmpty()) {
            return input;
        }

        for (int i = 0; i < input.length(); i++) {
            if (!Character.isWhitespace(input.charAt(i))) {
                return input.substring(0, i)
                        + Character.toTitleCase(input.charAt(i))
                        + input.substring(i + 1);
            }
        }

        return input;
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
