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

import static java.lang.Math.min;

import androidx.annotation.UiThread;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateStringSource;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedString;

/** Dynamic data nodes which yield Strings. */
class StringNodes {

    static final int MAXIMUM_STRING_LENGTH = 200;

    private StringNodes() {}

    /** Dynamic string node that has a fixed value. */
    static class FixedStringNode implements DynamicDataSourceNode<String> {
        private final String mValue;
        private final DynamicTypeValueReceiverWithPreUpdate<String> mDownstream;

        FixedStringNode(
                FixedString protoNode, DynamicTypeValueReceiverWithPreUpdate<String> downstream) {
            this.mValue = truncate(protoNode.getValue());
            this.mDownstream = downstream;
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

    /** Dynamic string node that gets a value from integer. */
    static class Int32FormatNode extends DynamicDataTransformNode<Integer, String> {
        Int32FormatNode(
                NumberFormatter formatter,
                DynamicTypeValueReceiverWithPreUpdate<String> downstream) {
            super(downstream, (value) -> truncate(formatter.format(value)));
        }
    }

    /** Dynamic string node that gets a value from the other strings. */
    static class StringConcatOpNode extends DynamicDataBiTransformNode<String, String, String> {
        StringConcatOpNode(DynamicTypeValueReceiverWithPreUpdate<String> downstream) {
            super(
                    downstream,
                    (lhs, rhs) -> {
                        int lhsLength = lhs.length();
                        int rhsLength = rhs.length();

                        if (lhsLength >= MAXIMUM_STRING_LENGTH) {
                            return truncate(lhs);
                        }

                        return lhs.concat(
                                rhs.substring(
                                        0, min(rhsLength, MAXIMUM_STRING_LENGTH - lhsLength)));
                    });
        }
    }

    /** Dynamic string node that gets a value from float. */
    static class FloatFormatNode extends DynamicDataTransformNode<Float, String> {

        FloatFormatNode(
                NumberFormatter formatter,
                DynamicTypeValueReceiverWithPreUpdate<String> downstream) {
            super(downstream, (value) -> truncate(formatter.format(value)));
        }
    }

    /** Dynamic string node that gets a value from the state. */
    static class StateStringNode extends StateSourceNode<String> {
        StateStringNode(
                DataStore dataStore,
                StateStringSource protoNode,
                DynamicTypeValueReceiverWithPreUpdate<String> downstream) {
            super(
                    dataStore,
                    StateSourceNode.<DynamicString>createKey(
                            protoNode.getSourceNamespace(), protoNode.getSourceKey()),
                    se -> truncate(se.getStringVal().getValue()),
                    downstream);
        }
    }

    static String truncate(String input) {
        return input.substring(0, min(input.length(), MAXIMUM_STRING_LENGTH));
    }
}
