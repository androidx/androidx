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

import static androidx.wear.protolayout.expression.proto.DynamicProto.ComparisonOpType.COMPARISON_OP_TYPE_EQUALS;
import static androidx.wear.protolayout.expression.proto.DynamicProto.ComparisonOpType.COMPARISON_OP_TYPE_GREATER_THAN;
import static androidx.wear.protolayout.expression.proto.DynamicProto.ComparisonOpType.COMPARISON_OP_TYPE_GREATER_THAN_OR_EQUAL_TO;
import static androidx.wear.protolayout.expression.proto.DynamicProto.ComparisonOpType.COMPARISON_OP_TYPE_LESS_THAN;
import static androidx.wear.protolayout.expression.proto.DynamicProto.ComparisonOpType.COMPARISON_OP_TYPE_LESS_THAN_OR_EQUAL_TO;
import static androidx.wear.protolayout.expression.proto.DynamicProto.ComparisonOpType.COMPARISON_OP_TYPE_NOT_EQUALS;
import static androidx.wear.protolayout.expression.proto.DynamicProto.ComparisonOpType.COMPARISON_OP_TYPE_UNDEFINED;
import static androidx.wear.protolayout.expression.proto.DynamicProto.LogicalOpType.LOGICAL_OP_TYPE_AND;
import static androidx.wear.protolayout.expression.proto.DynamicProto.LogicalOpType.LOGICAL_OP_TYPE_EQUAL;
import static androidx.wear.protolayout.expression.proto.DynamicProto.LogicalOpType.LOGICAL_OP_TYPE_NOT_EQUAL;
import static androidx.wear.protolayout.expression.proto.DynamicProto.LogicalOpType.LOGICAL_OP_TYPE_OR;
import static androidx.wear.protolayout.expression.proto.DynamicProto.LogicalOpType.LOGICAL_OP_TYPE_UNDEFINED;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool;
import androidx.wear.protolayout.expression.pipeline.BoolNodes.FixedBoolNode;
import androidx.wear.protolayout.expression.pipeline.BoolNodes.StateBoolNode;
import androidx.wear.protolayout.expression.pipeline.FloatNodes.FixedFloatNode;
import androidx.wear.protolayout.expression.pipeline.Int32Nodes.FixedInt32Node;
import androidx.wear.protolayout.expression.proto.DynamicDataProto.DynamicDataValue;
import androidx.wear.protolayout.expression.proto.DynamicProto;
import androidx.wear.protolayout.expression.proto.DynamicProto.LogicalBoolOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateBoolSource;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedBool;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedFloat;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedInt32;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

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
                                new AppDataKey<DynamicBool>("foo"),
                                DynamicDataValue.newBuilder()
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
                                new AppDataKey<DynamicBool>("foo"),
                                DynamicDataValue.newBuilder()
                                        .setBoolVal(FixedBool.newBuilder().setValue(true))
                                        .build()));

        StateBoolSource protoNode = StateBoolSource.newBuilder().setSourceKey("foo").build();
        StateBoolNode node = new StateBoolNode(oss, protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();

        results.clear();

        oss.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        new AppDataKey<DynamicBool>("foo"),
                        DynamicDataValue.newBuilder()
                                .setBoolVal(FixedBool.newBuilder().setValue(false))
                                .build()));

        assertThat(results).containsExactly(false);
    }

    @Test
    public void stateBoolNoUpdatesAfterDestroy() {
        List<Boolean> results = new ArrayList<>();
        AppDataKey<DynamicBool> keyFoo = new AppDataKey<>("foo");
        StateStore oss =
                new StateStore(
                        ImmutableMap.of(
                                keyFoo,
                                DynamicDataValue.newBuilder()
                                        .setBoolVal(FixedBool.newBuilder().setValue(false))
                                        .build()));

        StateBoolSource protoNode = StateBoolSource.newBuilder().setSourceKey("foo").build();
        StateBoolNode node = new StateBoolNode(oss, protoNode, new AddToListCallback<>(results));

        node.preInit();
        node.init();
        assertThat(results).containsExactly(false);

        results.clear();
        node.destroy();
        oss.setAppStateEntryValuesProto(
                ImmutableMap.of(
                        keyFoo,
                        DynamicDataValue.newBuilder()
                                .setBoolVal(FixedBool.newBuilder().setValue(true))
                                .build()));
        assertThat(results).isEmpty();
    }

    @Test
    public void logicalBoolOpTest_unknownOp_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> evaluateLogicalOperation(LOGICAL_OP_TYPE_UNDEFINED, true, true));
        assertThrows(
                IllegalArgumentException.class,
                () -> evaluateLogicalOperation(-1 /* UNRECOGNIZED */, true, true));
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

    @Test
    public void int32CompareOp_unknownOp_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> evaluateInt32ComparisonOperation(COMPARISON_OP_TYPE_UNDEFINED, 1, 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> evaluateInt32ComparisonOperation(-1 /* UNRECOGNIZED */, 1, 1));
    }

    @Test
    public void int32CompareOpTest() {
        assertThat(evaluateInt32ComparisonOperation(COMPARISON_OP_TYPE_EQUALS, 1, 1)).isTrue();
        assertThat(evaluateInt32ComparisonOperation(COMPARISON_OP_TYPE_EQUALS, 1, 2)).isFalse();

        assertThat(evaluateInt32ComparisonOperation(COMPARISON_OP_TYPE_NOT_EQUALS, 1, 1)).isFalse();
        assertThat(evaluateInt32ComparisonOperation(COMPARISON_OP_TYPE_NOT_EQUALS, 1, 2)).isTrue();

        assertThat(evaluateInt32ComparisonOperation(COMPARISON_OP_TYPE_GREATER_THAN, 1, 2))
                .isFalse();
        assertThat(evaluateInt32ComparisonOperation(COMPARISON_OP_TYPE_GREATER_THAN, 1, 1))
                .isFalse();
        assertThat(evaluateInt32ComparisonOperation(COMPARISON_OP_TYPE_GREATER_THAN, 2, 1))
                .isTrue();

        assertThat(
                        evaluateInt32ComparisonOperation(
                                COMPARISON_OP_TYPE_GREATER_THAN_OR_EQUAL_TO, 1, 2))
                .isFalse();
        assertThat(
                        evaluateInt32ComparisonOperation(
                                COMPARISON_OP_TYPE_GREATER_THAN_OR_EQUAL_TO, 1, 1))
                .isTrue();
        assertThat(
                        evaluateInt32ComparisonOperation(
                                COMPARISON_OP_TYPE_GREATER_THAN_OR_EQUAL_TO, 2, 1))
                .isTrue();

        assertThat(evaluateInt32ComparisonOperation(COMPARISON_OP_TYPE_LESS_THAN, 2, 1)).isFalse();
        assertThat(evaluateInt32ComparisonOperation(COMPARISON_OP_TYPE_LESS_THAN, 1, 1)).isFalse();
        assertThat(evaluateInt32ComparisonOperation(COMPARISON_OP_TYPE_LESS_THAN, 1, 2)).isTrue();

        assertThat(evaluateInt32ComparisonOperation(COMPARISON_OP_TYPE_LESS_THAN_OR_EQUAL_TO, 2, 1))
                .isFalse();
        assertThat(evaluateInt32ComparisonOperation(COMPARISON_OP_TYPE_LESS_THAN_OR_EQUAL_TO, 1, 1))
                .isTrue();
        assertThat(evaluateInt32ComparisonOperation(COMPARISON_OP_TYPE_LESS_THAN_OR_EQUAL_TO, 1, 2))
                .isTrue();
    }

    @Test
    public void floatCompareOp_unknownOp_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> evaluateFloatComparisonOperation(COMPARISON_OP_TYPE_UNDEFINED, 1, 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> evaluateFloatComparisonOperation(-1 /* UNRECOGNIZED */, 1, 1));
    }

    @Test
    public void floatCompareOpTest() {
        assertThat(evaluateFloatComparisonOperation(COMPARISON_OP_TYPE_EQUALS, 1, 1)).isTrue();
        assertThat(evaluateFloatComparisonOperation(COMPARISON_OP_TYPE_EQUALS, 1, 2)).isFalse();

        assertThat(evaluateFloatComparisonOperation(COMPARISON_OP_TYPE_NOT_EQUALS, 1, 1)).isFalse();
        assertThat(evaluateFloatComparisonOperation(COMPARISON_OP_TYPE_NOT_EQUALS, 1, 2)).isTrue();

        assertThat(evaluateFloatComparisonOperation(COMPARISON_OP_TYPE_GREATER_THAN, 1, 2))
                .isFalse();
        assertThat(evaluateFloatComparisonOperation(COMPARISON_OP_TYPE_GREATER_THAN, 1, 1))
                .isFalse();
        assertThat(evaluateFloatComparisonOperation(COMPARISON_OP_TYPE_GREATER_THAN, 2, 1))
                .isTrue();

        assertThat(
                        evaluateFloatComparisonOperation(
                                COMPARISON_OP_TYPE_GREATER_THAN_OR_EQUAL_TO, 1, 2))
                .isFalse();
        assertThat(
                        evaluateFloatComparisonOperation(
                                COMPARISON_OP_TYPE_GREATER_THAN_OR_EQUAL_TO, 1, 1))
                .isTrue();
        assertThat(
                        evaluateFloatComparisonOperation(
                                COMPARISON_OP_TYPE_GREATER_THAN_OR_EQUAL_TO, 2, 1))
                .isTrue();

        assertThat(evaluateFloatComparisonOperation(COMPARISON_OP_TYPE_LESS_THAN, 2, 1)).isFalse();
        assertThat(evaluateFloatComparisonOperation(COMPARISON_OP_TYPE_LESS_THAN, 1, 1)).isFalse();
        assertThat(evaluateFloatComparisonOperation(COMPARISON_OP_TYPE_LESS_THAN, 1, 2)).isTrue();

        assertThat(evaluateFloatComparisonOperation(COMPARISON_OP_TYPE_LESS_THAN_OR_EQUAL_TO, 2, 1))
                .isFalse();
        assertThat(evaluateFloatComparisonOperation(COMPARISON_OP_TYPE_LESS_THAN_OR_EQUAL_TO, 1, 1))
                .isTrue();
        assertThat(evaluateFloatComparisonOperation(COMPARISON_OP_TYPE_LESS_THAN_OR_EQUAL_TO, 1, 2))
                .isTrue();
    }

    private static boolean evaluateLogicalOperation(
            DynamicProto.LogicalOpType logicalOpType, boolean lhs, boolean rhs) {
        return evaluateLogicalOperation(logicalOpType.getNumber(), lhs, rhs);
    }

    private static boolean evaluateLogicalOperation(int logicalOpType, boolean lhs, boolean rhs) {
        List<Boolean> results = new ArrayList<>();

        LogicalBoolOp protoNode =
                LogicalBoolOp.newBuilder().setOperationTypeValue(logicalOpType).build();
        BoolNodes.LogicalBoolOp node =
                new BoolNodes.LogicalBoolOp(protoNode, new AddToListCallback<>(results));

        FixedBool lhsProtoNode = FixedBool.newBuilder().setValue(lhs).build();
        FixedBoolNode lhsNode = new FixedBoolNode(lhsProtoNode, node.getLhsUpstreamCallback());

        FixedBool rhsProtoNode = FixedBool.newBuilder().setValue(rhs).build();
        FixedBoolNode rhsNode = new FixedBoolNode(rhsProtoNode, node.getRhsUpstreamCallback());

        lhsNode.preInit();
        rhsNode.preInit();

        lhsNode.init();
        rhsNode.init();

        return results.get(0);
    }

    private static boolean evaluateInt32ComparisonOperation(
            DynamicProto.ComparisonOpType opType, int lhs, int rhs) {
        return evaluateInt32ComparisonOperation(opType.getNumber(), lhs, rhs);
    }

    private static boolean evaluateInt32ComparisonOperation(int opType, int lhs, int rhs) {
        List<Boolean> results = new ArrayList<>();

        DynamicProto.ComparisonInt32Op protoNode =
                DynamicProto.ComparisonInt32Op.newBuilder().setOperationTypeValue(opType).build();
        BoolNodes.ComparisonInt32Node node =
                new BoolNodes.ComparisonInt32Node(protoNode, new AddToListCallback<>(results));

        FixedInt32 lhsProtoNode = FixedInt32.newBuilder().setValue(lhs).build();
        FixedInt32Node lhsNode = new FixedInt32Node(lhsProtoNode, node.getLhsUpstreamCallback());
        lhsNode.preInit();

        FixedInt32 rhsProtoNode = FixedInt32.newBuilder().setValue(rhs).build();
        FixedInt32Node rhsNode = new FixedInt32Node(rhsProtoNode, node.getRhsUpstreamCallback());
        rhsNode.preInit();

        lhsNode.init();
        rhsNode.init();

        return results.get(0);
    }

    private static boolean evaluateFloatComparisonOperation(
            DynamicProto.ComparisonOpType opType, float lhs, float rhs) {
        return evaluateFloatComparisonOperation(opType.getNumber(), lhs, rhs);
    }

    private static boolean evaluateFloatComparisonOperation(int opType, float lhs, float rhs) {
        List<Boolean> results = new ArrayList<>();

        DynamicProto.ComparisonFloatOp protoNode =
                DynamicProto.ComparisonFloatOp.newBuilder().setOperationTypeValue(opType).build();
        BoolNodes.ComparisonFloatNode node =
                new BoolNodes.ComparisonFloatNode(protoNode, new AddToListCallback<>(results));

        FixedFloat lhsProtoNode = FixedFloat.newBuilder().setValue(lhs).build();
        FixedFloatNode lhsNode = new FixedFloatNode(lhsProtoNode, node.getLhsUpstreamCallback());
        lhsNode.preInit();

        FixedFloat rhsProtoNode = FixedFloat.newBuilder().setValue(rhs).build();
        FixedFloatNode rhsNode = new FixedFloatNode(rhsProtoNode, node.getRhsUpstreamCallback());
        rhsNode.preInit();

        lhsNode.init();
        rhsNode.init();

        return results.get(0);
    }
}
