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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Dynamic data nodes which yield result based on the given condition. */
class ConditionalOpNode<T> implements DynamicDataNode<T> {
    private final DynamicTypeValueReceiverWithPreUpdate<T> mTrueValueIncomingCallback;
    private final DynamicTypeValueReceiverWithPreUpdate<T> mFalseValueIncomingCallback;
    private final DynamicTypeValueReceiverWithPreUpdate<Boolean> mConditionIncomingCallback;

    final DynamicTypeValueReceiverWithPreUpdate<T> mDownstream;

    @Nullable Boolean mLastConditional;
    @Nullable T mLastTrueValue;
    @Nullable T mLastFalseValue;

    // Counters to track how many "in-flight" updates are pending for each input. If any of these
    // are >0, then we're still waiting for onData() to be called for one of the callbacks, so we
    // shouldn't emit any values until they have all resolved.
    int mPendingConditionalUpdates = 0;
    int mPendingTrueValueUpdates = 0;
    int mPendingFalseValueUpdates = 0;

    ConditionalOpNode(DynamicTypeValueReceiverWithPreUpdate<T> downstream) {
        mDownstream = downstream;

        // These classes refer to this.handleUpdate, which is @UnderInitialization when these
        // initializers run, and hence raise an error. It's invalid to annotate
        // handle{Pre}StateUpdate as @UnderInitialization (since it refers to initialized fields),
        // and moving this assignment into the constructor yields the same error (since one of the
        // fields has to be assigned first, when the class is still under initialization).
        //
        // The only path to get these is via get*IncomingCallback, which can only be called when the
        // class is initialized (and which also cannot be called from a sub-constructor, as that
        // will again complain that it's calling something which is @UnderInitialization). Given
        // that, suppressing the warning in onData should be safe.
        mTrueValueIncomingCallback =
                new DynamicTypeValueReceiverWithPreUpdate<T>() {
                    @Override
                    public void onPreUpdate() {
                        mPendingTrueValueUpdates++;

                        if (mPendingTrueValueUpdates == 1
                                && mPendingFalseValueUpdates == 0
                                && mPendingConditionalUpdates == 0) {
                            mDownstream.onPreUpdate();
                        }
                    }

                    @SuppressWarnings("method.invocation")
                    @Override
                    public void onData(@NonNull T newData) {
                        if (mPendingTrueValueUpdates > 0) {
                            mPendingTrueValueUpdates--;
                        }

                        mLastTrueValue = newData;
                        handleUpdate();
                    }

                    @SuppressWarnings("method.invocation")
                    @Override
                    public void onInvalidated() {
                        if (mPendingTrueValueUpdates > 0) {
                            mPendingTrueValueUpdates--;
                        }

                        mLastTrueValue = null;
                        handleUpdate();
                    }
                };

        mFalseValueIncomingCallback =
                new DynamicTypeValueReceiverWithPreUpdate<T>() {
                    @Override
                    public void onPreUpdate() {
                        mPendingFalseValueUpdates++;

                        if (mPendingTrueValueUpdates == 0
                                && mPendingFalseValueUpdates == 1
                                && mPendingConditionalUpdates == 0) {
                            mDownstream.onPreUpdate();
                        }
                    }

                    @SuppressWarnings("method.invocation")
                    @Override
                    public void onData(@NonNull T newData) {
                        if (mPendingFalseValueUpdates > 0) {
                            mPendingFalseValueUpdates--;
                        }

                        mLastFalseValue = newData;
                        handleUpdate();
                    }

                    @SuppressWarnings("method.invocation")
                    @Override
                    public void onInvalidated() {
                        if (mPendingFalseValueUpdates > 0) {
                            mPendingFalseValueUpdates--;
                        }

                        mLastFalseValue = null;
                        handleUpdate();
                    }
                };

        mConditionIncomingCallback =
                new DynamicTypeValueReceiverWithPreUpdate<Boolean>() {
                    @Override
                    public void onPreUpdate() {
                        mPendingConditionalUpdates++;

                        if (mPendingTrueValueUpdates == 0
                                && mPendingFalseValueUpdates == 0
                                && mPendingConditionalUpdates == 1) {
                            mDownstream.onPreUpdate();
                        }
                    }

                    @SuppressWarnings("method.invocation")
                    @Override
                    public void onData(@NonNull Boolean newData) {
                        if (mPendingConditionalUpdates > 0) {
                            mPendingConditionalUpdates--;
                        }

                        mLastConditional = newData;
                        handleUpdate();
                    }

                    @SuppressWarnings("method.invocation")
                    @Override
                    public void onInvalidated() {
                        if (mPendingConditionalUpdates > 0) {
                            mPendingConditionalUpdates--;
                        }

                        mLastConditional = null;
                        handleUpdate();
                    }
                };
    }

    public DynamicTypeValueReceiverWithPreUpdate<T> getTrueValueIncomingCallback() {
        return mTrueValueIncomingCallback;
    }

    public DynamicTypeValueReceiverWithPreUpdate<T> getFalseValueIncomingCallback() {
        return mFalseValueIncomingCallback;
    }

    public DynamicTypeValueReceiverWithPreUpdate<Boolean> getConditionIncomingCallback() {
        return mConditionIncomingCallback;
    }

    void handleUpdate() {
        if (mPendingTrueValueUpdates > 0
                || mPendingFalseValueUpdates > 0
                || mPendingConditionalUpdates > 0) {
            return;
        }

        if (mLastTrueValue == null || mLastFalseValue == null || mLastConditional == null) {
            mDownstream.onInvalidated();
            return;
        }

        if (mLastConditional) {
            mDownstream.onData(mLastTrueValue);
        } else {
            mDownstream.onData(mLastFalseValue);
        }
    }

    @Override
    public int getCost() {
        return DEFAULT_NODE_COST;
    }
}
