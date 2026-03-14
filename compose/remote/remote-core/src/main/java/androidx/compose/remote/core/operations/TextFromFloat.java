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

import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.SHORT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.operations.utilities.StringUtils;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Operation convert floats to text This command is structured
 * [command][textId][before,after][flags] before and after define number of digits before and after
 * the decimal point
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TextFromFloat extends Operation implements VariableSupport, Serializable,
        ComponentData {
    private static final int OP_CODE = Operations.TEXT_FROM_FLOAT;
    private static final String CLASS_NAME = "TextFromFloat";
    public int mTextId;
    public float mValue;
    public float mOutValue;
    public short mDigitsBefore;
    public short mDigitsAfter;
    public int mFlags;
    public boolean mLegacy = false;
    public static final int MAX_STRING_SIZE = 4000;
    char mPre = ' ';
    char mAfter = ' ';
    byte mGroup = GROUPING_NONE;
    byte mSeparator = SEPARATOR_PERIOD_COMMA;
    int mOptions;
    boolean mFullFormat = false;

    // Theses flags define what how to/if  fill the space
    public static final int PAD_AFTER_SPACE = 0; // pad past point with space
    public static final int PAD_AFTER_NONE = 1; // do not pad past last digit
    public static final int PAD_AFTER_ZERO = 3; // pad with 0 past last digit
    public static final int PAD_PRE_SPACE = 0; // pad before number with spaces
    public static final int PAD_PRE_NONE = 4; // do not pad before number
    public static final int PAD_PRE_ZERO = 12; // pad before number with 0s
    public static final int GROUPING_NONE = 0; // e.g. 1234567890.12
    public static final int GROUPING_BY3 = 1 << 4;   // e.g. 1,234,567,890.12
    public static final int GROUPING_BY4 = 2 << 4;  // e.g. 12,3456,7890.12
    public static final int GROUPING_BY32 = 3 << 4; // e.g. 1,23,45,67,890.12
    public static final int SEPARATOR_COMMA_PERIOD = 0; // e.g. 123,456.12
    public static final int SEPARATOR_PERIOD_COMMA = 1 << 6; // e.g. 123.456,12
    public static final int SEPARATOR_SPACE_COMMA = 2 << 6;  // e.g. 123 456,12
    public static final int SEPARATOR_UNDER_PERIOD = 3 << 6; // e.g. 123_456.12
    public static final int OPTIONS_NONE = 0;         // e.g. -890.12
    public static final int OPTIONS_NEGATIVE_PARENTHESES = 1 << 8; // e.g. (890.12)
    public static final int OPTIONS_ROUNDING = 2 << 8; // Default is simple clipping
    public static final int LEGACY_MODE = 1 << 10; // Default is simple clipping
    public static final int FULL_FORMAT = 1 << 12; // ignore all of the above full fidelity

    // the flags are critical
    // A = pad after
    // P = pad before
    // G = grouping
    // S = separator
    // O = options
    // L = legacy mode
    // F = full format
    // bit pattern for flags . F L O O _ S S G G _ P P A A


    public TextFromFloat(
            int textId, float value, short digitsBefore, short digitsAfter, int flags) {
        this.mTextId = textId;
        this.mValue = value;
        this.mDigitsAfter = digitsAfter;
        this.mDigitsBefore = digitsBefore;
        this.mFlags = flags;
        mOutValue = mValue;
        switch (mFlags & 3) { // post bits 0000_0011
            case PAD_AFTER_SPACE:
                mAfter = ' ';
                break;
            case PAD_AFTER_NONE:
                mAfter = 0;
                break;
            case PAD_AFTER_ZERO:
                mAfter = '0';
                break;
        }
        switch (mFlags & (3 << 2)) { // pre pad bits 0000_1100
            case PAD_PRE_SPACE:
                mPre = ' ';
                break;
            case PAD_PRE_NONE:
                mPre = 0;
                break;
            case PAD_PRE_ZERO:
                mPre = '0';
                break;
        }

        switch (mFlags & (3 << 4)) { // pre pad bits 0000_1100
            case GROUPING_BY3:
                mGroup = GROUPING_BY3 >> 4;
                break;
            case GROUPING_BY4:
                mGroup = GROUPING_BY4 >> 4;
                break;
            case GROUPING_BY32:
                mGroup = GROUPING_BY32 >> 4;
                break;
        }
        switch (mFlags & (3 << 6)) { // pre pad bits 000
            case SEPARATOR_PERIOD_COMMA:
                mSeparator = SEPARATOR_PERIOD_COMMA >> 6;
                break;
            case SEPARATOR_COMMA_PERIOD:
                mSeparator = SEPARATOR_COMMA_PERIOD >> 6;
                break;
            case SEPARATOR_SPACE_COMMA:
                mSeparator = SEPARATOR_SPACE_COMMA >> 6;
                break;
            case SEPARATOR_UNDER_PERIOD:
                mSeparator = SEPARATOR_UNDER_PERIOD >> 6;
                break;
        }
        if ((mFlags & OPTIONS_ROUNDING) != 0) {
            mOptions |= OPTIONS_ROUNDING >> 8;
        }
        if ((mFlags & OPTIONS_NEGATIVE_PARENTHESES) != 0) {
            mOptions |= OPTIONS_NEGATIVE_PARENTHESES >> 8;
        }
        mLegacy = (mFlags & LEGACY_MODE) != 0;
        if ((mFlags & FULL_FORMAT) != 0) {
            mFullFormat = true;
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mTextId, mValue, mDigitsBefore, mDigitsAfter, mFlags);
    }

    @NonNull
    @Override
    public String toString() {
        return "TextFromFloat["
                + mTextId
                + "] = "
                + Utils.floatToString(mValue)
                + " "
                + mDigitsBefore
                + "."
                + mDigitsAfter
                + " "
                + mFlags;
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        if (Float.isNaN(mValue)) {
            mOutValue = context.getFloat(Utils.idFromNan(mValue));
        }
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (Float.isNaN(mValue)) {
            context.listensTo(Utils.idFromNan(mValue), this);
        }
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
     * @param buffer       buffer to write to
     * @param textId       the id of the output text
     * @param value        the float value to be turned into strings
     * @param digitsBefore the digits before the decimal point
     * @param digitsAfter  the digits after the decimal point
     * @param flags        flags that control if and how to fill the empty spots
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int textId,
            float value,
            short digitsBefore,
            short digitsAfter,
            int flags) {
        buffer.start(OP_CODE);
        buffer.writeInt(textId);
        buffer.writeFloat(value);
        buffer.writeInt((digitsBefore << 16) | digitsAfter);
        buffer.writeInt(flags);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer     the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int textId = buffer.readInt();
        float value = buffer.readFloat();
        int tmp = buffer.readInt();
        short post = (short) (tmp & 0xFFFF);
        short pre = (short) ((tmp >> 16) & 0xFFFF);

        int flags = buffer.readInt();
        operations.add(new TextFromFloat(textId, value, pre, post, flags));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Text Operations", OP_CODE, CLASS_NAME)
                .description("Convert a float value into a formatted string")
                .field(DocumentedOperation.INT, "textId", "The ID of the resulting text")
                .field(DocumentedOperation.FLOAT, "value", "The float value to convert")
                .field(SHORT, "digitsBefore", "Number of digits before the decimal point")
                .field(SHORT, "digitsAfter", "Number of digits after the decimal point")
                .field(INT, "flags", "Formatting and padding flags");
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        float v = mOutValue;
        String s;
        if (mFullFormat) {
            s = Float.toString(v);
            context.loadText(mTextId, s);
            return;
        }
        if (mLegacy) {
            s = StringUtils.floatToString(v, mDigitsBefore, mDigitsAfter, mPre, mAfter);
        } else {
            s = StringUtils.floatToString(
                    v, mDigitsBefore, mDigitsAfter, mPre, mAfter, mSeparator, mGroup, mOptions);
        }
        context.loadText(mTextId, s);
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("textId", mTextId)
                .add("value", mValue, mOutValue)
                .add("digitsBefore", mDigitsBefore)
                .add("digitsAfter", mDigitsAfter)
                .add("flags", mFlags);
    }
}
