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

/**
 * Node within a dynamic data pipeline.
 *
 * <p>Each node should either be a {@link DynamicDataSourceNode}, in which case it pushes data into
 * the pipeline, or it should expose one or more callbacks (generally instances of {@link
 * DynamicTypeValueReceiver}), which can be used by an upstream node to "push" data through the
 * pipeline. A node would typically look like the following:
 *
 * <pre>{@code
 * class IntToStringNode implements DynamicDataNode<String> {
 *   // The consumer on the downstream node to push data to.
 *   private final DynamicTypeValueReceiver<String> downstreamNode;
 *
 *   private final DynamicTypeValueReceiver<Integer> myNode =
 *     new DynamicTypeValueReceiver<Integer>() {
 *       @Override
 *       public void onPreUpdate() {
 *         // Don't need to do anything here; just relay.
 *         downstreamNode.onPreUpdate();
 *       }
 *
 *       @Override
 *       public void onData(Integer newData) {
 *         downstreamNode.onData(newData.toString());
 *       }
 *     };
 *
 *   public DynamicTypeValueReceiver<Integer> getConsumer() { return myNode; }
 * }
 * }</pre>
 *
 * An upstream node (i.e. one which yields an Integer) would then push data in to this node, via the
 * consumer returned from {@code IntToStringNode#getConsumer}.
 *
 * <p>Generally, node implementations will not use this interface directly; {@link
 * DynamicDataTransformNode} and {@link DynamicDataBiTransformNode} provide canonical
 * implementations for transforming data pushed from one or two source nodes, to a downstream node.
 *
 * @param <O> The data type that this node yields.
 */
interface DynamicDataNode<O> {}
