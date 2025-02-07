/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.subspace.node

import androidx.compose.ui.semantics.SemanticsPropertyReceiver

/**
 * A [SubspaceModifier.Node] that adds semantics key/values for use in testing, accessibility, and
 * similar use cases.
 */
public interface SubspaceSemanticsModifierNode {
    /**
     * Adds semantics key/value pairs to the layout node, for use in testing, accessibility, etc.
     *
     * The [SemanticsPropertyReceiver] provides "key = value"-style setters for any
     * [SemanticsPropertyKey]. Also, chaining multiple semantics modifiers is supported.
     */
    public fun SemanticsPropertyReceiver.applySemantics()
}
