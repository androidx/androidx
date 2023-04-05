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

import java.util.List;

/**
 * Dynamic type node implementation that contains a list of {@link DynamicDataNode} created during
 * evaluation.
 */
class BoundDynamicTypeImpl implements BoundDynamicType {
    private final List<DynamicDataNode<?>> mNodes;
    private final QuotaManager mDynamicDataNodesQuotaManager;

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

    @Override
    public int getDynamicNodeCount() {
        return mNodes.size();
    }

    @Override
    public void close() {
        mNodes.stream()
                .filter(n -> n instanceof DynamicDataSourceNode)
                .forEach(n -> ((DynamicDataSourceNode<?>) n).destroy());
        mDynamicDataNodesQuotaManager.releaseQuota(getDynamicNodeCount());
    }
}
