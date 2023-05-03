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

package androidx.wear.protolayout.expression.pipeline;

import android.util.Log;

import androidx.annotation.UiThread;
import androidx.wear.protolayout.expression.proto.DynamicProto;
import androidx.wear.protolayout.expression.proto.DynamicProto.ComparisonFloatOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.ComparisonInt32Op;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateBoolSource;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedBool;

/** Dynamic data nodes which yield boleans. */
class BoolNodes {
    private BoolNodes() {}

    /** Dynamic boolean node that has a fixed value. */
    static class FixedBoolNode implements DynamicDataSourceNode<Boolean> {
        private final boolean mValue;
        private final DynamicTypeValueReceiverWithPreUpdate<Boolean> mDownstream;

        FixedBoolNode(
                FixedBool protoNode, DynamicTypeValueReceiverWithPreUpdate<Boolean> downstream) {
            mValue = protoNode.getValue();
            mDownstream = downstream;
        }

        @Override
        @UiThread
        public void preInit() {
            mDownstream.onPreUpdate();
        }

        @Override
        @UiThread
        public void init() {
            mDownstream.onData(mValue);
        }

        @Override
        @UiThread
        public void destroy() {}
    }

    /** Dynamic boolean node that gets value from the state. */
    static class StateBoolNode extends StateSourceNode<Boolean> {
        StateBoolNode(
                StateStore stateStore,
                StateBoolSource protoNode,
                DynamicTypeValueReceiverWithPreUpdate<Boolean> downstream) {
            super(
                    stateStore,
                    protoNode.getSourceKey(),
                    se -> se.getBoolVal().getValue(),
                    downstream);
        }
    }

    /** Dynamic boolean node that gets value from comparing two integers. */
    static class ComparisonInt32Node extends DynamicDataBiTransformNode<Integer, Integer, Boolean> {
        private static final String TAG = "ComparisonInt32Node";

        ComparisonInt32Node(
                ComparisonInt32Op protoNode,
                DynamicTypeValueReceiverWithPreUpdate<Boolean> downstream) {
            super(
                    downstream,
                    (lhs, rhs) -> {
                        int unboxedLhs = lhs;
                        int unboxedRhs = rhs;

                        switch (protoNode.getOperationType()) {
                            case COMPARISON_OP_TYPE_EQUALS:
                                return unboxedLhs == unboxedRhs;
                            case COMPARISON_OP_TYPE_NOT_EQUALS:
                                return unboxedLhs != unboxedRhs;
                            case COMPARISON_OP_TYPE_LESS_THAN:
                                return unboxedLhs < unboxedRhs;
                            case COMPARISON_OP_TYPE_LESS_THAN_OR_EQUAL_TO:
                                return unboxedLhs <= unboxedRhs;
                            case COMPARISON_OP_TYPE_GREATER_THAN:
                                return unboxedLhs > unboxedRhs;
                            case COMPARISON_OP_TYPE_GREATER_THAN_OR_EQUAL_TO:
                                return unboxedLhs >= unboxedRhs;
                            default:
                                Log.e(TAG, "Unknown operation type in ComparisonInt32Node");
                                return false;
                        }
                    });
        }
    }

    /** Dynamic boolean node that gets value from comparing two floats. */
    static class ComparisonFloatNode extends DynamicDataBiTransformNode<Float, Float, Boolean> {
        private static final String TAG = "ComparisonFloatNode";
        public static final float EPSILON = 1e-6f;

        ComparisonFloatNode(
                ComparisonFloatOp protoNode,
                DynamicTypeValueReceiverWithPreUpdate<Boolean> downstream) {
            super(
                    downstream,
                    (lhs, rhs) -> {
                        float unboxedLhs = lhs;
                        float unboxedRhs = rhs;

                        switch (protoNode.getOperationType()) {
                            case COMPARISON_OP_TYPE_EQUALS:
                                return equalFloats(unboxedLhs, unboxedRhs);
                            case COMPARISON_OP_TYPE_NOT_EQUALS:
                                return !equalFloats(unboxedLhs, unboxedRhs);
                            case COMPARISON_OP_TYPE_LESS_THAN:
                                return (unboxedLhs < unboxedRhs)
                                        && !equalFloats(unboxedLhs, unboxedRhs);
                            case COMPARISON_OP_TYPE_LESS_THAN_OR_EQUAL_TO:
                                return (unboxedLhs < unboxedRhs)
                                        || equalFloats(unboxedLhs, unboxedRhs);
                            case COMPARISON_OP_TYPE_GREATER_THAN:
                                return (unboxedLhs > unboxedRhs)
                                        && !equalFloats(unboxedLhs, unboxedRhs);
                            case COMPARISON_OP_TYPE_GREATER_THAN_OR_EQUAL_TO:
                                return (unboxedLhs > unboxedRhs)
                                        || equalFloats(unboxedLhs, unboxedRhs);
                            default:
                                Log.e(TAG, "Unknown operation type in ComparisonInt32Node");
                                return false;
                        }
                    });
        }

        private static boolean equalFloats(float lhs, float rhs) {
            return Math.abs(lhs - rhs) < EPSILON;
        }
    }

    /** Dynamic boolean node that gets opposite value from another boolean node. */
    static class NotBoolOp extends DynamicDataTransformNode<Boolean, Boolean> {
        NotBoolOp(DynamicTypeValueReceiverWithPreUpdate<Boolean> downstream) {
            super(downstream, b -> !b);
        }
    }

    /** Dynamic boolean node that gets value from logical operation. */
    static class LogicalBoolOp extends DynamicDataBiTransformNode<Boolean, Boolean, Boolean> {
        private static final String TAG = "LogicalBooleanOp";

        LogicalBoolOp(
                DynamicProto.LogicalBoolOp protoNode,
                DynamicTypeValueReceiverWithPreUpdate<Boolean> downstream) {
            super(
                    downstream,
                    (a, b) -> {
                        switch (protoNode.getOperationType()) {
                            case LOGICAL_OP_TYPE_AND:
                                return a && b;
                            case LOGICAL_OP_TYPE_OR:
                                return a || b;
                            case LOGICAL_OP_TYPE_EQUAL:
                                return a.equals(b);
                            case LOGICAL_OP_TYPE_NOT_EQUAL:
                                return !a.equals(b);
                            default:
                                Log.e(TAG, "Unknown operation type in LogicalBoolOp");
                                return false;
                        }
                    });
        }
    }
}
