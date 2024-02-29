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

import androidx.annotation.UiThread;

/**
 * Data source node within a dynamic data pipeline. This node type should push data into the
 * pipeline, either once when the pipeline is inited, or periodically from its data source (e.g. for
 * sensor nodes).
 *
 * @param <T> The type of data this node emits.
 */
interface DynamicDataSourceNode<T> extends DynamicDataNode<T> {
    /**
     * Called on all source nodes before {@link DynamicDataSourceNode#init()} is called on any node.
     * This should generally only call {@link DynamicTypeValueReceiverWithPreUpdate#onPreUpdate()}
     * on all downstream nodes.
     */
    @UiThread
    void preInit();

    /**
     * Initialize this node. This should cause it to bind to any data sources, and emit its first
     * value.
     */
    @UiThread
    void init();

    /** Destroy this node. This should cause it to unbind from any data sources. */
    @UiThread
    void destroy();

    @Override
    default int getCost() {
        return DEFAULT_NODE_COST;
    }
}
