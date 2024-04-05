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

package androidx.compose.foundation.gestures

import androidx.compose.animation.core.cupertino.CupertinoScrollDecayAnimationSpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.foundation.gestures.cupertino.CupertinoFlingBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

internal actual fun platformDefaultFlingBehavior(): ScrollableDefaultFlingBehavior =
    CupertinoFlingBehavior(CupertinoScrollDecayAnimationSpec().generateDecayAnimationSpec())

@Composable
internal actual fun rememberPlatformDefaultFlingBehavior(): FlingBehavior =
    // Unlike other platforms, we don't need to remember it based on density,
    // because it's density independent
    remember {
        platformDefaultFlingBehavior()
    }