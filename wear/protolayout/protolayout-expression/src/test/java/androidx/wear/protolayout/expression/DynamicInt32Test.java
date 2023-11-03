/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.protolayout.expression;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32;
import androidx.wear.protolayout.expression.proto.DynamicProto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class DynamicInt32Test {
    private static final String STATE_KEY = "state-key";
    private static final int CONSTANT_VALUE = 42;

    @Test
    public void constantInt32() {
        DynamicInt32 constantInt32 = DynamicInt32.constant(CONSTANT_VALUE);

        assertThat(constantInt32.toDynamicInt32Proto().getFixed().getValue())
                .isEqualTo(CONSTANT_VALUE);
    }

    @Test
    public void constantToString() {
        assertThat(DynamicInt32.constant(1).toString()).isEqualTo("FixedInt32{value=1}");
    }

    @Test
    public void stateEntryValueInt32() {
        DynamicInt32 stateInt32 = DynamicInt32.from(new AppDataKey<>(STATE_KEY));

        assertThat(stateInt32.toDynamicInt32Proto().getStateSource().getSourceKey())
                .isEqualTo(STATE_KEY);
    }

    @Test
    public void stateToString() {
        assertThat(DynamicInt32.from(new AppDataKey<>("key")).toString())
                .isEqualTo("StateInt32Source{sourceKey=key, sourceNamespace=}");
    }

    @Test
    public void constantInt32_asFloat() {
        DynamicInt32 constantInt32 = DynamicInt32.constant(CONSTANT_VALUE);

        DynamicFloat dynamicFloat = constantInt32.asFloat();

        assertThat(
                        dynamicFloat
                                .toDynamicFloatProto()
                                .getInt32ToFloatOperation()
                                .getInput()
                                .getFixed()
                                .getValue())
                .isEqualTo(CONSTANT_VALUE);
    }

    @Test
    public void constantInt32_asFloatToString() {
        assertThat(DynamicInt32.constant(1).asFloat().toString())
                .isEqualTo("Int32ToFloatOp{input=FixedInt32{value=1}}");
    }

    @Test
    public void formatInt32_defaultParameters() {
        DynamicInt32 constantInt32 = DynamicInt32.constant(CONSTANT_VALUE);

        DynamicBuilders.DynamicString defaultFormat = constantInt32.format();

        DynamicProto.Int32FormatOp int32FormatOp =
                defaultFormat.toDynamicStringProto().getInt32FormatOp();
        assertThat(int32FormatOp.getInput()).isEqualTo(constantInt32.toDynamicInt32Proto());
        assertThat(int32FormatOp.getGroupingUsed()).isFalse();
        assertThat(int32FormatOp.hasMinIntegerDigits()).isFalse();
    }

    @Test
    public void formatInt32_customFormatter() {
        DynamicInt32 constantInt32 = DynamicInt32.constant(CONSTANT_VALUE);
        boolean groupingUsed = true;
        int minIntegerDigits = 3;
        DynamicInt32.IntFormatter intFormatter =
                new DynamicInt32.IntFormatter.Builder()
                        .setMinIntegerDigits(minIntegerDigits)
                        .setGroupingUsed(groupingUsed)
                        .build();

        DynamicBuilders.DynamicString customFormat = constantInt32.format(intFormatter);

        DynamicProto.Int32FormatOp int32FormatOp =
                customFormat.toDynamicStringProto().getInt32FormatOp();
        assertThat(int32FormatOp.getInput()).isEqualTo(constantInt32.toDynamicInt32Proto());
        assertThat(int32FormatOp.getGroupingUsed()).isEqualTo(groupingUsed);
        assertThat(int32FormatOp.getMinIntegerDigits()).isEqualTo(minIntegerDigits);
    }

    @Test
    public void formatToString() {
        assertThat(
                        DynamicInt32.constant(1)
                                .format(
                                        new DynamicInt32.IntFormatter.Builder()
                                                .setMinIntegerDigits(2)
                                                .setGroupingUsed(true)
                                                .build())
                                .toString())
                .isEqualTo(
                        "Int32FormatOp{input=FixedInt32{value=1}, minIntegerDigits=2,"
                                + " groupingUsed=true}");
    }

    @Test
    public void fromByteArray_validProto() {
        DynamicInt32 from = DynamicInt32.constant(CONSTANT_VALUE);
        DynamicInt32 to = DynamicInt32.fromByteArray(from.toDynamicInt32ByteArray());

        assertThat(to.toDynamicInt32Proto().getFixed().getValue()).isEqualTo(CONSTANT_VALUE);
    }

    @Test
    public void fromByteArray_invalidProto() {
        assertThrows(
                IllegalArgumentException.class, () -> DynamicInt32.fromByteArray(new byte[] {1}));
    }

    @Test
    public void fromByteArray_existingByteArray() {
        DynamicInt32 from = DynamicInt32.constant(CONSTANT_VALUE);
        byte[] buffer = new byte[100];
        int written = from.toDynamicInt32ByteArray(buffer, 10, 50);

        DynamicInt32 to = DynamicInt32.fromByteArray(buffer, 10, written);

        assertThat(to.toDynamicInt32Proto().getFixed().getValue()).isEqualTo(CONSTANT_VALUE);
    }

    @Test
    public void fromByteArray_existingByteArrayTooSmall() {
        DynamicInt32 from = DynamicInt32.constant(CONSTANT_VALUE);
        byte[] buffer = new byte[100];
        int written = from.toDynamicInt32ByteArray(buffer);

        assertThrows(
                IllegalArgumentException.class,
                () -> DynamicInt32.fromByteArray(buffer, 0, written - 1));
    }

    @Test
    public void fromByteArray_existingByteArrayTooLarge() {
        DynamicInt32 from = DynamicInt32.constant(CONSTANT_VALUE);
        byte[] buffer = new byte[100];
        int written = from.toDynamicInt32ByteArray(buffer);

        assertThrows(
                IllegalArgumentException.class,
                () -> DynamicInt32.fromByteArray(buffer, 0, written + 1));
    }

    @Test
    public void toByteArray_existingByteArrayTooSmall() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DynamicInt32.constant(CONSTANT_VALUE).toDynamicInt32ByteArray(new byte[1]));
    }

    @Test
    public void toByteArray_existingByteArraySameSize() {
        DynamicInt32 from = DynamicInt32.constant(CONSTANT_VALUE);

        assertThat(from.toDynamicInt32ByteArray(new byte[100]))
                .isEqualTo(from.toDynamicInt32ByteArray().length);
    }
}
