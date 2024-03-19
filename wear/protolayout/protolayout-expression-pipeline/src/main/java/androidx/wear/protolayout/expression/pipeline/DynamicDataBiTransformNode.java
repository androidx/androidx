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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Dynamic data node that can perform a transformation from two upstream nodes. This should be
 * created by passing a {@link Function} in, which implements the transformation.
 *
 * <p>The two inputs to this are called the left/right-hand side of the operation, since many of the
 * operations extending this class are likely to be simple maths operations. Conventionally then,
 * descendants of this class will implement operations of the form "O = LHS [op] RHS", or "O =
 * op(LHS, RHS)".
 *
 * <p>This node will wait until there's no pending data from either side:
 *
 * <ul>
 *   <li>This node will wait until both sides invoked either {@code onData} or {@code onInvalidated}
 *       at least once.
 *   <li>If one side invoked {@code onPreUpdate}, this node will wait until it invoked either {@code
 *       onData} or {@code onInvalidated}.
 *   <li>This node expects each side to invoke exactly on {@code onData} or {@code onInvalidated}
 *       after each {@code onPreUpdate}:
 *       <ul>
 *         <li>If {@code onData} or {@code onInvalidated} are invoked without {@code onPreUpdate},
 *             this node will log a warning and downstream the callback immediately (unless waiting
 *             for the other side).
 *         <li>If {@code onPreUpdate} is invoked more than once without {@code onData} or {@code
 *             onInvalidated}, this node will log a warning and ignore the followup {@code
 *             onPreUpdate}.
 *       </ul>
 *       Both of these scenarios can mean {@link DynamicTypeEvaluator} might publish a partially
 *       evaluated update.
 * </ul>
 *
 * @param <LhsT> The source data type for the left-hand side of the operation.
 * @param <RhsT> The source data type for the right-hand side of the operation.
 * @param <O> The data type that this node emits.
 */
class DynamicDataBiTransformNode<LhsT, RhsT, O> implements DynamicDataNode<O> {
    private static final String TAG = "DynamicDataBiTransform";

    private final UpstreamCallback<LhsT> mLhsUpstreamCallback = new UpstreamCallback<>();
    private final UpstreamCallback<RhsT> mRhsUpstreamCallback = new UpstreamCallback<>();

    final DynamicTypeValueReceiverWithPreUpdate<O> mDownstream;
    private final BiFunction<LhsT, RhsT, O> mTransformer;

    boolean mDownstreamPreUpdated = false;

    DynamicDataBiTransformNode(
            DynamicTypeValueReceiverWithPreUpdate<O> downstream,
            BiFunction<LhsT, RhsT, O> transformer) {
        this.mDownstream = downstream;
        this.mTransformer = transformer;
    }

    @Override
    public int getCost() {
        return DEFAULT_NODE_COST;
    }

    private class UpstreamCallback<T> implements DynamicTypeValueReceiverWithPreUpdate<T> {
        private boolean mUpstreamPreUpdated = false;

        /**
         * Whether {@link #cache} should be used, i.e. set to true when either {@link #onData} or
         * {@link #onInvalidated} were invoked at least once, and there's no pending {@link
         * #onPreUpdate} call.
         */
        boolean isCacheReady = false;

        /**
         * Latest value arrived in {@link #onData}, or {@code null} if the last callback was {@link
         * #onInvalidated}. Should not be used if {@link #isCacheReady} is {@code false}.
         */
        @Nullable T cache;

        @Override
        public void onPreUpdate() {
            if (mUpstreamPreUpdated) {
                Log.w(TAG, "Received onPreUpdate twice without onData/onInvalidated.");
            }
            isCacheReady = false;
            mUpstreamPreUpdated = true;
            if (!mDownstreamPreUpdated) {
                mDownstreamPreUpdated = true;
                mDownstream.onPreUpdate();
            }
        }

        @Override
        public void onData(@NonNull T newData) {
            onUpdate(newData);
        }

        @Override
        public void onInvalidated() {
            onUpdate(null);
        }

        private void onUpdate(@Nullable T newData) {
            if (!mUpstreamPreUpdated) {
                Log.w(TAG, "Received onData/onInvalidated without onPreUpdate.");
            }
            mUpstreamPreUpdated = false;
            isCacheReady = true;
            cache = newData;
            handleStateUpdate();
        }
    }

    void handleStateUpdate() {
        if (!mLhsUpstreamCallback.isCacheReady || !mRhsUpstreamCallback.isCacheReady) {
            return;
        }
        LhsT lhs = mLhsUpstreamCallback.cache;
        RhsT rhs = mRhsUpstreamCallback.cache;

        if (lhs == null || rhs == null) {
            mDownstream.onInvalidated();
        } else {
            O result = mTransformer.apply(lhs, rhs);
            if (result == null) {
                mDownstream.onInvalidated();
            } else {
                mDownstream.onData(result);
            }
        }
    }

    public DynamicTypeValueReceiverWithPreUpdate<LhsT> getLhsUpstreamCallback() {
        return mLhsUpstreamCallback;
    }

    public DynamicTypeValueReceiverWithPreUpdate<RhsT> getRhsUpstreamCallback() {
        return mRhsUpstreamCallback;
    }
}
