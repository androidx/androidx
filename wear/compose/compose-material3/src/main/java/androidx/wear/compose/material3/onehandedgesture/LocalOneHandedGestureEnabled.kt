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

package androidx.wear.compose.material3.onehandedgesture

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal that controls whether one-handed gestures are enabled within the provided
 * composition tree.
 *
 * When set to `true` (the default), any `Modifier.oneHandedGesture` applied within this composition
 * scope will actively track and process one-handed gestures. When provided with `false`, those
 * modifiers will gracefully ignore relevant touch events without being removed from the composition
 * tree. Example usage:
 * ```
 * CompositionLocalProvider(LocalOneHandedGestureEnabled provides false) {
 *   // Any oneHandedGesture modifiers inside this component will be disabled
 *   MyEncapsulatedScreenContent()
 * }
 * ```
 *
 * Sample demonstrating how to disable gesture:
 *
 * @sample androidx.wear.compose.material3.samples.OneHandedGestureDisableButtonSample
 */
public val LocalOneHandedGestureEnabled: ProvidableCompositionLocal<Boolean> = compositionLocalOf {
    true
}
