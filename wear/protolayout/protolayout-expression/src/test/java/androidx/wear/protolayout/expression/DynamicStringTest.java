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

import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString;
import androidx.wear.protolayout.expression.proto.DynamicProto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class DynamicStringTest {
  private static final String STATE_KEY = "state-key";
  private static final String CONSTANT_VALUE = "constant-value";

  @Test
  public void constantString() {
    DynamicString constantString = DynamicString.constant(CONSTANT_VALUE);

    assertThat(constantString.toDynamicStringProto().getFixed().getValue())
        .isEqualTo(CONSTANT_VALUE);
  }

  @Test
  public void constantToString() {
    assertThat(DynamicString.constant("a").toString()).isEqualTo("FixedString{value=a}");
  }

  @Test
  public void stateEntryValueString() {
    DynamicString stateString = DynamicString.from(new AppDataKey<>(STATE_KEY));

    assertThat(stateString.toDynamicStringProto().getStateSource().getSourceKey())
        .isEqualTo(STATE_KEY);
  }

  @Test
  public void stateToString() {
    assertThat(DynamicString.from(new AppDataKey<>("key")).toString())
        .isEqualTo("StateStringSource{sourceKey=key, sourceNamespace=}");
  }

  @Test
  public void constantString_concat() {
    DynamicString firstString = DynamicString.constant(CONSTANT_VALUE);
    DynamicString secondString = DynamicString.constant(CONSTANT_VALUE);

    DynamicString resultString = firstString.concat(secondString);

    DynamicProto.ConcatStringOp concatOp = resultString.toDynamicStringProto().getConcatOp();
    assertThat(concatOp.getInputLhs()).isEqualTo(firstString.toDynamicStringProto());
    assertThat(concatOp.getInputRhs()).isEqualTo(secondString.toDynamicStringProto());
  }

  @Test
  public void concatToString() {
    assertThat(DynamicString.constant("a").concat(DynamicString.constant("b")).toString())
        .isEqualTo("ConcatStringOp{inputLhs=FixedString{value=a}, inputRhs=FixedString{value=b}}");
  }

  @Test
  public void constantString_conditional() {
    DynamicBool condition = DynamicBool.constant(true);
    DynamicString firstString = DynamicString.constant(CONSTANT_VALUE);
    DynamicString secondString = DynamicString.constant(CONSTANT_VALUE);

    DynamicString resultString =
        DynamicString.onCondition(condition).use(firstString).elseUse(secondString);

    DynamicProto.ConditionalStringOp conditionalOp =
        resultString.toDynamicStringProto().getConditionalOp();
    assertThat(conditionalOp.getCondition()).isEqualTo(condition.toDynamicBoolProto());
    assertThat(conditionalOp.getValueIfTrue()).isEqualTo(firstString.toDynamicStringProto());
    assertThat(conditionalOp.getValueIfFalse()).isEqualTo(secondString.toDynamicStringProto());
  }

  @Test
  public void rawString_conditional() {
    DynamicBool condition = DynamicBool.constant(true);
    String firstString = "raw-string";
    DynamicString secondString = DynamicString.constant(CONSTANT_VALUE);

    DynamicString resultString =
        DynamicString.onCondition(condition).use(firstString).elseUse(secondString);

    DynamicProto.ConditionalStringOp conditionalOp =
        resultString.toDynamicStringProto().getConditionalOp();
    assertThat(conditionalOp.getCondition()).isEqualTo(condition.toDynamicBoolProto());
    assertThat(conditionalOp.getValueIfTrue().getFixed().getValue()).isEqualTo(firstString);
    assertThat(conditionalOp.getValueIfFalse()).isEqualTo(secondString.toDynamicStringProto());
  }

  @Test
  public void conditionalToString() {
    assertThat(
            DynamicString.onCondition(DynamicBool.constant(true)).use("a").elseUse("b").toString())
        .isEqualTo(
            "ConditionalStringOp{"
                + "condition=FixedBool{value=true}, "
                + "valueIfTrue=FixedString{value=a}, "
                + "valueIfFalse=FixedString{value=b}}");
  }

  @Test
  public void validProto() {
    DynamicString from = DynamicString.constant(CONSTANT_VALUE);
    DynamicString to = DynamicString.fromByteArray(from.toDynamicStringByteArray());

    assertThat(to.toDynamicStringProto().getFixed().getValue()).isEqualTo(CONSTANT_VALUE);
  }

  @Test
  public void invalidProto() {
    assertThrows(IllegalArgumentException.class, () -> DynamicString.fromByteArray(new byte[] {1}));
  }
}
