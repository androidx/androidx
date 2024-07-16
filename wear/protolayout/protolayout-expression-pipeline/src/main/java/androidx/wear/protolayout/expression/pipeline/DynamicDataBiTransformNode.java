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
 * @param <LhsT> The source data type for the left-hand side of the operation.
 * @param <RhsT> The source data type for the right-hand side of the operation.
 * @param <O> The data type that this node emits.
 */
class DynamicDataBiTransformNode<LhsT, RhsT, O> implements DynamicDataNode<O> {
    private static final String TAG = "DynamicDataBiTransform";

    private final DynamicTypeValueReceiverWithPreUpdate<LhsT> mLhsIncomingCallback;
    private final DynamicTypeValueReceiverWithPreUpdate<RhsT> mRhsIncomingCallback;

    final DynamicTypeValueReceiverWithPreUpdate<O> mDownstream;
    private final BiFunction<LhsT, RhsT, O> mTransformer;

    @Nullable LhsT mCachedLhsData;
    @Nullable RhsT mCachedRhsData;

    int mPendingLhsStateUpdates = 0;
    int mPendingRhsStateUpdates = 0;

    DynamicDataBiTransformNode(
            DynamicTypeValueReceiverWithPreUpdate<O> downstream,
            BiFunction<LhsT, RhsT, O> transformer) {
        this.mDownstream = downstream;
        this.mTransformer = transformer;

        // These classes refer to handlePreStateUpdate, which is @UnderInitialization when these
        // initializers run, and hence raise an error. It's invalid to annotate
        // handle{Pre}StateUpdate as @UnderInitialization (since it refers to initialized fields),
        // and moving this assignment into the constructor yields the same error (since one of the
        // fields has to be assigned first, when the class is still under initialization).
        //
        // The only path to get these is via get{Lhs,Rhs}IncomingCallback, which can only be called
        // when the class is initialized (and which also cannot be called from a sub-constructor, as
        // that will again complain that it's calling something which is @UnderInitialization).
        // Given that, suppressing the warning in onStateUpdate should be safe.
        this.mLhsIncomingCallback =
                new DynamicTypeValueReceiverWithPreUpdate<LhsT>() {
                    @Override
                    public void onPreUpdate() {
                        mPendingLhsStateUpdates++;

                        if (mPendingLhsStateUpdates == 1 && mPendingRhsStateUpdates == 0) {
                            mDownstream.onPreUpdate();
                        }
                    }

                    @SuppressWarnings("method.invocation")
                    @Override
                    public void onData(@NonNull LhsT newData) {
                        onUpdatedImpl(newData);
                    }

                    private void onUpdatedImpl(@Nullable LhsT newData) {
                        if (mPendingLhsStateUpdates == 0) {
                            Log.w(
                                    TAG,
                                    "Received a state update, but one or more suppliers did not"
                                            + " call onPreStateUpdate");
                        } else {
                            mPendingLhsStateUpdates--;
                        }

                        mCachedLhsData = newData;
                        handleStateUpdate();
                    }

                    @Override
                    public void onInvalidated() {
                        // Note: Casts are required here to help out the null checker.
                        onUpdatedImpl((LhsT) null);
                    }
                };

        this.mRhsIncomingCallback =
                new DynamicTypeValueReceiverWithPreUpdate<RhsT>() {
                    @Override
                    public void onPreUpdate() {
                        mPendingRhsStateUpdates++;

                        if (mPendingLhsStateUpdates == 0 && mPendingRhsStateUpdates == 1) {
                            mDownstream.onPreUpdate();
                        }
                    }

                    @SuppressWarnings("method.invocation")
                    @Override
                    public void onData(@NonNull RhsT newData) {
                        onUpdatedImpl(newData);
                    }

                    private void onUpdatedImpl(@Nullable RhsT newData) {
                        if (mPendingRhsStateUpdates == 0) {
                            Log.w(
                                    TAG,
                                    "Received a state update, but one or more suppliers did not"
                                            + " call onPreStateUpdate");
                        } else {
                            mPendingRhsStateUpdates--;
                        }

                        mCachedRhsData = newData;
                        handleStateUpdate();
                    }

                    @Override
                    public void onInvalidated() {
                        onUpdatedImpl((RhsT) null);
                    }
                };
    }

    void handleStateUpdate() {
        if (mPendingLhsStateUpdates == 0 && mPendingRhsStateUpdates == 0) {
            LhsT lhs = mCachedLhsData;
            RhsT rhs = mCachedRhsData;

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
    }

    public DynamicTypeValueReceiverWithPreUpdate<LhsT> getLhsIncomingCallback() {
        return mLhsIncomingCallback;
    }

    public DynamicTypeValueReceiverWithPreUpdate<RhsT> getRhsIncomingCallback() {
        return mRhsIncomingCallback;
    }

    @Override
    public int getCost() {
        return DEFAULT_NODE_COST;
    }
}
