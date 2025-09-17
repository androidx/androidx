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
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A modifier that animates the text content to scroll across the screen like a marquee.
 *
 * @param iterations The number of times to repeat the animation. Int.MAX_VALUE will repeat forever,
 *   and 0 will disable animation.
 * @param animationMode Whether the marquee should start animating Immediately or only WhileFocused.
 *   In WhileFocused mode, the modified node or the content must be made focusable. Note that the
 *   initialDelayMillis is part of the animation, so this parameter determines when that initial
 *   delay starts counting down, not when the content starts to actually scroll.
 * @param repeatDelayMillis The duration to wait before starting each subsequent iteration, in
 *   millis.
 * @param initialDelayMillis The duration to wait before starting the first iteration of the
 *   animation, in millis. By default, there will be no initial delay if animationMode is
 *   WhileFocused, otherwise the initial delay will be repeatDelayMillis.
 * @param spacing A [MarqueeSpacing] that specifies how much space to leave at the end of the
 *   content before showing the beginning again.
 * @param velocity The speed of the animation in dps / second.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MarqueeModifier(
    public val iterations: Int,
    public val animationMode: Int,
    public val repeatDelayMillis: Float,
    public val initialDelayMillis: Float,
    public val spacing: Float,
    public val velocity: Float,
) : RemoteLayoutModifier {

    override fun toRemoteComposeElement(): RecordingModifier.Element {
        return androidx.compose.remote.creation.modifiers.MarqueeModifier(
            iterations,
            animationMode,
            repeatDelayMillis,
            initialDelayMillis,
            spacing,
            velocity,
        )
    }

    @Composable
    override fun Modifier.toComposeUi(): Modifier {
        return basicMarquee(
            iterations,
            animationMode = MarqueeAnimationMode.Immediately,
            repeatDelayMillis = repeatDelayMillis.toInt(),
            initialDelayMillis.toInt(),
            spacing = MarqueeSpacing(spacing.dp),
            velocity = velocity.dp,
        )
    }
}

public fun RemoteModifier.basicMarquee(
    iterations: Int = Int.MAX_VALUE,
    animationMode: Int = 0,
    repeatDelayMillis: Float = 0f,
    initialDelayMillis: Float = 0f,
    spacing: Float = 0f,
    velocity: Float = 20f,
): RemoteModifier =
    then(
        MarqueeModifier(
            iterations,
            animationMode,
            repeatDelayMillis,
            initialDelayMillis,
            spacing,
            velocity,
        )
    )
