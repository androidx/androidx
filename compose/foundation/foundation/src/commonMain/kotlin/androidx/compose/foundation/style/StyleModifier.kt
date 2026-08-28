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

@file:Suppress("NOTHING_TO_INLINE", "RemoveRedundantQualifierName")
@file:OptIn(ExperimentalFoundationStyleApi::class)

package androidx.compose.foundation.style

import androidx.compose.ui.Modifier

/**
 * Transitionary modifier. Do not use. This will be removed before the Styles API is stable.
 *
 * Modifier that creates a region that is styled by the given [Style] object for the component this
 * Modifier is attached to.
 *
 * Apply [styleable] to creates a "styleable" component whose looks can be customized using the
 * provided style. This should be called by a component that wishes to make itself styleable via a
 * `style: Style = Style` parameter. If a component already takes a [Style] parameter, then that
 * component internally is applying the [styleable], and that [Style] parameter should be used
 * instead of applying [styleable] again.
 *
 * If [styleable] is added to a modifier chain that is after an another [styleable], then the second
 * region will wrap around the first. For example, if the two regions both supply padding then the
 * padding will the sum of both regions.
 *
 * @param styleState the state the style will use to decide which styles should be applied. If
 *   `null` is supplied, the style will only see the default state that will never be changed.
 * @param style the style to apply to the styleable region.
 * @see MutableStyleState
 * @see Style
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
public fun Modifier.styleable(styleState: StyleState? = null, style: Style): Modifier =
    if (style === Style) this
    else {
        val resolver = ProxyStyleResolver(style, styleState)
        this.styleResolver(resolver)
            .styleExternalPadding(resolver)
            .styleGraphicsLayer(resolver)
            .stylePlacement(resolver)
            .styleAppearance(resolver)
            .styleBorderPadding(resolver)
            .styleContentPadding(resolver)
    }

/**
 * Transitionary modifier. Do not use. This will be removed before the Styles API is stable.
 *
 * Modifier that creates a region that is styled by the given [Style] object for the component this
 * Modifier is attached to. Styles that are further "to the right", will have the properties they
 * set override set properties of Styles to the left of them.
 *
 * Apply [styleable] to creates a "styleable" component whose looks can be customized using the
 * provided `style` together with one or more default [Style] objects. This should be called by a
 * component that wishes to make itself styleable via a `style: Style = Style` parameter. If a
 * component already takes a [Style] parameter, then that component internally is applying the
 * [styleable], and that [Style] parameter should be used instead of applying [styleable] again,
 *
 * If [styleable] is added to a modifier chain that is after an another [styleable], then the second
 * region will wrap around the first. For example, if the two regions both supply padding then the
 * padding will the sum of both regions.
 *
 * @param styleState the state the style will use to decide which styles should be applied. If
 *   `null` is supplied, the style will only see the default state that will never be changed.
 * @param styles the styles to apply, in order, to the stylable region.
 * @see MutableStyleState
 * @see Style
 * @see StyleScope
 */
@ExperimentalFoundationStyleApi
public fun Modifier.styleable(styleState: StyleState?, vararg styles: Style): Modifier =
    styleable(styleState, Style(*styles))

/**
 * Modifier that creates a region that is styled by the given [Style] object for the component this
 * Modifier is attached to. Styles that are further "to the right", will have the properties they
 * set override set properties of Styles to the left of them.
 *
 * Apply [styleable] to creates a "styleable" component whose looks can be customized using the
 * provided `style` together with one or more default [Style] objects. This should be called by a
 * component that wishes to make itself styleable via a `style: Style = Style` parameter. If a
 * component already takes a [Style] parameter, then that component internally is applying the
 * [styleable], and that [Style] parameter should be used instead of applying [styleable] again,
 *
 * If [styleable] is added to a modifier chain that is after an another [styleable], then the second
 * region will wrap around the first. For example, if the two regions both supply padding then the
 * padding will the sum of both regions.
 *
 * @param styleState the state the style will use to decide which styles should be applied. If
 *   `null` is supplied, the style will only see the default state that will never be changed.
 * @see MutableStyleState
 * @see Style
 * @see StyleScope
 */
@Deprecated(StyleableWithNoStyles, level = DeprecationLevel.ERROR)
@ExperimentalFoundationStyleApi
@Suppress(
    "DeprecatedCallableAddReplaceWith",
    "UNUSED_PARAMETER",
    "UnusedReceiverParameter",
    "ModifierFactoryUnreferencedReceiver",
)
public fun Modifier.styleable(styleState: StyleState?): Modifier {
    error(StyleableWithNoStyles)
}

private const val StyleableWithNoStyles =
    "The styleable() modifier must provide one or more 'style' parameter values. Calling it " +
        "with no style parameter values has no effect."

/**
 * Transitionary modifier. Do not use. This will be removed before the Styles API is stable.
 *
 * This modifier is to help enable transition between the old [Style] based API and the new
 * [CustomStyle] based API. This duplicates the behavior of the properties from [StyleScope] of
 * [Style].
 *
 * This modifier is an intermediate step from [styleable] that takes just a [MutableStyleState] and
 * [Style] to one that takes a [StyleResolver].
 *
 * This will be moved to be an example of how to use [appearance] to implement the style properties
 * similar to those provided in [Style].
 */
public fun Modifier.styleable(styleResolver: StyleResolver): Modifier =
    styleResolver(styleResolver)
        .styleExternalPadding(styleResolver)
        .styleGraphicsLayer(styleResolver)
        .stylePlacement(styleResolver)
        .styleAppearance(styleResolver)
        .styleBorderPadding(styleResolver)
        .styleContentPadding(styleResolver)
