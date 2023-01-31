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
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;

/**
 * Callback for an evaluation result. This is intended to support two-step updates; first a
 * notification will be sent that the evaluation result item will be updated, then the new
 * evaluation result will be delivered. This allows downstream consumers to properly synchronize
 * their updates if they depend on two or more evaluation result items, rather than updating
 * multiple times (with potentially invalid states).
 *
 * <p>It is guaranteed that for any given batch evaluation result, {@link #onPreUpdate()} will be
 * called on all listeners before any {@link #onData} calls are fired.
 *
 * @param <T> Data type.
 */
public interface DynamicTypeValueReceiver<T> {
    /**
     * Called when evaluation result for the expression that this callback was registered for is
     * about to be updated. This allows a downstream consumer to properly synchronize updates if it
     * depends on two or more evaluation result items. In that case, it should use this call to
     * figure out how many of its dependencies are going to be updated, and wait for all of them to
     * be updated (via {@link DynamicTypeValueReceiver#onData(T)}) before acting on the change.
     *
     * @hide
     */
    @UiThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    void onPreUpdate();

    /**
     * Called when the expression that this callback was registered for has a new evaluation result.
     *
     * @see DynamicTypeValueReceiver#onPreUpdate()
     */
    @UiThread
    void onData(@NonNull T newData);

    /** Called when the expression that this callback was registered for has an invalid result. */
    @UiThread
    void onInvalidated();
}
