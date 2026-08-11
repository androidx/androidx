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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.modifier

import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.remote.core.operations.layout.modifiers.MarqueeModifierOperation
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.rawDimensionDp
import androidx.compose.remote.player.compose.embedded.readDataReflection
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
internal fun Modifier.marquee(op: MarqueeModifierOperation): Modifier {
    // Note: To support precise remote-core marquee we'd need a custom modifier, but we can map the
    // basic parameters to Compose's basicMarquee. The spacing may be a NaN-encoded
    // variable/expression (e.g. a dp recorded against the density variable), so resolve it before
    // scaling; remote-core scales it by density only under DP (rawDimensionDp). Velocity is not
    // density-scaled.
    val data = op.readDataReflection()
    val density = LocalDensity.current.density
    val behavior = LocalCoreDocument.current.densityBehavior
    val spacing = rememberRemoteFloatAsState(data.spacing).value
    return this.basicMarquee(
        iterations = if (data.iterations == -1) Int.MAX_VALUE else data.iterations,
        animationMode =
            if (data.animationMode == 0) MarqueeAnimationMode.Immediately
            else MarqueeAnimationMode.WhileFocused,
        repeatDelayMillis = data.repeatDelayMillis.toInt(),
        initialDelayMillis = data.initialDelayMillis.toInt(),
        spacing = MarqueeSpacing(rawDimensionDp(spacing, behavior, density)),
        velocity = data.velocity.dp,
    )
}
