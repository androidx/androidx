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

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedInstant;

import java.time.Instant;

/** Dynamic data nodes which yield instants. */
class InstantNodes {
    private InstantNodes() {}

    /** Dynamic instant node that has a fixed value. */
    static class FixedInstantNode implements DynamicDataSourceNode<Integer> {
        private final Instant mValue;
        private final DynamicTypeValueReceiverWithPreUpdate<Instant> mDownstream;

        FixedInstantNode(
                FixedInstant protoNode,
                DynamicTypeValueReceiverWithPreUpdate<Instant> downstream) {
            this.mValue = Instant.ofEpochSecond(protoNode.getEpochSeconds());
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

    /** Dynamic Instant node that gets value from the platform source. */
    static class PlatformTimeSourceNode implements DynamicDataSourceNode<Integer> {
        @Nullable private final EpochTimePlatformDataSource mEpochTimePlatformDataSource;
        private final DynamicTypeValueReceiverWithPreUpdate<Instant> mDownstream;

        PlatformTimeSourceNode(
                @Nullable EpochTimePlatformDataSource epochTimePlatformDataSource,
                DynamicTypeValueReceiverWithPreUpdate<Instant> downstream) {
            this.mEpochTimePlatformDataSource = epochTimePlatformDataSource;
            this.mDownstream = downstream;
        }

        @Override
        @UiThread
        public void preInit() {
            if (mEpochTimePlatformDataSource != null) {
                mEpochTimePlatformDataSource.preRegister();
            } else {
                // If we have epoch time, it will call onPreUpdate when needed. Otherwise, because
                // the init() will invalidate the date in downstream, we should call onPreUpdate
                // here.
                mDownstream.onPreUpdate();
            }
        }

        @Override
        @UiThread
        public void init() {
            if (mEpochTimePlatformDataSource != null) {
                mEpochTimePlatformDataSource.registerForData(mDownstream);
            } else {
                mDownstream.onInvalidated();
            }
        }

        @Override
        @UiThread
        public void destroy() {
            if (mEpochTimePlatformDataSource != null) {
                mEpochTimePlatformDataSource.unregisterForData(mDownstream);
            }
        }
    }
}
