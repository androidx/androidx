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

package androidx.wear.protolayout.modifiers

import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ModifiersBuilders.AnimatedVisibility
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Border
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata
import androidx.wear.protolayout.ModifiersBuilders.EnterTransition
import androidx.wear.protolayout.ModifiersBuilders.ExitTransition
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ModifiersBuilders.Semantics
import androidx.wear.protolayout.TypeBuilders.BoolProp
import androidx.wear.protolayout.TypeBuilders.FloatProp
import androidx.wear.protolayout.expression.ProtoLayoutExperimental
import androidx.wear.protolayout.modifiers.LayoutModifier.Element

/** Creates a [ModifiersBuilders.Modifiers] from a [LayoutModifier]. */
@SuppressLint("ProtoLayoutMinSchema")
@OptIn(ProtoLayoutExperimental::class)
fun LayoutModifier.toProtoLayoutModifiers(): ModifiersBuilders.Modifiers {
    var semantics: Semantics.Builder? = null
    var background: Background.Builder? = null
    var corners: Corner.Builder? = null
    var clickable: Clickable.Builder? = null
    var padding: Padding.Builder? = null
    var metadata: ElementMetadata.Builder? = null
    var border: Border.Builder? = null
    var visible: BoolProp.Builder? = null
    var opacity: FloatProp.Builder? = null
    var enterTransition: EnterTransition.Builder? = null
    var exitTransition: ExitTransition.Builder? = null

    this.foldRight(Unit) { _, e ->
        when (e) {
            is BaseSemanticElement -> semantics = e.mergeTo(semantics)
            is BaseBackgroundElement -> background = e.mergeTo(background)
            is BaseCornerElement -> corners = e.mergeTo(corners)
            is BaseClickableElement -> clickable = e.mergeTo(clickable)
            is BasePaddingElement -> padding = e.mergeTo(padding)
            is BaseMetadataElement -> metadata = e.mergeTo(metadata)
            is BaseBorderElement -> border = e.mergeTo(border)
            is BaseVisibilityElement -> visible = e.mergeTo(visible)
            is BaseOpacityElement -> opacity = e.mergeTo(opacity)
            is BaseEnterTransitionElement -> enterTransition = e.mergeTo(enterTransition)
            is BaseExitTransitionElement -> exitTransition = e.mergeTo(exitTransition)
        }
    }

    corners?.let { background = (background ?: Background.Builder()).setCorner(it.build()) }

    return ModifiersBuilders.Modifiers.Builder()
        .apply {
            semantics?.let { setSemantics(it.build()) }
            background?.let { setBackground(it.build()) }
            clickable?.let { setClickable(it.build()) }
            padding?.let { setPadding(it.build()) }
            metadata?.let { setMetadata(it.build()) }
            border?.let { setBorder(it.build()) }
            visible?.let { setVisible(it.build()) }
            opacity?.let { setOpacity(it.build()) }
            if (enterTransition != null || exitTransition != null) {
                val transition = AnimatedVisibility.Builder()
                enterTransition?.let { transition.setEnterTransition(it.build()) }
                exitTransition?.let { transition.setExitTransition(it.build()) }
                setContentUpdateAnimation(transition.build())
            }
        }
        .build()
}

/** Base class for all modifiers that can merge to a ProtoLayout modifiers builder. */
internal interface BaseProtoLayoutModifiersElement<T> : Element {
    /** Merges the modifier to the passed in builder. */
    fun mergeTo(initialBuilder: T?): T?
}
