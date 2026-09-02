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

package androidx.compose.foundation.style

import androidx.compose.runtime.CompositionLocalAccessorScope
import androidx.compose.ui.unit.Density

/**
 * An interface that is the base interface for all styles scopes.
 *
 * [CustomStyleScope] is a constraint on the [CustomStyle]'s `ScopeT` parameter which is the
 * receiver scope of the style. At minimum all style scopes used as `ScopeT` must implement
 * [CustomStyleScope] typically by delegation to a [StyleScope] received in a [Style] lambda.
 *
 * By default, a [CustomStyleScope] does not include [AnimateStyleScope] and [StyleStateScope] which
 * should be added for these features to be usable in the corresponding [CustomStyle] type.
 * [CommonStyleScope] can be used instead which includes both.
 *
 * @sample androidx.compose.foundation.samples.StyleStateKeySample
 * @see CommonStyleScope
 */
@ExperimentalFoundationStyleApi
@StyleScopeMarker
public interface CustomStyleScope :
    Density, CompositionLocalAccessorScope, StylePropertyProviderScope

/**
 * A base type for custom styles including [CommonStyle]. A design system is expected to have
 * multiple style types that each derive from [CustomStyle], one that is used by most components in
 * the system, and a set of component specific style types. These types allow the style types to
 * only surface the properties, states, and subcomponents a design system, and its components,
 * supports. These members are accessed through the [CustomStyleScope], the class passed as the
 * [ScopeT] parameter, that is created at the same time. The [CustomStyleScope] has custom extension
 * functions (such a `background`, `minSize`, etc.) that set the design system defined
 * [StyleProperty].
 *
 * To be resolved by a [StyleResolver], a [CustomStyle] must be converted to a [CommonStyle] first.
 * A [CustomStyle] is expected to implement a `toCommonStyle()` extension function that implements a
 * boilerplate conversion. See the sample for an example of how this is done.
 *
 * @sample androidx.compose.foundation.samples.StyleStateKeySample
 * @see CommonStyle
 */
@ExperimentalFoundationStyleApi
public fun interface CustomStyle<in ScopeT : CustomStyleScope> {
    /**
     * The single abstract method used to invoke the lambda. The lambda has [ScopeT] as its receiver
     * (i.e.`this`) type.
     */
    public fun ScopeT.applyStyle()
}
