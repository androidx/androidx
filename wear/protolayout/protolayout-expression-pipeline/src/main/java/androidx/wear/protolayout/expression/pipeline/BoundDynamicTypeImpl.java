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

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Dynamic type node implementation that contains a list of {@link DynamicDataNode} created during
 * evaluation.
 */
class BoundDynamicTypeImpl implements BoundDynamicType {
    private static final String TAG = "BoundDynamicTypeImpl";

    private final List<DynamicDataNode<?>> mNodes;
    private final QuotaManager mDynamicDataNodesQuotaManager;
    private boolean mIsClosed = false;

    BoundDynamicTypeImpl(
            List<DynamicDataNode<?>> nodes, QuotaManager dynamicDataNodesQuotaManager) {
        this.mNodes = nodes;
        this.mDynamicDataNodesQuotaManager = dynamicDataNodesQuotaManager;
    }

    /**
     * Initializes evaluation.
     *
     * <p>See {@link BoundDynamicType#startEvaluation()}.
     */
    @Override
    public void startEvaluation() {
        mNodes.stream()
                .filter(n -> n instanceof DynamicDataSourceNode)
                .forEach(n -> ((DynamicDataSourceNode<?>) n).preInit());

        mNodes.stream()
                .filter(n -> n instanceof DynamicDataSourceNode)
                .forEach(n -> ((DynamicDataSourceNode<?>) n).init());
    }

    /**
     * Sets visibility for all {@link AnimatableNode} in this dynamic type. For others, this is
     * no-op.
     */
    @Override
    public void setAnimationVisibility(boolean visible) {
        mNodes.stream()
                .filter(n -> n instanceof AnimatableNode)
                .forEach(n -> ((AnimatableNode) n).setVisibility(visible));
    }

    /** Returns how many of {@link AnimatableNode} are running. */
    @Override
    public int getRunningAnimationCount() {
        return (int)
                mNodes.stream()
                        .filter(n -> n instanceof AnimatableNode)
                        .filter(n -> ((AnimatableNode) n).hasRunningAnimation())
                        .count();
    }

    @NonNull
    @Override
    public List<DynamicTypeAnimator> getAnimations() {
        List<QuotaAwareAnimator> animators =
                mNodes.stream()
                        .filter(n -> n instanceof AnimatableNode)
                        .map(n -> ((AnimatableNode) n).mQuotaAwareAnimator)
                        .collect(Collectors.toList());

        if (!animators.isEmpty()) {
            animators.get(animators.size() - 1).setTerminal();
        }

        return animators.stream()
                .map(animator -> (DynamicTypeAnimator) animator)
                .collect(Collectors.toList());
    }

    @Override
    public int getDynamicNodeCount() {
        return mNodes.size();
    }

    @Override
    public int getDynamicNodeCost() {
        return mNodes.stream().mapToInt(DynamicDataNode::getCost).sum();
    }

    @Override
    public void close() {
        if (Looper.getMainLooper().isCurrentThread()) {
            closeInternal();
        } else {
            new Handler(Looper.getMainLooper()).post(this::closeInternal);
        }
    }

    /**
     * Closes this {@link BoundDynamicTypeImpl} instance and releases any allocated quota. This
     * method must be called only once on each {@link BoundDynamicTypeImpl} instance.
     */
    @UiThread
    private void closeInternal() {
        if (mIsClosed) {
            Log.w(TAG, "close() method was called more than once.");
            return;
        }
        mIsClosed = true;
        mNodes.stream()
                .filter(n -> n instanceof DynamicDataSourceNode)
                .forEach(n -> ((DynamicDataSourceNode<?>) n).destroy());
        mDynamicDataNodesQuotaManager.releaseQuota(getDynamicNodeCost());
    }
}
