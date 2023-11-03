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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Dynamic data node that can perform a transformation from an upstream node. This should be created
 * by passing a {@link Function} in, which implements the transformation
 *
 * @param <I> The source data type of this node.
 * @param <O> The data type that this node emits.
 */
class DynamicDataTransformNode<I, O> implements DynamicDataNode<O> {
    private final DynamicTypeValueReceiverWithPreUpdate<I> mCallback;

    final DynamicTypeValueReceiverWithPreUpdate<O> mDownstream;
    final Function<I, O> mTransformer;

    DynamicDataTransformNode(
            DynamicTypeValueReceiverWithPreUpdate<O> downstream, Function<I, O> transformer) {
        this(downstream, transformer, /* validator= */ null);
    }

    DynamicDataTransformNode(
            DynamicTypeValueReceiverWithPreUpdate<O> downstream,
            Function<I, O> transformer,
            @Nullable Predicate<I> validator) {
        this.mDownstream = downstream;
        this.mTransformer = transformer;

        mCallback =
                new DynamicTypeValueReceiverWithPreUpdate<I>() {
                    @Override
                    public void onPreUpdate() {
                        // Don't need to do anything here; just relay.
                        mDownstream.onPreUpdate();
                    }

                    @Override
                    public void onData(@NonNull I newData) {
                        if (validator != null && !validator.test(newData)) {
                            mDownstream.onInvalidated();
                            return;
                        }
                        O result = mTransformer.apply(newData);
                        mDownstream.onData(result);
                    }

                    @Override
                    public void onInvalidated() {
                        mDownstream.onInvalidated();
                    }
                };
    }

    public DynamicTypeValueReceiverWithPreUpdate<I> getIncomingCallback() {
        return mCallback;
    }
}
