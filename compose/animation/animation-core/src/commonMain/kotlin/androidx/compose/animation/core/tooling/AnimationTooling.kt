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

package androidx.compose.animation.core.tooling

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.runtime.State

/**
 * Exposes tooling values used in Android Studio to control animations externally for each
 * [androidx.compose.animation.core.animateValueAsState] in composition.
 *
 * The tooling handle is intended to be used with ui-tooling artifact of a matching version.
 */
@RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
public interface AnimateValueAsStateToolingHandle<T, V : AnimationVector> {
    /** Current [Animatable] for this instance */
    public val animatable: Animatable<T, V>
    /** Current [AnimationSpec] for this instance */
    public val animationSpec: AnimationSpec<T>

    /**
     * Sets state that overrides the provided value. This state can later be controlled externally
     * if required by tooling. Setting the value to `null` returns control back to the underlying
     * [animatable].
     */
    public fun setToolingOverrideState(toolingOverrideState: State<T>?)
}
