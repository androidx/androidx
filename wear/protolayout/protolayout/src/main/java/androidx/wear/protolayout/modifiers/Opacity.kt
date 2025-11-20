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

@file:Suppress("FacadeClassJvmName") // Cannot be updated, the Kt name has been released

package androidx.wear.protolayout.modifiers

import android.annotation.SuppressLint
import androidx.annotation.FloatRange
import androidx.wear.protolayout.TypeBuilders.FloatProp
import androidx.wear.protolayout.expression.AnimationParameterBuilders
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.PlatformEventSources
import androidx.wear.protolayout.expression.RequiresSchemaVersion

/**
 * Adds a modifier to specify the opacity of the element with a value from 0 to 1, where 0 means the
 * element is completely transparent and 1 means the element is completely opaque.
 *
 * @param staticValue The static value for opacity. This value will be used if [dynamicValue] is
 *   null, or if can't be resolved.
 * @param dynamicValue The dynamic value for opacity. This can be used to change the opacity of the
 *   element dynamically (without changing the layout definition). To create a smooth transition for
 *   the dynamic change, you can use one of [DynamicFloat.animate] methods.
 */
@RequiresSchemaVersion(major = 1, minor = 400)
fun LayoutModifier.opacity(
    @FloatRange(from = 0.0, to = 1.0) staticValue: Float,
    dynamicValue: DynamicFloat? = null,
): LayoutModifier = this then BaseOpacityElement(staticValue, dynamicValue)

/**
 * The recommended opacity modifier for Lottie animation or any other element that should appear as
 * the layout is swiped to.
 *
 * It is recommended to be used when Lottie animation is infinite, so that Lottie animation first
 * becomes visible with fading in, followed by animation show, to avoid showing static state while
 * user swipes to the layout.
 *
 * @sample androidx.wear.protolayout.material3.samples.lottieWithFadeIn
 */
@RequiresSchemaVersion(major = 1, minor = 500)
fun LayoutModifier.fadeInOnVisibleModifier(
    animationSpec: AnimationParameterBuilders.AnimationSpec =
        AnimationParameterBuilders.AnimationSpec.Builder()
            .setAnimationParameters(
                AnimationParameterBuilders.AnimationParameters.Builder()
                    .setDurationMillis(150)
                    .build()
            )
            .build()
): LayoutModifier =
    this then
        LayoutModifier.opacity(
            staticValue = 1F,
            dynamicValue =
                DynamicFloat.onCondition(PlatformEventSources.isLayoutVisible())
                    .use(1F)
                    .elseUse(0F)
                    .animate(animationSpec),
        )

@RequiresSchemaVersion(major = 1, minor = 400)
internal class BaseOpacityElement(val staticValue: Float, val dynamicValue: DynamicFloat? = null) :
    BaseProtoLayoutModifiersElement<FloatProp.Builder> {
    @SuppressLint("ProtoLayoutMinSchema")
    override fun mergeTo(initialBuilder: FloatProp.Builder?): FloatProp.Builder =
        (initialBuilder ?: FloatProp.Builder(staticValue)).apply {
            dynamicValue?.let { setDynamicValue(it) }
        }
}
