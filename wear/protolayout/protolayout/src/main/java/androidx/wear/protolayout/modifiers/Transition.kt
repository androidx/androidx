/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.protolayout.modifiers

import android.annotation.SuppressLint
import androidx.wear.protolayout.ModifiersBuilders.EnterTransition
import androidx.wear.protolayout.ModifiersBuilders.ExitTransition
import androidx.wear.protolayout.ModifiersBuilders.FadeInTransition
import androidx.wear.protolayout.ModifiersBuilders.FadeOutTransition
import androidx.wear.protolayout.ModifiersBuilders.SlideInTransition
import androidx.wear.protolayout.ModifiersBuilders.SlideOutTransition
import androidx.wear.protolayout.expression.ProtoLayoutExperimental
import androidx.wear.protolayout.expression.RequiresSchemaVersion

/**
 * Adds a modifier to specify content transition that is triggered when element enters the layout.
 *
 * Any update to the element or its children will trigger this animation for this element and
 * everything underneath it.
 *
 * @param fadeIn The fading in animation for content transition of an element and its children
 *   happening when entering the layout.
 * @param slideIn The sliding in animation for content transition of an element and its children
 *   happening when entering the layout.
 */
@RequiresSchemaVersion(major = 1, minor = 200)
@ProtoLayoutExperimental
public fun LayoutModifier.enterTransition(
    fadeIn: FadeInTransition? = null,
    slideIn: SlideInTransition? = null,
): LayoutModifier = this then BaseEnterTransitionElement(fadeIn, slideIn)

/**
 * Adds a modifier to specify content transition that is triggered when element enters the layout.
 *
 * Any update to the element or its children will trigger this animation for this element and
 * everything underneath it.
 */
@RequiresSchemaVersion(major = 1, minor = 200)
@ProtoLayoutExperimental
public fun LayoutModifier.enterTransition(transition: EnterTransition): LayoutModifier =
    enterTransition(
        transition.fadeIn?.let { FadeInTransition.fromProto(it.toProto(), transition.fingerprint) },
        transition.slideIn?.let {
            SlideInTransition.fromProto(it.toProto(), transition.fingerprint)
        },
    )

/**
 * Adds a modifier to specify content transition that is triggered when element exits the layout.
 *
 * Any update to the element or its children will trigger this animation for this element and
 * everything underneath it.
 *
 * @param fadeOut The fading out animation for content transition of an element and its children
 *   happening when exiting the layout.
 * @param slideOut The sliding out animation for content transition of an element and its children
 *   happening when exiting the layout.
 */
@RequiresSchemaVersion(major = 1, minor = 200)
@ProtoLayoutExperimental
public fun LayoutModifier.exitTransition(
    fadeOut: FadeOutTransition? = null,
    slideOut: SlideOutTransition? = null,
): LayoutModifier = this then BaseExitTransitionElement(fadeOut, slideOut)

/**
 * Adds a modifier to specify content transition that is triggered when element exits the layout.
 *
 * Any update to the element or its children will trigger this animation for this element and
 * everything underneath it.
 */
@RequiresSchemaVersion(major = 1, minor = 200)
@ProtoLayoutExperimental
public fun LayoutModifier.exitTransition(transition: ExitTransition): LayoutModifier =
    exitTransition(
        transition.fadeOut?.let {
            FadeOutTransition.fromProto(it.toProto(), transition.fingerprint)
        },
        transition.slideOut?.let {
            SlideOutTransition.fromProto(it.toProto(), transition.fingerprint)
        },
    )

@RequiresSchemaVersion(major = 1, minor = 200)
@ProtoLayoutExperimental
internal class BaseEnterTransitionElement(
    val fadeIn: FadeInTransition? = null,
    val slideIn: SlideInTransition? = null,
) : BaseProtoLayoutModifiersElement<EnterTransition.Builder> {
    @SuppressLint("ProtoLayoutMinSchema")
    override fun mergeTo(initialBuilder: EnterTransition.Builder?): EnterTransition.Builder =
        (initialBuilder ?: EnterTransition.Builder()).apply {
            if (fadeIn != null) setFadeIn(fadeIn)
            if (slideIn != null) setSlideIn(slideIn)
        }
}

@RequiresSchemaVersion(major = 1, minor = 200)
@ProtoLayoutExperimental
internal class BaseExitTransitionElement(
    val fadeOut: FadeOutTransition? = null,
    val slideOut: SlideOutTransition? = null,
) : BaseProtoLayoutModifiersElement<ExitTransition.Builder> {
    @SuppressLint("ProtoLayoutMinSchema")
    override fun mergeTo(initialBuilder: ExitTransition.Builder?): ExitTransition.Builder =
        (initialBuilder ?: ExitTransition.Builder()).apply {
            if (fadeOut != null) setFadeOut(fadeOut)
            if (slideOut != null) setSlideOut(slideOut)
        }
}
