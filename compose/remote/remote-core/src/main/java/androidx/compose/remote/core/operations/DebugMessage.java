/*
 * Copyright (C) 2024 The Android Open Source Project
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

import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * This prints debugging message useful for debugging. It should not be use in production documents
 */
public class DebugMessage extends Operation implements VariableSupport {
    private static final int OP_CODE = Operations.DEBUG_MESSAGE;
    private static final String CLASS_NAME = "DebugMessage";
    int mTextID;
    float mFloatValue;
    float mOutFloatValue;
    int mFlags = 0;

    public static final int SHOW_USAGE = 1;

    public DebugMessage(int textId, float value, int flags) {
        mTextID = textId;
        mFloatValue = value;
        mFlags = flags;
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        mOutFloatValue =
                Float.isNaN(mFloatValue)
                        ? context.getFloat(Utils.idFromNan(mFloatValue))
                        : mFloatValue;
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (Float.isNaN(mFloatValue)) {
            context.listensTo(Utils.idFromNan(mFloatValue), this);
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mTextID, mFloatValue, mFlags);
    }

    @NonNull
    @Override
    public String toString() {
        return "DebugMessage "
                + mTextID
                + ", "
                + Utils.floatToString(mFloatValue, mOutFloatValue)
                + ", "
                + mFlags;
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int text = buffer.readInt();
        float floatValue = buffer.readFloat();
        int flags = buffer.readInt();
        DebugMessage op = new DebugMessage(text, floatValue, flags);
        operations.add(op);
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
     * @param buffer write the command to the buffer
     * @param textId id of the text
     * @param value value to print
     * @param flags flags to print
     */
    public static void apply(@NonNull WireBuffer buffer, int textId, float value, int flags) {
        buffer.start(OP_CODE);
        buffer.writeInt(textId);
        buffer.writeFloat(value);
        buffer.writeInt(flags);
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("DebugMessage Operations", id(), CLASS_NAME)
                .description("Print debugging messages")
                .field(DocumentedOperation.INT, "textId", "test to print")
                .field(DocumentedOperation.FLOAT, "value", "value of a float to print")
                .field(DocumentedOperation.INT, "flags", "print additional information");
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        String str = context.getText(mTextID);

        System.out.println("Debug message : " + str + " " + mOutFloatValue);
        if ((mFlags & SHOW_USAGE) > 0) {
            ArrayList<VariableSupport> list = context.getListeners(Utils.idFromNan(mFloatValue));
            for (VariableSupport varSupport : list) {
                System.out.println("Debug message : " + str + " " + varSupport.toString());
            }
        }
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }
}
