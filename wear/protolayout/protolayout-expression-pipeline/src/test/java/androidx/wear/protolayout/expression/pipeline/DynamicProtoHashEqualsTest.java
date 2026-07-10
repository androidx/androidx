/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.protolayout.expression.pipeline;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.proto.DynamicProto;
import androidx.wear.protolayout.expression.proto.FixedProto;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DynamicProtoHashEqualsTest {
    @Test
    public void fixedInt32_equals() {
        DynamicProto.DynamicInt32 fixed1 =
                DynamicProto.DynamicInt32.newBuilder()
                        .setFixed(FixedProto.FixedInt32.newBuilder().setValue(1))
                        .build();
        DynamicProto.DynamicInt32 fixed2 =
                DynamicProto.DynamicInt32.newBuilder()
                        .setFixed(FixedProto.FixedInt32.newBuilder().setValue(1))
                        .build();
        DynamicProto.DynamicInt32 fixed3 =
                DynamicProto.DynamicInt32.newBuilder()
                        .setFixed(FixedProto.FixedInt32.newBuilder().setValue(2))
                        .build();

        assertThat(DynamicProtoHashEquals.equals(fixed1, fixed2)).isTrue();
        assertThat(DynamicProtoHashEquals.hashCode(fixed1))
                .isEqualTo(DynamicProtoHashEquals.hashCode(fixed2));
        assertThat(DynamicProtoHashEquals.equals(fixed1, fixed3)).isFalse();
    }

    @Test
    public void stateInt32_equals() {
        DynamicProto.StateInt32Source stateSource1 =
                DynamicProto.StateInt32Source.newBuilder().setSourceKey("key").build();
        DynamicProto.StateInt32Source stateSource2 =
                DynamicProto.StateInt32Source.newBuilder().setSourceKey("key").build();
        DynamicProto.StateInt32Source stateSource3 =
                DynamicProto.StateInt32Source.newBuilder().setSourceKey("key2").build();
        DynamicProto.DynamicInt32 state1 =
                DynamicProto.DynamicInt32.newBuilder().setStateSource(stateSource1).build();
        DynamicProto.DynamicInt32 state2 =
                DynamicProto.DynamicInt32.newBuilder().setStateSource(stateSource2).build();
        DynamicProto.DynamicInt32 state3 =
                DynamicProto.DynamicInt32.newBuilder().setStateSource(stateSource3).build();

        assertThat(DynamicProtoHashEquals.equals(state1, state2)).isTrue();
        assertThat(DynamicProtoHashEquals.hashCode(state1))
                .isEqualTo(DynamicProtoHashEquals.hashCode(state2));
        assertThat(DynamicProtoHashEquals.equals(state1, state3)).isFalse();
    }

    @Test
    public void arithmeticInt32_equals() {
        DynamicProto.DynamicInt32 fixed1 =
                DynamicProto.DynamicInt32.newBuilder()
                        .setFixed(FixedProto.FixedInt32.newBuilder().setValue(1))
                        .build();
        DynamicProto.DynamicInt32 fixed2 =
                DynamicProto.DynamicInt32.newBuilder()
                        .setFixed(FixedProto.FixedInt32.newBuilder().setValue(2))
                        .build();
        DynamicProto.ArithmeticInt32Op add1 =
                DynamicProto.ArithmeticInt32Op.newBuilder()
                        .setInputLhs(fixed1)
                        .setInputRhs(fixed2)
                        .setOperationType(DynamicProto.ArithmeticOpType.ARITHMETIC_OP_TYPE_ADD)
                        .build();
        DynamicProto.ArithmeticInt32Op add2 =
                DynamicProto.ArithmeticInt32Op.newBuilder()
                        .setInputLhs(fixed1)
                        .setInputRhs(fixed2)
                        .setOperationType(DynamicProto.ArithmeticOpType.ARITHMETIC_OP_TYPE_ADD)
                        .build();
        DynamicProto.ArithmeticInt32Op subtract =
                DynamicProto.ArithmeticInt32Op.newBuilder()
                        .setInputLhs(fixed1)
                        .setInputRhs(fixed2)
                        .setOperationType(DynamicProto.ArithmeticOpType.ARITHMETIC_OP_TYPE_SUBTRACT)
                        .build();
        DynamicProto.ArithmeticInt32Op addReversed =
                DynamicProto.ArithmeticInt32Op.newBuilder()
                        .setInputLhs(fixed2)
                        .setInputRhs(fixed1)
                        .setOperationType(DynamicProto.ArithmeticOpType.ARITHMETIC_OP_TYPE_ADD)
                        .build();

        DynamicProto.DynamicInt32 op1 =
                DynamicProto.DynamicInt32.newBuilder().setArithmeticOperation(add1).build();
        DynamicProto.DynamicInt32 op2 =
                DynamicProto.DynamicInt32.newBuilder().setArithmeticOperation(add2).build();
        DynamicProto.DynamicInt32 op3 =
                DynamicProto.DynamicInt32.newBuilder().setArithmeticOperation(subtract).build();
        DynamicProto.DynamicInt32 op4 =
                DynamicProto.DynamicInt32.newBuilder().setArithmeticOperation(addReversed).build();

        assertThat(DynamicProtoHashEquals.equals(op1, op2)).isTrue();
        assertThat(DynamicProtoHashEquals.hashCode(op1))
                .isEqualTo(DynamicProtoHashEquals.hashCode(op2));
        assertThat(DynamicProtoHashEquals.equals(op1, op3)).isFalse();
        assertThat(DynamicProtoHashEquals.equals(op1, op4)).isFalse();
    }

    @Test
    public void conditionalInt32_equals() {
        DynamicProto.DynamicBool trueBool =
                DynamicProto.DynamicBool.newBuilder()
                        .setFixed(FixedProto.FixedBool.newBuilder().setValue(true))
                        .build();
        DynamicProto.DynamicInt32 fixed1 =
                DynamicProto.DynamicInt32.newBuilder()
                        .setFixed(FixedProto.FixedInt32.newBuilder().setValue(1))
                        .build();
        DynamicProto.DynamicInt32 fixed2 =
                DynamicProto.DynamicInt32.newBuilder()
                        .setFixed(FixedProto.FixedInt32.newBuilder().setValue(2))
                        .build();

        DynamicProto.ConditionalInt32Op conditional1 =
                DynamicProto.ConditionalInt32Op.newBuilder()
                        .setCondition(trueBool)
                        .setValueIfTrue(fixed1)
                        .setValueIfFalse(fixed2)
                        .build();
        DynamicProto.ConditionalInt32Op conditional2 =
                DynamicProto.ConditionalInt32Op.newBuilder()
                        .setCondition(trueBool)
                        .setValueIfTrue(fixed1)
                        .setValueIfFalse(fixed2)
                        .build();
        DynamicProto.ConditionalInt32Op conditional3 =
                DynamicProto.ConditionalInt32Op.newBuilder()
                        .setCondition(trueBool)
                        .setValueIfTrue(fixed2)
                        .setValueIfFalse(fixed1)
                        .build();

        DynamicProto.DynamicInt32 op1 =
                DynamicProto.DynamicInt32.newBuilder().setConditionalOp(conditional1).build();
        DynamicProto.DynamicInt32 op2 =
                DynamicProto.DynamicInt32.newBuilder().setConditionalOp(conditional2).build();
        DynamicProto.DynamicInt32 op3 =
                DynamicProto.DynamicInt32.newBuilder().setConditionalOp(conditional3).build();

        assertThat(DynamicProtoHashEquals.equals(op1, op2)).isTrue();
        assertThat(DynamicProtoHashEquals.hashCode(op1))
                .isEqualTo(DynamicProtoHashEquals.hashCode(op2));
        assertThat(DynamicProtoHashEquals.equals(op1, op3)).isFalse();
    }

    @Test
    public void fixedFloat_equals() {
        DynamicProto.DynamicFloat fixed1 =
                DynamicProto.DynamicFloat.newBuilder()
                        .setFixed(FixedProto.FixedFloat.newBuilder().setValue(1.0f))
                        .build();
        DynamicProto.DynamicFloat fixed2 =
                DynamicProto.DynamicFloat.newBuilder()
                        .setFixed(FixedProto.FixedFloat.newBuilder().setValue(1.0f))
                        .build();
        DynamicProto.DynamicFloat fixed3 =
                DynamicProto.DynamicFloat.newBuilder()
                        .setFixed(FixedProto.FixedFloat.newBuilder().setValue(2.0f))
                        .build();

        assertThat(DynamicProtoHashEquals.equals(fixed1, fixed2)).isTrue();
        assertThat(DynamicProtoHashEquals.hashCode(fixed1))
                .isEqualTo(DynamicProtoHashEquals.hashCode(fixed2));
        assertThat(DynamicProtoHashEquals.equals(fixed1, fixed3)).isFalse();
    }

    @Test
    public void fixedString_equals() {
        DynamicProto.DynamicString fixed1 =
                DynamicProto.DynamicString.newBuilder()
                        .setFixed(FixedProto.FixedString.newBuilder().setValue("a"))
                        .build();
        DynamicProto.DynamicString fixed2 =
                DynamicProto.DynamicString.newBuilder()
                        .setFixed(FixedProto.FixedString.newBuilder().setValue("a"))
                        .build();
        DynamicProto.DynamicString fixed3 =
                DynamicProto.DynamicString.newBuilder()
                        .setFixed(FixedProto.FixedString.newBuilder().setValue("b"))
                        .build();

        assertThat(DynamicProtoHashEquals.equals(fixed1, fixed2)).isTrue();
        assertThat(DynamicProtoHashEquals.hashCode(fixed1))
                .isEqualTo(DynamicProtoHashEquals.hashCode(fixed2));
        assertThat(DynamicProtoHashEquals.equals(fixed1, fixed3)).isFalse();
    }

    @Test
    public void fixedBool_equals() {
        DynamicProto.DynamicBool fixed1 =
                DynamicProto.DynamicBool.newBuilder()
                        .setFixed(FixedProto.FixedBool.newBuilder().setValue(true))
                        .build();
        DynamicProto.DynamicBool fixed2 =
                DynamicProto.DynamicBool.newBuilder()
                        .setFixed(FixedProto.FixedBool.newBuilder().setValue(true))
                        .build();
        DynamicProto.DynamicBool fixed3 =
                DynamicProto.DynamicBool.newBuilder()
                        .setFixed(FixedProto.FixedBool.newBuilder().setValue(false))
                        .build();

        assertThat(DynamicProtoHashEquals.equals(fixed1, fixed2)).isTrue();
        assertThat(DynamicProtoHashEquals.hashCode(fixed1))
                .isEqualTo(DynamicProtoHashEquals.hashCode(fixed2));
        assertThat(DynamicProtoHashEquals.equals(fixed1, fixed3)).isFalse();
    }

    @Test
    public void fixedColor_equals() {
        DynamicProto.DynamicColor fixed1 =
                DynamicProto.DynamicColor.newBuilder()
                        .setFixed(FixedProto.FixedColor.newBuilder().setArgb(0xFF0000FF))
                        .build();
        DynamicProto.DynamicColor fixed2 =
                DynamicProto.DynamicColor.newBuilder()
                        .setFixed(FixedProto.FixedColor.newBuilder().setArgb(0xFF0000FF))
                        .build();
        DynamicProto.DynamicColor fixed3 =
                DynamicProto.DynamicColor.newBuilder()
                        .setFixed(FixedProto.FixedColor.newBuilder().setArgb(0xFFFF0000))
                        .build();

        assertThat(DynamicProtoHashEquals.equals(fixed1, fixed2)).isTrue();
        assertThat(DynamicProtoHashEquals.hashCode(fixed1))
                .isEqualTo(DynamicProtoHashEquals.hashCode(fixed2));
        assertThat(DynamicProtoHashEquals.equals(fixed1, fixed3)).isFalse();
    }

    @Test
    public void arithmeticFloat_equals() {
        DynamicProto.DynamicFloat fixed1 =
                DynamicProto.DynamicFloat.newBuilder()
                        .setFixed(FixedProto.FixedFloat.newBuilder().setValue(1.0f))
                        .build();
        DynamicProto.DynamicFloat fixed2 =
                DynamicProto.DynamicFloat.newBuilder()
                        .setFixed(FixedProto.FixedFloat.newBuilder().setValue(2.0f))
                        .build();
        DynamicProto.ArithmeticFloatOp add1 =
                DynamicProto.ArithmeticFloatOp.newBuilder()
                        .setInputLhs(fixed1)
                        .setInputRhs(fixed2)
                        .setOperationType(DynamicProto.ArithmeticOpType.ARITHMETIC_OP_TYPE_ADD)
                        .build();
        DynamicProto.ArithmeticFloatOp add2 =
                DynamicProto.ArithmeticFloatOp.newBuilder()
                        .setInputLhs(fixed1)
                        .setInputRhs(fixed2)
                        .setOperationType(DynamicProto.ArithmeticOpType.ARITHMETIC_OP_TYPE_ADD)
                        .build();
        DynamicProto.ArithmeticFloatOp subtract =
                DynamicProto.ArithmeticFloatOp.newBuilder()
                        .setInputLhs(fixed1)
                        .setInputRhs(fixed2)
                        .setOperationType(DynamicProto.ArithmeticOpType.ARITHMETIC_OP_TYPE_SUBTRACT)
                        .build();
        DynamicProto.ArithmeticFloatOp addReversed =
                DynamicProto.ArithmeticFloatOp.newBuilder()
                        .setInputLhs(fixed2)
                        .setInputRhs(fixed1)
                        .setOperationType(DynamicProto.ArithmeticOpType.ARITHMETIC_OP_TYPE_ADD)
                        .build();

        DynamicProto.DynamicFloat op1 =
                DynamicProto.DynamicFloat.newBuilder().setArithmeticOperation(add1).build();
        DynamicProto.DynamicFloat op2 =
                DynamicProto.DynamicFloat.newBuilder().setArithmeticOperation(add2).build();
        DynamicProto.DynamicFloat op3 =
                DynamicProto.DynamicFloat.newBuilder().setArithmeticOperation(subtract).build();
        DynamicProto.DynamicFloat op4 =
                DynamicProto.DynamicFloat.newBuilder().setArithmeticOperation(addReversed).build();

        assertThat(DynamicProtoHashEquals.equals(op1, op2)).isTrue();
        assertThat(DynamicProtoHashEquals.hashCode(op1))
                .isEqualTo(DynamicProtoHashEquals.hashCode(op2));
        assertThat(DynamicProtoHashEquals.equals(op1, op3)).isFalse();
        assertThat(DynamicProtoHashEquals.equals(op1, op4)).isFalse();
    }

    @Test
    public void conditionalFloat_equals() {
        DynamicProto.DynamicBool trueBool =
                DynamicProto.DynamicBool.newBuilder()
                        .setFixed(FixedProto.FixedBool.newBuilder().setValue(true))
                        .build();
        DynamicProto.DynamicFloat fixed1 =
                DynamicProto.DynamicFloat.newBuilder()
                        .setFixed(FixedProto.FixedFloat.newBuilder().setValue(1.0f))
                        .build();
        DynamicProto.DynamicFloat fixed2 =
                DynamicProto.DynamicFloat.newBuilder()
                        .setFixed(FixedProto.FixedFloat.newBuilder().setValue(2.0f))
                        .build();

        DynamicProto.ConditionalFloatOp conditional1 =
                DynamicProto.ConditionalFloatOp.newBuilder()
                        .setCondition(trueBool)
                        .setValueIfTrue(fixed1)
                        .setValueIfFalse(fixed2)
                        .build();
        DynamicProto.ConditionalFloatOp conditional2 =
                DynamicProto.ConditionalFloatOp.newBuilder()
                        .setCondition(trueBool)
                        .setValueIfTrue(fixed1)
                        .setValueIfFalse(fixed2)
                        .build();
        DynamicProto.ConditionalFloatOp conditional3 =
                DynamicProto.ConditionalFloatOp.newBuilder()
                        .setCondition(trueBool)
                        .setValueIfTrue(fixed2)
                        .setValueIfFalse(fixed1)
                        .build();

        DynamicProto.DynamicFloat op1 =
                DynamicProto.DynamicFloat.newBuilder().setConditionalOp(conditional1).build();
        DynamicProto.DynamicFloat op2 =
                DynamicProto.DynamicFloat.newBuilder().setConditionalOp(conditional2).build();
        DynamicProto.DynamicFloat op3 =
                DynamicProto.DynamicFloat.newBuilder().setConditionalOp(conditional3).build();

        assertThat(DynamicProtoHashEquals.equals(op1, op2)).isTrue();
        assertThat(DynamicProtoHashEquals.hashCode(op1))
                .isEqualTo(DynamicProtoHashEquals.hashCode(op2));
        assertThat(DynamicProtoHashEquals.equals(op1, op3)).isFalse();
    }

    @Test
    public void logicalBool_equals() {
        DynamicProto.DynamicBool trueBool =
                DynamicProto.DynamicBool.newBuilder()
                        .setFixed(FixedProto.FixedBool.newBuilder().setValue(true))
                        .build();
        DynamicProto.DynamicBool falseBool =
                DynamicProto.DynamicBool.newBuilder()
                        .setFixed(FixedProto.FixedBool.newBuilder().setValue(false))
                        .build();

        DynamicProto.LogicalBoolOp and1 =
                DynamicProto.LogicalBoolOp.newBuilder()
                        .setInputLhs(trueBool)
                        .setInputRhs(falseBool)
                        .setOperationType(DynamicProto.LogicalOpType.LOGICAL_OP_TYPE_AND)
                        .build();
        DynamicProto.LogicalBoolOp and2 =
                DynamicProto.LogicalBoolOp.newBuilder()
                        .setInputLhs(trueBool)
                        .setInputRhs(falseBool)
                        .setOperationType(DynamicProto.LogicalOpType.LOGICAL_OP_TYPE_AND)
                        .build();
        DynamicProto.LogicalBoolOp or =
                DynamicProto.LogicalBoolOp.newBuilder()
                        .setInputLhs(trueBool)
                        .setInputRhs(falseBool)
                        .setOperationType(DynamicProto.LogicalOpType.LOGICAL_OP_TYPE_OR)
                        .build();

        DynamicProto.DynamicBool op1 =
                DynamicProto.DynamicBool.newBuilder().setLogicalOp(and1).build();
        DynamicProto.DynamicBool op2 =
                DynamicProto.DynamicBool.newBuilder().setLogicalOp(and2).build();
        DynamicProto.DynamicBool op3 =
                DynamicProto.DynamicBool.newBuilder().setLogicalOp(or).build();

        assertThat(DynamicProtoHashEquals.equals(op1, op2)).isTrue();
        assertThat(DynamicProtoHashEquals.hashCode(op1))
                .isEqualTo(DynamicProtoHashEquals.hashCode(op2));
        assertThat(DynamicProtoHashEquals.equals(op1, op3)).isFalse();
    }

    @Test
    public void fixedDuration_equals() {
        DynamicProto.DynamicDuration fixed1 =
                DynamicProto.DynamicDuration.newBuilder()
                        .setFixed(FixedProto.FixedDuration.newBuilder().setSeconds(1))
                        .build();
        DynamicProto.DynamicDuration fixed2 =
                DynamicProto.DynamicDuration.newBuilder()
                        .setFixed(FixedProto.FixedDuration.newBuilder().setSeconds(1))
                        .build();
        DynamicProto.DynamicDuration fixed3 =
                DynamicProto.DynamicDuration.newBuilder()
                        .setFixed(FixedProto.FixedDuration.newBuilder().setSeconds(2))
                        .build();

        assertThat(DynamicProtoHashEquals.equals(fixed1, fixed2)).isTrue();
        assertThat(DynamicProtoHashEquals.hashCode(fixed1))
                .isEqualTo(DynamicProtoHashEquals.hashCode(fixed2));
        assertThat(DynamicProtoHashEquals.equals(fixed1, fixed3)).isFalse();
    }

    @Test
    public void fixedInstant_equals() {
        DynamicProto.DynamicInstant fixed1 =
                DynamicProto.DynamicInstant.newBuilder()
                        .setFixed(FixedProto.FixedInstant.newBuilder().setEpochSeconds(1234567890L))
                        .build();
        DynamicProto.DynamicInstant fixed2 =
                DynamicProto.DynamicInstant.newBuilder()
                        .setFixed(FixedProto.FixedInstant.newBuilder().setEpochSeconds(1234567890L))
                        .build();
        DynamicProto.DynamicInstant fixed3 =
                DynamicProto.DynamicInstant.newBuilder()
                        .setFixed(FixedProto.FixedInstant.newBuilder().setEpochSeconds(1234567891L))
                        .build();

        assertThat(DynamicProtoHashEquals.equals(fixed1, fixed2)).isTrue();
        assertThat(DynamicProtoHashEquals.hashCode(fixed1))
                .isEqualTo(DynamicProtoHashEquals.hashCode(fixed2));
        assertThat(DynamicProtoHashEquals.equals(fixed1, fixed3)).isFalse();
    }

    @Test
    public void nullAndDifferentTypes() {
        DynamicProto.DynamicInt32 fixed1 =
                DynamicProto.DynamicInt32.newBuilder()
                        .setFixed(FixedProto.FixedInt32.newBuilder().setValue(1))
                        .build();
        DynamicProto.DynamicString fixedString =
                DynamicProto.DynamicString.newBuilder()
                        .setFixed(FixedProto.FixedString.newBuilder().setValue("a"))
                        .build();

        assertThat(DynamicProtoHashEquals.equals(fixed1, null)).isFalse();
        assertThat(DynamicProtoHashEquals.equals(null, fixed1)).isFalse();
        assertThat(DynamicProtoHashEquals.equals(null, null)).isTrue();
        assertThat(DynamicProtoHashEquals.equals(fixed1, fixedString)).isFalse();
    }
}
