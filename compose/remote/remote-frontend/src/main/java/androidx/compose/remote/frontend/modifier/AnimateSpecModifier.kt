/*
 * Copyright (C) 2024 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.modifier

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec.ANIMATION
import androidx.compose.remote.core.operations.utilities.easing.GeneralEasing
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class AnimateSpecModifier(
    val animationId: Int,
    val motionDuration: Float,
    val motionEasingType: Int,
    val visibilityDuration: Float,
    val visibilityEasingType: Int,
    val enterAnimation: ANIMATION,
    val exitAnimation: ANIMATION,
) : RemoteLayoutModifier {
    override fun toRemoteComposeElement(): RecordingModifier.Element {
        return androidx.compose.remote.creation.modifiers.AnimateSpecModifier(
            animationId,
            motionDuration,
            motionEasingType,
            visibilityDuration,
            visibilityEasingType,
            enterAnimation,
            exitAnimation,
        )
    }

    @Composable
    override fun Modifier.toComposeUi(): Modifier {
        return this
    }
}

@Composable
fun RemoteModifier.animationSpec(
    animationId: Int = -1,
    motionDuration: Float,
    motionEasingType: Int,
    visibilityDuration: Float,
    visibilityEasingType: Int,
    enterAnimation: ANIMATION,
    exitAnimation: ANIMATION,
    enabled: Boolean = true,
): RemoteModifier {
    val id = if (enabled) animationId else 0
    return then(
        AnimateSpecModifier(
            id,
            motionDuration,
            motionEasingType,
            visibilityDuration,
            visibilityEasingType,
            enterAnimation,
            exitAnimation,
        )
    )
}

@Composable
fun RemoteModifier.animationSpec(animationId: Int = -1, enabled: Boolean): RemoteModifier {
    val id = if (enabled) animationId else 0
    return then(
        AnimateSpecModifier(
            id,
            300f,
            GeneralEasing.CUBIC_STANDARD,
            300f,
            GeneralEasing.CUBIC_STANDARD,
            ANIMATION.FADE_IN,
            ANIMATION.FADE_OUT,
        )
    )
}
