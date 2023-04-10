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

package androidx.appactions.interaction.capabilities.core.impl.concurrent;

import androidx.annotation.NonNull;

/**
 * A FutureCallback that can be attached to a ListenableFuture with Futures#addCallback.
 *
 * @param <V>
 */
public interface FutureCallback<V> {
    /** Called with the ListenableFuture's result if it completes successfully. */
    void onSuccess(V result);

    /** Called with the ListenableFuture's exception if it fails. */
    void onFailure(@NonNull Throwable t);
}
