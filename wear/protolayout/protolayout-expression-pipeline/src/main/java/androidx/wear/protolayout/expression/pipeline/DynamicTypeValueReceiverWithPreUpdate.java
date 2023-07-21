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
 * @see DynamicTypeValueReceiver
 *     <p>It is guaranteed that for any given batch evaluation result, {@link #onPreUpdate()} will
 *     be called on all listeners before any {@link #onData} or {@link #onInvalidated} calls are
 *     fired.
 * @param <T> Data type.
 */
interface DynamicTypeValueReceiverWithPreUpdate<T>
        extends DynamicTypeValueReceiver<T> {
    /**
     * Called when evaluation result for the dynamic type that this callback was registered for is
     * about to be updated. This allows a downstream consumer to properly synchronize updates if it
     * depends on two or more evaluation result items. In that case, it should use this call to
     * figure out how many of its dependencies are going to be updated, and wait for all of them to
     * be updated (via {@link #onData(T)}) or {@link #onInvalidated()} before acting on the change.
     */
    void onPreUpdate();
}
