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

import static androidx.wear.protolayout.expression.proto.DynamicProto.LogicalOpType.LOGICAL_OP_TYPE_AND;
import static androidx.wear.protolayout.expression.proto.DynamicProto.LogicalOpType.LOGICAL_OP_TYPE_OR;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class DynamicBoolTest {
  private static final String STATE_KEY = "state-key";

  @Test
  public void constantBool() {
    DynamicBool falseBool = DynamicBool.constant(false);
    DynamicBool trueBool = DynamicBool.constant(true);

    assertThat(falseBool.toDynamicBoolProto().getFixed().getValue()).isFalse();
    assertThat(trueBool.toDynamicBoolProto().getFixed().getValue()).isTrue();
  }

  @Test
  public void constantToString() {
    assertThat(DynamicBool.constant(true).toString()).isEqualTo("FixedBool{value=true}");
  }

  @Test
  public void stateEntryValueBool() {
    DynamicBool stateBool = DynamicBool.fromState(STATE_KEY);

    assertThat(stateBool.toDynamicBoolProto().getStateSource().getSourceKey()).isEqualTo(STATE_KEY);
  }

  @Test
  public void stateToString() {
    assertThat(DynamicBool.fromState("key").toString()).isEqualTo("StateBoolSource{sourceKey=key}");
  }

  @Test
  public void andOpBool() {
    DynamicBool firstBool = DynamicBool.constant(false);
    DynamicBool secondBool = DynamicBool.constant(true);

    DynamicBool result = firstBool.and(secondBool);
    assertThat(result.toDynamicBoolProto().getLogicalOp().getOperationType())
        .isEqualTo(LOGICAL_OP_TYPE_AND);
    assertThat(result.toDynamicBoolProto().getLogicalOp().getInputLhs())
        .isEqualTo(firstBool.toDynamicBoolProto());
    assertThat(result.toDynamicBoolProto().getLogicalOp().getInputRhs())
        .isEqualTo(secondBool.toDynamicBoolProto());
  }

  @Test
  public void orOpBool() {
    DynamicBool firstBool = DynamicBool.constant(false);
    DynamicBool secondBool = DynamicBool.constant(true);

    DynamicBool result = firstBool.or(secondBool);
    assertThat(result.toDynamicBoolProto().getLogicalOp().getOperationType())
        .isEqualTo(LOGICAL_OP_TYPE_OR);
    assertThat(result.toDynamicBoolProto().getLogicalOp().getInputLhs())
        .isEqualTo(firstBool.toDynamicBoolProto());
    assertThat(result.toDynamicBoolProto().getLogicalOp().getInputRhs())
        .isEqualTo(secondBool.toDynamicBoolProto());
  }

  @Test
  public void logicalOpToString() {
    assertThat(DynamicBool.constant(true).and(DynamicBool.constant(false)).toString())
        .isEqualTo(
            "LogicalBoolOp{"
                + "inputLhs=FixedBool{value=true}, "
                + "inputRhs=FixedBool{value=false}, "
                + "operationType=1}");
  }

  @Test
  public void negateOpBool() {
    DynamicBool firstBool = DynamicBool.constant(true);

    assertThat(firstBool.negate().toDynamicBoolProto().getNotOp().getInput())
        .isEqualTo(firstBool.toDynamicBoolProto());
  }

  @Test
  public void logicalToString() {
    assertThat(DynamicBool.constant(true).negate().toString())
        .isEqualTo("NotBoolOp{input=FixedBool{value=true}}");
  }

  @Test
  public void validProto() {
    DynamicBool from = DynamicBool.constant(true);
    DynamicBool to = DynamicBool.fromByteArray(from.toDynamicBoolByteArray());

    assertThat(to.toDynamicBoolProto().getFixed().getValue()).isTrue();
  }

  @Test
  public void invalidProto() {
    assertThrows(IllegalArgumentException.class, () -> DynamicBool.fromByteArray(new byte[] {1}));
  }
}
