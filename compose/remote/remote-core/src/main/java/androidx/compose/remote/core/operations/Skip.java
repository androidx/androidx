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

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.SerializableToString;
import androidx.compose.remote.core.SystemInfo;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** Operation SKIP over a section of bytes */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Skip extends Operation implements SerializableToString, Serializable {
    private static final int OP_CODE = Operations.SKIP;
    private static final String CLASS_NAME = "Skip";
    public final short mConditionType;
    public int mValue;
    public int mSkipLength;
    private final int mLibraryApiLevel;
    private final int mProfile;

    public static final short SKIP_IF_API_LESS_THAN = 1;
    public static final short SKIP_IF_API_GREATER_THAN = 2;
    public static final short SKIP_IF_API_EQUAL_TO = 3;
    public static final short SKIP_IF_API_NOT_EQUAL_TO = 4;
    public static final short SKIP_IF_PROFILE_INCLUDES = 5;
    public static final short SKIP_IF_PROFILE_EXCLUDES = 6;

    public Skip(short type, int value, int skipLength, @NonNull SystemInfo systemInfo) {
        this.mConditionType = type;
        this.mValue = value;
        this.mSkipLength = skipLength;
        mLibraryApiLevel = systemInfo.getLibraryApiLevel();
        mProfile = systemInfo.getProfile();
    }


    private boolean needsToSkip() {
        switch (mConditionType) {
            case SKIP_IF_API_LESS_THAN:
                return mLibraryApiLevel < mValue;
            case SKIP_IF_API_GREATER_THAN:
                return mLibraryApiLevel > mValue;
            case SKIP_IF_API_EQUAL_TO:
                return mLibraryApiLevel == mValue;
            case SKIP_IF_API_NOT_EQUAL_TO:
                return mLibraryApiLevel != mValue;
            case SKIP_IF_PROFILE_INCLUDES:
                return (mProfile & mValue) != 0;
            case SKIP_IF_PROFILE_EXCLUDES:
                return (mProfile & mValue) == 0;
            default:
                return false;
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mConditionType, mValue, mSkipLength);
    }

    @NonNull
    @Override
    public String toString() {
        return "Skip(" + mConditionType + ", " + mValue + " )  " + mSkipLength;
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
     * add a skip  operation
     *
     * @param buffer        buffer to add to
     * @param conditionType the type of condition
     * @param value         the value to compare against
     */
    public static int apply(@NonNull WireBuffer buffer, short conditionType, int value,
            int skipLength) {
        buffer.start(OP_CODE);
        buffer.writeInt(conditionType); // int
        buffer.writeInt(value);   // int
        int offset = buffer.getIndex();
        buffer.writeInt(skipLength); // int
        System.out.println();
        Utils.logStack(">> offset = " + offset + " pos", 8);

        return offset;
    }

    /**
     * add a text data operation
     *
     * @param buffer buffer to add to
     * @param offset the type of condition
     */
    public static void applyEndSkip(@NonNull WireBuffer buffer, int offset) {
        int current = buffer.getIndex();
        System.out.println(" current = " + current + " bytes");
        System.out.println(" write at " + offset + " skip " + (current - offset - 4));
        buffer.overwriteInt(offset, current - offset - 4);
    }


    /**
     * Read this operation and DOES NOT ADD it to the list of operations
     *
     * @param buffer     the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        short conditionType = (short) buffer.readInt();
        int value = buffer.readInt();
        int skipLength = buffer.readInt();
        Skip skip = new Skip(conditionType, value, skipLength, buffer.getSystemInfo());
        if (skip.needsToSkip()) {
            buffer.setIndex(buffer.getIndex() + skip.mSkipLength);
        }
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Miscellaneous Operations", OP_CODE, CLASS_NAME)
                .description("Skin a section of bytes if condition is true")
                .field(DocumentedOperation.SHORT, "Condition", "The type of condition")
                .field(DocumentedOperation.INT, "Value", "The value to compare against")
                .field(DocumentedOperation.INT, "Length", "the number of bytes to skip");

    }

    @Override
    public void apply(@NonNull RemoteContext context) {
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(indent,
                getSerializedName() + ", " + mConditionType + ", " + mValue + ", " + mSkipLength
                        + "\"");

    }

    @NonNull
    private String getSerializedName() {
        return CLASS_NAME;
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("conditionType", mConditionType).add("value",
                mValue).add("skipLength", mSkipLength);


    }
}
