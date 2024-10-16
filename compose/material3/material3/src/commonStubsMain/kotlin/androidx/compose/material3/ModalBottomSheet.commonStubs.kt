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

package androidx.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

@Immutable
@ExperimentalMaterial3Api
actual class ModalBottomSheetProperties
actual constructor(
    actual val shouldDismissOnBackPress: Boolean,
    actual val isAppearanceLightStatusBars: Boolean,
    actual val isAppearanceLightNavigationBars: Boolean,
)

@Immutable
@ExperimentalMaterial3Api
actual object ModalBottomSheetDefaults {
    actual val properties: ModalBottomSheetProperties = implementedInJetBrainsFork()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal actual fun ModalBottomSheetDialog(
    onDismissRequest: () -> Unit,
    properties: ModalBottomSheetProperties,
    predictiveBackProgress: Animatable<Float, AnimationVector1D>,
    content: @Composable () -> Unit,
): Unit = implementedInJetBrainsFork()
