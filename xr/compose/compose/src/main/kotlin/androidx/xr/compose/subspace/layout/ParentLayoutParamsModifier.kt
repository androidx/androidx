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

package androidx.xr.compose.subspace.layout

/**
 * Interface for modifiers that can adjust the layout parameters of a composable that implements
 * [ParentLayoutParamsAdjustable].
 *
 * This interface allows a parent composable to influence the layout of its children in a structured
 * way. Implementations of this interface are responsible for determining if they can and should
 * adjust the provided layout parameters.
 *
 * When the layout system processes modifiers, if a modifier implements ParentLayoutParamsModifier`,
 * its `adjustParams` method will be called with an object that implements
 * [ParentLayoutParamsAdjustable]. This object typically represents the layout parameters of the
 * child composable being laid out.
 *
 * ```
 * An example modifier node that sets a specific property on TargetLayoutParams.
 *
 * private class ExamplePropertySetterNode(
 *     private val newValue: Any? // The value to set, similar to 'alignment'
 * ) : SomeModifierNode(), ParentLayoutParamsModifier { // Extends a base node and implements the interface
 *
 *     override fun adjustParams(params: ParentLayoutParamsAdjustable) {
 *         // Check if 'params' is the specific type this modifier can handle.
 *         if (params is TargetLayoutParams) {
 *             // Smart cast: 'params' is now known to be TargetLayoutParams.
 *             // Safely access and modify its 'someProperty'.
 *             params.someProperty = newValue
 *         }
 *     }
 * }
 * ```
 */
public interface ParentLayoutParamsModifier {

    /**
     * Adjusts the given [ParentLayoutParamsAdjustable] object.
     *
     * Implementations **must first verify** that `params` is of an expected type before casting and
     * modifying. This prevents runtime errors and ensures the modifier only alters parameters it
     * understands.
     *
     * @param params The layout parameters to potentially adjust.
     */
    public fun adjustParams(params: ParentLayoutParamsAdjustable)
}

/** Marker interface for types allowed to be adjusted by a [ParentLayoutParamsModifier]. */
public interface ParentLayoutParamsAdjustable
