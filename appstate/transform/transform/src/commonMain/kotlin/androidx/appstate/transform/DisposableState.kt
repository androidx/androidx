/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appstate.transform

import androidx.compose.runtime.State

/**
 * A [State] that can be disposed to stop its underlying state transformation.
 *
 * This is meant to be returned from a [transform].
 *
 * @param T The type of data held by this state.
 */
public interface DisposableState<T> : State<T> {
    /**
     * Disposes this state.
     *
     * In the context of a transform, this will remove it from the [TransformScope] and prevent it
     * from getting any further updates.
     */
    public fun dispose()
}
