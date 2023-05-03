/*
 * Copyright 2023 The Android Open Source Project
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

import static androidx.wear.protolayout.expression.proto.DynamicProto.LogicalOpType.LOGICAL_OP_TYPE_AND;
import static androidx.wear.protolayout.expression.proto.DynamicProto.LogicalOpType.LOGICAL_OP_TYPE_EQUAL;
import static androidx.wear.protolayout.expression.proto.DynamicProto.LogicalOpType.LOGICAL_OP_TYPE_NOT_EQUAL;
import static androidx.wear.protolayout.expression.proto.DynamicProto.LogicalOpType.LOGICAL_OP_TYPE_OR;
import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.pipeline.BoolNodes.FixedBoolNode;
import androidx.wear.protolayout.expression.pipeline.BoolNodes.StateBoolNode;
import androidx.wear.protolayout.expression.proto.DynamicProto;
import androidx.wear.protolayout.expression.proto.DynamicProto.LogicalBoolOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateBoolSource;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedBool;
import androidx.wear.protolayout.expression.proto.StateEntryProto.StateEntryValue;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BoolNodesTest {
  @Test
  public void fixedBoolNodeTest() {
    List<Boolean> results = new ArrayList<>();

    FixedBool protoNode = FixedBool.newBuilder().setValue(false).build();
    FixedBoolNode node = new FixedBoolNode(protoNode, new AddToListCallback<>(results));

    node.preInit();
    node.init();

    assertThat(results).containsExactly(false);
  }

  @Test
  public void stateBoolNodeTest() {
    List<Boolean> results = new ArrayList<>();
    StateStore oss =
        new StateStore(
            ImmutableMap.of(
                "foo",
                StateEntryValue.newBuilder()
                    .setBoolVal(FixedBool.newBuilder().setValue(true))
                    .build()));

    StateBoolSource protoNode = StateBoolSource.newBuilder().setSourceKey("foo").build();
    StateBoolNode node = new StateBoolNode(oss, protoNode, new AddToListCallback<>(results));

    node.preInit();
    node.init();

    assertThat(results).containsExactly(true);
  }

  @Test
  public void stateBoolUpdatesWithStateChanges() {
    List<Boolean> results = new ArrayList<>();
    StateStore oss =
        new StateStore(
            ImmutableMap.of(
                "foo",
                StateEntryValue.newBuilder()
                    .setBoolVal(FixedBool.newBuilder().setValue(true))
                    .build()));

    StateBoolSource protoNode = StateBoolSource.newBuilder().setSourceKey("foo").build();
    StateBoolNode node = new StateBoolNode(oss, protoNode, new AddToListCallback<>(results));

    node.preInit();
    node.init();

    results.clear();

    oss.setStateEntryValuesProto(
        ImmutableMap.of(
            "foo",
            StateEntryValue.newBuilder()
                .setBoolVal(FixedBool.newBuilder().setValue(false))
                .build()));

    assertThat(results).containsExactly(false);
  }

  @Test
  public void stateBoolNoUpdatesAfterDestroy() {
    List<Boolean> results = new ArrayList<>();
    StateStore oss =
        new StateStore(
            ImmutableMap.of(
                "foo",
                StateEntryValue.newBuilder()
                    .setBoolVal(FixedBool.newBuilder().setValue(false))
                    .build()));

    StateBoolSource protoNode = StateBoolSource.newBuilder().setSourceKey("foo").build();
    StateBoolNode node = new StateBoolNode(oss, protoNode, new AddToListCallback<>(results));

    node.preInit();
    node.init();
    assertThat(results).containsExactly(false);

    results.clear();
    node.destroy();
    oss.setStateEntryValuesProto(
        ImmutableMap.of(
            "foo",
            StateEntryValue.newBuilder()
                .setBoolVal(FixedBool.newBuilder().setValue(true))
                .build()));
    assertThat(results).isEmpty();
  }

  @Test
  public void logicalBoolOpTest() {
    assertThat(evaluateLogicalOperation(LOGICAL_OP_TYPE_AND, true, true)).isTrue();
    assertThat(evaluateLogicalOperation(LOGICAL_OP_TYPE_AND, true, false)).isFalse();

    assertThat(evaluateLogicalOperation(LOGICAL_OP_TYPE_OR, true, false)).isTrue();
    assertThat(evaluateLogicalOperation(LOGICAL_OP_TYPE_OR, false, false)).isFalse();

    assertThat(evaluateLogicalOperation(LOGICAL_OP_TYPE_EQUAL, true, true)).isTrue();
    assertThat(evaluateLogicalOperation(LOGICAL_OP_TYPE_EQUAL, true, false)).isFalse();

    assertThat(evaluateLogicalOperation(LOGICAL_OP_TYPE_NOT_EQUAL, true, false)).isTrue();
    assertThat(evaluateLogicalOperation(LOGICAL_OP_TYPE_NOT_EQUAL, false, false)).isFalse();
  }

  private static boolean evaluateLogicalOperation(
      DynamicProto.LogicalOpType logicalOpType, boolean lhs, boolean rhs) {
    List<Boolean> results = new ArrayList<>();

    LogicalBoolOp protoNode = LogicalBoolOp.newBuilder().setOperationType(logicalOpType).build();
    BoolNodes.LogicalBoolOp node =
        new BoolNodes.LogicalBoolOp(protoNode, new AddToListCallback<>(results));

    FixedBool lhsProtoNode = FixedBool.newBuilder().setValue(lhs).build();
    FixedBoolNode lhsNode = new FixedBoolNode(lhsProtoNode, node.getLhsIncomingCallback());
    lhsNode.init();

    FixedBool rhsProtoNode = FixedBool.newBuilder().setValue(rhs).build();
    FixedBoolNode rhsNode = new FixedBoolNode(rhsProtoNode, node.getRhsIncomingCallback());
    rhsNode.init();

    return results.get(0);
  }
}
