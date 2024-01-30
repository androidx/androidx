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

import androidx.annotation.UiThread;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedDuration;

import java.time.Duration;
import java.time.Instant;

/** Dynamic data nodes which yield durations. */
class DurationNodes {
    private DurationNodes() {}

    /** Dynamic duration node that has a fixed value. */
    static class FixedDurationNode implements DynamicDataSourceNode<Duration> {
        private final Duration mValue;
        private final DynamicTypeValueReceiverWithPreUpdate<Duration> mDownstream;

        FixedDurationNode(
                FixedDuration protoNode,
                DynamicTypeValueReceiverWithPreUpdate<Duration> downstream) {
            this.mValue = Duration.ofSeconds(protoNode.getSeconds());
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
        public void destroy() {}
    }

    /** Dynamic duration node that gets the duration between two time instants. */
    static class BetweenInstancesNode
            extends DynamicDataBiTransformNode<Instant, Instant, Duration> {

        BetweenInstancesNode(DynamicTypeValueReceiverWithPreUpdate<Duration> downstream) {
            super(downstream, Duration::between);
        }
    }
}
